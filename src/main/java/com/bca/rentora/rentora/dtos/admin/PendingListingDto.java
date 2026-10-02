package com.bca.rentora.rentora.dtos.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record PendingListingDto(
        UUID id,
        String title,
        String category,
        UUID ownerId,
        String ownerName,
        String ownerEmail,
        String description,
        BigDecimal pricePerUnit,
        String priceUnit,
        Integer quantity,
        String city,
        String district,
        LocalTime openingTime,
        LocalTime closingTime,
        List<String> imageUrls,
        Instant createdAt
) {}