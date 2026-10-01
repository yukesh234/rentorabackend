package com.bca.rentora.rentora.repo;

import com.bca.rentora.rentora.entity.Tournament;
import com.bca.rentora.rentora.entity.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentRepo extends JpaRepository<Tournament, UUID> {
    Optional<Tournament> findByBooking_Id(UUID bookingId);
    boolean existsByBooking_Id(UUID bookingId);
    List<Tournament> findByStatus(TournamentStatus status);

    @Query("SELECT t FROM Tournament t WHERE t.booking.user.userid = :organizerId ORDER BY t.createdAt DESC")
    List<Tournament> findByOrganizerId(@Param("organizerId") UUID organizerId);
}