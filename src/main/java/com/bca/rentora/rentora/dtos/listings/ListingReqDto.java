package com.bca.rentora.rentora.dtos.listings;

import com.bca.rentora.rentora.entity.CategoryType;
import com.bca.rentora.rentora.entity.ListingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingReqDto {
    private UUID id;
    private CategoryType category;

    private String title;
    private String description;
    private BigDecimal pricePerUnit;
    private String priceUnit;
    private Integer quantity;
    private ListingStatus status;
    private String rejectionReason;

    private String city;
    private String district;
    private Double latitude;
    private Double longitude;

    private LocalTime openingTime;
    private LocalTime closingTime;

    private Instant createdAt;
    private List<String> imageUrls;

    private OwnerSummaryDto owner;
}