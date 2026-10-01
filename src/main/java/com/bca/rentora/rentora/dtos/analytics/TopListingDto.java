package com.bca.rentora.rentora.dtos.analytics;

import java.math.BigDecimal;
import java.util.UUID;

public record TopListingDto(
        UUID listingId,
        String title,
        Long bookingCount,
        BigDecimal revenue,
        Double averageRating
) {}