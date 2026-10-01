package com.bca.rentora.rentora.dtos.Tournament;

import com.bca.rentora.rentora.entity.TournamentFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TournamentCreateDto(
        @NotNull UUID bookingId,
        @NotBlank String name,
        @NotBlank String sportType,
        @NotNull TournamentFormat format,
        @NotNull @Min(2) Integer maxTeams
) {}