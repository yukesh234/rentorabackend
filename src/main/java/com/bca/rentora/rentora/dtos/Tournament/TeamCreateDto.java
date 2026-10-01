package com.bca.rentora.rentora.dtos.Tournament;

import jakarta.validation.constraints.NotBlank;

public record TeamCreateDto(
        @NotBlank String name
) {}