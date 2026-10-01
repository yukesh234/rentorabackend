package com.bca.rentora.rentora.dtos.booking;

import java.time.Instant;

public record BookedSlotDto(
        Instant startTime,
        Instant endTime,
        Integer quantity
) {}