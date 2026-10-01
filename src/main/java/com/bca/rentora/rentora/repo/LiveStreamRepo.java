package com.bca.rentora.rentora.repo;

import com.bca.rentora.rentora.entity.LiveStream;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveStreamRepo extends JpaRepository<LiveStream, UUID> {
    Optional<LiveStream> findByBooking_Id(UUID bookingId);
    List<LiveStream> findByIsLiveTrue();
}