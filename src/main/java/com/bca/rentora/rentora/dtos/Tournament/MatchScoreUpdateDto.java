package com.bca.rentora.rentora.dtos.Tournament;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MatchScoreUpdateDto(
        @NotNull @Min(0) Integer scoreA,
        @NotNull @Min(0) Integer scoreB
) {}