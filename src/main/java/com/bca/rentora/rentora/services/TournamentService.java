package com.bca.rentora.rentora.services;

import com.bca.rentora.rentora.dtos.Tournament.*;

import java.util.List;
import java.util.UUID;

public interface TournamentService {
    TournamentResponseDto createTournament(TournamentCreateDto dto, UUID organizerId);
    TournamentResponseDto getTournament(UUID tournamentId);
    List<TournamentResponseDto> getBrowsableTournaments();

    // any logged-in user can register a team while the tournament is PLANNED
    TeamResponseDto addTeam(UUID tournamentId, TeamCreateDto dto, UUID userId);
    // organizer can remove any team, a user can remove the team they registered
    void removeTeam(UUID tournamentId, UUID teamId, UUID userId);
    List<TeamResponseDto> getTeams(UUID tournamentId);

    List<MatchResponseDto> generateBracket(UUID tournamentId, UUID organizerId);
    List<MatchResponseDto> getMatches(UUID tournamentId);
    MatchResponseDto updateScore(UUID matchId, MatchScoreUpdateDto dto, UUID organizerId);
    MatchResponseDto updateSchedule(UUID matchId, MatchScheduleUpdateDto dto, UUID organizerId);

    List<StandingRowDto> getStandings(UUID tournamentId);

    List<TournamentResponseDto> getMyTournaments(UUID organizerId);
    void deleteTournament(UUID tournamentId, UUID organizerId);
}