package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.services.PaymentService;
import com.bca.rentora.rentora.dtos.Payment.PaymentInitiateDto;
import com.bca.rentora.rentora.dtos.Payment.PaymentResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponseDto> initiate(@RequestBody PaymentInitiateDto dto) {
        return ResponseEntity.ok(paymentService.initiatePayment(dto));
    }

    @GetMapping("/esewa/success")
    public ResponseEntity<String> success(@RequestParam("data") String data) {
        boolean verified = paymentService.verifyPayment(data);
        if (verified) {
            return ResponseEntity.ok("Payment verified successfully");
        }
        return ResponseEntity.badRequest().body("Payment verification failed");
    }

    @GetMapping("/esewa/failure")
    public ResponseEntity<String> failure() {
        return ResponseEntity.badRequest().body("Payment failed or cancelled");
    }
}
