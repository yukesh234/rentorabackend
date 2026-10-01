package com.bca.rentora.rentora.dtos.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReviewCreateDto(
        @NotNull UUID bookingId,
        @NotNull @Min(1) @Max(5) Integer rating,
        String comment
) {}