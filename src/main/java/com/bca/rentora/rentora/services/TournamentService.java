package com.bca.rentora.rentora.services;

import com.bca.rentora.rentora.dtos.Tournament.*;

import java.util.List;
import java.util.UUID;

public interface TournamentService {
    TournamentResponseDto createTournament(TournamentCreateDto dto, UUID organizerId);
    TournamentResponseDto getTournament(UUID tournamentId);
    List<TournamentResponseDto> getBrowsableTournaments();

    TeamResponseDto addTeam(UUID tournamentId, TeamCreateDto dto, UUID organizerId);
    List<TeamResponseDto> getTeams(UUID tournamentId);

    List<MatchResponseDto> generateBracket(UUID tournamentId, UUID organizerId);
    List<MatchResponseDto> getMatches(UUID tournamentId);
    MatchResponseDto updateScore(UUID matchId, MatchScoreUpdateDto dto, UUID organizerId);
    MatchResponseDto updateSchedule(UUID matchId, MatchScheduleUpdateDto dto, UUID organizerId);

    List<StandingRowDto> getStandings(UUID tournamentId);

    List<TournamentResponseDto> getMyTournaments(UUID organizerId);
    void deleteTournament(UUID tournamentId, UUID organizerId);
}