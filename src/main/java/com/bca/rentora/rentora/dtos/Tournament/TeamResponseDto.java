package com.bca.rentora.rentora.dtos.Tournament;

import java.util.UUID;

public record TeamResponseDto(
        UUID id,
        UUID tournamentId,
        String name
) {}