package com.bca.rentora.rentora.dtos.review;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponseDto(
        UUID id,
        UUID listingId,
        UUID reviewerId,
        String reviewerName,
        Integer rating,
        String comment,
        Instant createdAt
) {}