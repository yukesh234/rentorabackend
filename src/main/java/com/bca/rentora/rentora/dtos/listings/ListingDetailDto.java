package com.bca.rentora.rentora.dtos.listings;

import com.bca.rentora.rentora.entity.CategoryType;
import com.bca.rentora.rentora.entity.ListingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ListingDetailDto(
        UUID id,
        CategoryType category,
        String title,
        String description,
        BigDecimal pricePerUnit,
        String priceUnit,
        Integer quantity,
        ListingStatus status,
        String city,
        String district,
        Double latitude,
        Double longitude,
        LocalTime openingTime,
        LocalTime closingTime,
        List<String> imageUrls,
        OwnerSummaryDto owner,
        Double averageRating,
        Long reviewCount,
        Instant createdAt
) {}