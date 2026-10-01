package com.bca.rentora.rentora.dtos.Tournament;

import java.util.UUID;

// For LEAGUE/ROUND_ROBIN standings display
public record StandingRowDto(
        UUID teamId,
        String teamName,
        Integer played,
        Integer wins,
        Integer losses,
        Integer draws,
        Integer points
) {}