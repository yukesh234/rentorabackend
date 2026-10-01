package com.bca.rentora.rentora.dtos.analytics;

public record CategoryBreakdownDto(
        String category,
        Long bookingCount
) {}