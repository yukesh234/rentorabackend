package com.bca.rentora.rentora.services;

import com.bca.rentora.rentora.dtos.Payment.PaymentResponseDto;
import com.bca.rentora.rentora.dtos.Payment.PaymentInitiateDto;

public interface PaymentService {
    PaymentResponseDto initiatePayment(PaymentInitiateDto dto);
    boolean verifyPayment(String base64Data);
}
