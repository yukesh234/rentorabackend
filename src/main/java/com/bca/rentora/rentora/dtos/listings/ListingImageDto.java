package com.bca.rentora.rentora.dtos.listings;

import java.util.UUID;

public record ListingImageDto(
        UUID id,
        String url
) {}