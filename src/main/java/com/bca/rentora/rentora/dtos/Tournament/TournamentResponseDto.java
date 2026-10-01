package com.bca.rentora.rentora.dtos.Tournament;

import com.bca.rentora.rentora.entity.TournamentFormat;
import com.bca.rentora.rentora.entity.TournamentStatus;

import java.time.Instant;
import java.util.UUID;

public record TournamentResponseDto(
        UUID id,
        UUID bookingId,
        String listingTitle,
        UUID organizerId,
        String organizerName,
        String name,
        String sportType,
        TournamentFormat format,
        Integer maxTeams,
        TournamentStatus status,
        Instant createdAt
) {}