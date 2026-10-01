package com.bca.rentora.rentora.dtos.Tournament;

import java.time.Instant;

public record MatchScheduleUpdateDto(
        Instant scheduledAt
) {}