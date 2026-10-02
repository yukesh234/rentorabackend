package com.bca.rentora.rentora.dtos.analytics;

public record WeekdayPointDto(
        String day,
        Long bookingCount
) {}