package com.bca.rentora.rentora.repo;

import com.bca.rentora.rentora.entity.Booking;
import com.bca.rentora.rentora.entity.BookingStatus;
import com.bca.rentora.rentora.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BookingRepo extends JpaRepository<Booking, UUID> {
    List<Booking> findByUser_Userid(UUID userId);
    List<Booking> findByListing_Id(UUID listingId);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByListing_Owner_Userid(UUID ownerId);

    // used by the lifecycle job
    List<Booking> findByStatusAndPaymentMethodAndCreatedAtBefore(BookingStatus status, PaymentMethod paymentMethod, Instant cutoff);
    List<Booking> findByStatusAndEndTimeBefore(BookingStatus status, Instant time);

    // Bookings on this listing that overlap [startTime, endTime), excluding cancelled ones
    @Query("""
        SELECT b FROM Booking b
        WHERE b.listing.id = :listingId
        AND b.status != com.bca.rentora.rentora.entity.BookingStatus.CANCELLED
        AND b.startTime < :endTime
        AND b.endTime > :startTime
        """)
    List<Booking> findOverlappingBookings(
            @Param("listingId") UUID listingId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    // Public: all upcoming non-cancelled bookings for a listing, for showing "booked" slots
    @Query("""
        SELECT b FROM Booking b
        WHERE b.listing.id = :listingId
        AND b.status != com.bca.rentora.rentora.entity.BookingStatus.CANCELLED
        AND b.endTime > :now
        ORDER BY b.startTime ASC
        """)
    List<Booking> findUpcomingBookingsForListing(
            @Param("listingId") UUID listingId,
            @Param("now") Instant now
    );
}