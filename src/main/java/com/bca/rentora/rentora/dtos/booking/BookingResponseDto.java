package com.bca.rentora.rentora.dtos.booking;

import com.bca.rentora.rentora.entity.BookingStatus;
import com.bca.rentora.rentora.entity.CategoryType;
import com.bca.rentora.rentora.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingResponseDto(
        UUID id,
        UUID listingId,
        String listingTitle,
        String listingImageUrl,
        CategoryType listingCategory,
        UUID renterId,
        String renterName,
        Instant startTime,
        Instant endTime,
        Integer quantity,
        BigDecimal totalAmount,
        BookingStatus status,
        PaymentMethod paymentMethod,
        Boolean isPaid,
        Boolean isRefunded,
        Boolean hasReviewed,
        Boolean hasTournament,
        UUID tournamentId,
        Instant createdAt
) {}