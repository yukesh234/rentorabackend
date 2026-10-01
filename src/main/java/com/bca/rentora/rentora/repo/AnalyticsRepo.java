package com.bca.rentora.rentora.repo;

import com.bca.rentora.rentora.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalyticsRepo extends JpaRepository<Booking, UUID> {

    @Query("SELECT b FROM Booking b WHERE b.listing.owner.userid = :ownerId " +
            "AND (:listingId IS NULL OR b.listing.id = :listingId) " +
            "AND b.createdAt >= :startDate " +
            "AND b.createdAt <= :endDate")
    List<Booking> findForOwner(
            @Param("ownerId") UUID ownerId,
            @Param("listingId") UUID listingId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}