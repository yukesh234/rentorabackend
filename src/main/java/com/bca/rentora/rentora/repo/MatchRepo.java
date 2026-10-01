package com.bca.rentora.rentora.repo;

import com.bca.rentora.rentora.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchRepo extends JpaRepository<Match, UUID> {
    List<Match> findByTournament_IdOrderByRoundOrderAsc(UUID tournamentId);
    List<Match> findByTournament_IdAndRound(UUID tournamentId, String round);
}