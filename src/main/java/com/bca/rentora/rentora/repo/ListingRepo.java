package com.bca.rentora.rentora.repo;

import com.bca.rentora.rentora.entity.Listing;
import com.bca.rentora.rentora.entity.ListingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepo extends JpaRepository<Listing, UUID> {

    @Query("SELECT l FROM Listing l JOIN FETCH l.owner " +
            "WHERE (:excludeOwnerId IS NULL OR l.owner.userid <> :excludeOwnerId) " +
            "AND l.deletedAt IS NULL " +
            "AND l.status = com.bca.rentora.rentora.entity.ListingStatus.ACTIVE")
    List<Listing> findAllForFeed(@Param("excludeOwnerId") UUID excludeOwnerId);

    List<Listing> findByOwner_useridAndDeletedAtIsNull(UUID ownerId);

    // For update/updateAvailability/booking — ensures you can't act on a deleted listing
    @Query("SELECT l FROM Listing l WHERE l.id = :id AND l.deletedAt IS NULL")
    Optional<Listing> findActiveById(@Param("id") UUID id);

    // Row lock so two concurrent bookings can't both pass the availability check
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Listing l WHERE l.id = :id AND l.deletedAt IS NULL")
    Optional<Listing> findActiveByIdForUpdate(@Param("id") UUID id);

    List<Listing> findByStatusAndDeletedAtIsNull(ListingStatus status);
    long countByOwner_UseridAndStatusAndDeletedAtIsNull(UUID ownerId, ListingStatus status);
}