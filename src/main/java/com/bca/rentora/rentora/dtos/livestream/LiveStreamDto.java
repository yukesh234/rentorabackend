package com.bca.rentora.rentora.dtos.livestream;

import java.time.Instant;
import java.util.UUID;

public record LiveStreamDto(
        UUID id,
        UUID bookingId,
        String listingTitle,
        UUID broadcasterId,
        String broadcasterName,
        Boolean isLive,
        Instant startedAt
) {}