package com.bca.rentora.rentora.dtos.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminUserSummaryDto(
        UUID id,
        String name,
        String email,
        long activeListingCount,
        Instant createdAt
) {}