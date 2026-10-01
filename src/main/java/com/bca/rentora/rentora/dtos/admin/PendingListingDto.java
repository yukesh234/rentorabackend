package com.bca.rentora.rentora.dtos.admin;

import java.time.Instant;
import java.util.UUID;

public record PendingListingDto(
        UUID id,
        String title,
        String category,
        UUID ownerId,
        String ownerName,
        Instant createdAt
) {}