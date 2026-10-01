package com.bca.rentora.rentora.dtos.booking;

import com.bca.rentora.rentora.entity.PaymentMethod;

import java.time.Instant;
import java.util.UUID;

public record BookingCreateDto(
        UUID listingId,
        Instant startTime,
        Instant endTime,
        Integer quantity,
        PaymentMethod paymentMethod
) {}
