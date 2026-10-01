package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.dtos.Tournament.*;
import com.bca.rentora.rentora.helpers.AuthHelper;
import com.bca.rentora.rentora.security.JWTService;
import com.bca.rentora.rentora.services.TournamentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;
    private final AuthHelper authHelper;
    private final JWTService jwtService;

    public TournamentController(TournamentService tournamentService, AuthHelper authHelper, JWTService jwtService) {
        this.tournamentService = tournamentService;
        this.authHelper = authHelper;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<TournamentResponseDto> create(@Valid @RequestBody TournamentCreateDto dto,
                                                        HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(tournamentService.createTournament(dto, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponseDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.getTournament(id));
    }

    @GetMapping
    public ResponseEntity<List<TournamentResponseDto>> browse() {
        return ResponseEntity.ok(tournamentService.getBrowsableTournaments());
    }

    @PostMapping("/{id}/teams")
    public ResponseEntity<TeamResponseDto> addTeam(@PathVariable UUID id,
                                                   @Valid @RequestBody TeamCreateDto dto,
                                                   HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(tournamentService.addTeam(id, dto, userId));
    }

    @GetMapping("/{id}/teams")
    public ResponseEntity<List<TeamResponseDto>> getTeams(@PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.getTeams(id));
    }

    @PostMapping("/{id}/generate-bracket")
    public ResponseEntity<List<MatchResponseDto>> generateBracket(@PathVariable UUID id,
                                                                  HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(tournamentService.generateBracket(id, userId));
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<List<MatchResponseDto>> getMatches(@PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.getMatches(id));
    }

    @GetMapping("/{id}/standings")
    public ResponseEntity<List<StandingRowDto>> getStandings(@PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.getStandings(id));
    }

    @PatchMapping("/matches/{matchId}/score")
    public ResponseEntity<MatchResponseDto> updateScore(@PathVariable UUID matchId,
                                                        @Valid @RequestBody MatchScoreUpdateDto dto,
                                                        HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(tournamentService.updateScore(matchId, dto, userId));
    }

    @PatchMapping("/matches/{matchId}/schedule")
    public ResponseEntity<MatchResponseDto> updateSchedule(@PathVariable UUID matchId,
                                                           @RequestBody MatchScheduleUpdateDto dto,
                                                           HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(tournamentService.updateSchedule(matchId, dto, userId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<TournamentResponseDto>> getMyTournaments(HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(tournamentService.getMyTournaments(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        tournamentService.deleteTournament(id, userId);
        return ResponseEntity.noContent().build();
    }
}