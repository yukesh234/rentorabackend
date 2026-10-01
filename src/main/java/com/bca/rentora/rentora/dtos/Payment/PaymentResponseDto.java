package com.bca.rentora.rentora.dtos.Payment;

import java.util.Map;

public record PaymentResponseDto(
        String paymentUrl,
        Map<String, String> formFields,
        String transactionUuid
) {}
