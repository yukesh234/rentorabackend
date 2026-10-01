package com.bca.rentora.rentora.dtos.listings;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QuantityUpdateDto(
        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity
) {}