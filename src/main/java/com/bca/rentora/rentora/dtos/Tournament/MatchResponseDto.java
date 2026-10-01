package com.bca.rentora.rentora.dtos.Tournament;

import com.bca.rentora.rentora.entity.BracketSide;
import com.bca.rentora.rentora.entity.MatchStatus;

import java.time.Instant;
import java.util.UUID;

public record MatchResponseDto(
        UUID id,
        UUID tournamentId,
        UUID teamAId,
        String teamAName,
        UUID teamBId,
        String teamBName,
        Integer scoreA,
        Integer scoreB,
        String round,
        Integer roundOrder,
        BracketSide bracketSide,
        MatchStatus status,
        Instant scheduledAt
) {}