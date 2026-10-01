package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.core.service.EsewaService;
import com.bca.rentora.rentora.dtos.Payment.PaymentInitiateDto;
import com.bca.rentora.rentora.dtos.Payment.PaymentResponseDto;
import com.bca.rentora.rentora.entity.*;
import com.bca.rentora.rentora.exceptions.ResourceNotFoundException;
import com.bca.rentora.rentora.repo.BookingRepo;
import com.bca.rentora.rentora.repo.PaymentRepo;
import com.bca.rentora.rentora.services.PaymentService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepo bookingRepo;
    private final PaymentRepo paymentRepo;
    private final EsewaService esewaService;
    private final ObjectMapper objectMapper;

    @Value("${esewa.merchant-code}")
    private String merchantCode;

    @Value("${esewa.success-url}")
    private String successUrl;

    @Value("${esewa.failure-url}")
    private String failureUrl;

    @Value("${esewa.form-url}")
    private String esewaFormUrl;

    public PaymentServiceImpl(BookingRepo bookingRepo,
                              PaymentRepo paymentRepo,
                              EsewaService esewaService,
                              ObjectMapper objectMapper) {
        this.bookingRepo = bookingRepo;
        this.paymentRepo = paymentRepo;
        this.esewaService = esewaService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PaymentResponseDto initiatePayment(PaymentInitiateDto dto) {
        Booking booking = bookingRepo.findById(UUID.fromString(dto.bookingId()))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getPaymentMethod() != PaymentMethod.ESEWA) {
            throw new IllegalArgumentException("This booking is not set up for eSewa payment");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only pending bookings can be paid for");
        }

        String transactionUuid = UUID.randomUUID().toString();

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setGateway(PaymentGateway.ESEWA);
        payment.setTransactionId(transactionUuid);
        payment.setAmount(booking.getTotalAmount());
        paymentRepo.save(payment);

        String signature = esewaService.createSignature(booking.getTotalAmount(), transactionUuid, merchantCode);

        Map<String, String> fields = new HashMap<>();
        fields.put("amount", String.valueOf(booking.getTotalAmount()));
        fields.put("tax_amount", "0");
        fields.put("total_amount", String.valueOf(booking.getTotalAmount()));
        fields.put("transaction_uuid", transactionUuid);
        fields.put("product_code", merchantCode);
        fields.put("product_service_charge", "0");
        fields.put("product_delivery_charge", "0");
        fields.put("success_url", successUrl);
        fields.put("failure_url", failureUrl);
        fields.put("signed_field_names", "total_amount,transaction_uuid,product_code");
        fields.put("signature", signature);
        System.out.println(fields);
        return new PaymentResponseDto(esewaFormUrl, fields, transactionUuid);
    }

    @Override
    @Transactional
    public boolean verifyPayment(String base64Data) {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64Data));
            Map<String, String> payload = objectMapper.readValue(decoded, Map.class);

            String transactionCode = payload.get("transaction_code");
            String status = payload.get("status");
            String totalAmount = payload.get("total_amount");
            String transactionUuid = payload.get("transaction_uuid");
            String signedFieldNames = payload.get("signed_field_names");
            String receivedSignature = payload.get("signature");

            Payment payment = paymentRepo.findByTransactionId(transactionUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

            String expectedSignature = esewaService.createResponseSignature(
                    transactionCode, status, totalAmount, transactionUuid, merchantCode, signedFieldNames);

            if (!expectedSignature.equals(receivedSignature)) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepo.save(payment);
                return false;
            }

            if ("COMPLETE".equalsIgnoreCase(status)) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setTransactionCode(transactionCode);
                payment.setPaidAt(Instant.now());
                paymentRepo.save(payment);

                Booking booking = payment.getBooking();
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepo.save(booking);
                return true;
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepo.save(payment);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}