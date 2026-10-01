package com.bca.rentora.rentora.repo;

import com.bca.rentora.rentora.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeamRepo extends JpaRepository<Team, UUID> {
    List<Team> findByTournament_Id(UUID tournamentId);
}