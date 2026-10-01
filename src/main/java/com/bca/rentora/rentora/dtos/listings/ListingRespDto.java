package com.bca.rentora.rentora.dtos.listings;

import com.bca.rentora.rentora.dtos.auth.UserDto;
import com.bca.rentora.rentora.entity.CategoryType;
import com.bca.rentora.rentora.entity.ListingStatus;
import com.bca.rentora.rentora.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ListingRespDto {
    private UUID id;
    private User owner;
    private CategoryType category;

    private String title;
    private String description;
    private BigDecimal pricePerUnit;
    private String priceUnit;
    private Integer quantity;
    private ListingStatus status;

    // location fields for demand forecasting
    private String city;
    private String district;
    private Double latitude;
    private Double longitude;

    private Instant createdAt;
//    for the owner details
    public UserDto userdto;
}
