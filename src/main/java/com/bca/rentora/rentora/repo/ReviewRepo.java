package com.bca.rentora.rentora.repo;


import com.bca.rentora.rentora.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepo extends JpaRepository<Review, UUID> {
    List<Review> findByListing_Id(UUID listingId);
    Optional<Review> findByBooking_Id(UUID bookingId);
    boolean existsByBooking_Id(UUID bookingId);
}