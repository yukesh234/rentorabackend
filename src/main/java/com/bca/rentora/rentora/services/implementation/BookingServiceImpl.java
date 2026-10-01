package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.booking.BookedSlotDto;
import com.bca.rentora.rentora.dtos.booking.BookingCreateDto;
import com.bca.rentora.rentora.dtos.booking.BookingResponseDto;
import com.bca.rentora.rentora.entity.*;
import com.bca.rentora.rentora.exceptions.ResourceNotFoundException;
import com.bca.rentora.rentora.repo.*;
import com.bca.rentora.rentora.services.BookingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepo bookingRepo;
    private final ListingRepo listingRepo;
    private final UserRepo userRepo;
    private final ReviewRepo reviewRepo;
    private final TournamentRepo tournamentRepo;

    // update constructor
    public BookingServiceImpl(BookingRepo bookingRepo, ListingRepo listingRepo, UserRepo userRepo,
                              ReviewRepo reviewRepo, TournamentRepo tournamentRepo) {
        this.bookingRepo = bookingRepo;
        this.listingRepo = listingRepo;
        this.userRepo = userRepo;
        this.reviewRepo = reviewRepo;
        this.tournamentRepo = tournamentRepo;
    }

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingCreateDto dto, UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Listing listing = listingRepo.findActiveById(dto.listingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (dto.quantity() == null || dto.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (dto.startTime() == null || dto.endTime() == null || !dto.endTime().isAfter(dto.startTime())) {
            throw new IllegalArgumentException("Invalid booking time range");
        }

        if (dto.paymentMethod() == null) {
            throw new IllegalArgumentException("Payment method is required");
        }

        // Slot-based availability: sum quantity already booked in overlapping bookings
        List<Booking> overlapping = bookingRepo.findOverlappingBookings(
                dto.listingId(), dto.startTime(), dto.endTime());

        int alreadyBooked = overlapping.stream()
                .mapToInt(Booking::getQuantity)
                .sum();

        if (alreadyBooked + dto.quantity() > listing.getQuantity()) {
            int available = listing.getQuantity() - alreadyBooked;
            throw new IllegalArgumentException(
                    available <= 0
                            ? "This time slot is fully booked"
                            : "Only " + available + " available for this time slot"
            );
        }

        BigDecimal totalAmount = listing.getPricePerUnit()
                .multiply(BigDecimal.valueOf(dto.quantity()));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setListing(listing);
        booking.setStartTime(dto.startTime());
        booking.setEndTime(dto.endTime());
        booking.setQuantity(dto.quantity());
        booking.setTotalAmount(totalAmount);
        booking.setPaymentMethod(dto.paymentMethod());
        booking.setIsPaid(false);

        booking.setStatus(dto.paymentMethod() == PaymentMethod.CASH
                ? BookingStatus.CONFIRMED
                : BookingStatus.PENDING);

        bookingRepo.save(booking);

        // NOTE: listing.quantity is no longer decremented here — availability is
        // now computed dynamically per time slot via findOverlappingBookings.
        // This also means cancelBooking no longer needs to restore quantity.

        return toDto(booking);
    }

    @Override
    public List<BookedSlotDto> getBookedSlots(UUID listingId) {
        return bookingRepo.findUpcomingBookingsForListing(listingId, Instant.now())
                .stream()
                .map(b -> new BookedSlotDto(b.getStartTime(), b.getEndTime(), b.getQuantity()))
                .collect(Collectors.toList());
    }

    @Override
    public BookingResponseDto getBookingById(UUID id, UUID userId) {
        Booking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        boolean isRenter = booking.getUser().getUserid().equals(userId);
        boolean isOwner = booking.getListing().getOwner().getUserid().equals(userId);

        if (!isRenter && !isOwner) {
            throw new IllegalArgumentException("You do not have access to this booking");
        }

        return toDto(booking);
    }

    @Override
    public List<BookingResponseDto> getMyBookings(UUID userId) {
        return bookingRepo.findByUser_Userid(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingResponseDto> getBookingsForOwner(UUID ownerId) {
        return bookingRepo.findByListing_Owner_Userid(ownerId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponseDto cancelBooking(UUID id, UUID userId) {
        Booking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        boolean isRenter = booking.getUser().getUserid().equals(userId);
        boolean isOwner = booking.getListing().getOwner().getUserid().equals(userId);

        if (!isRenter && !isOwner) {
            throw new IllegalArgumentException("You do not have access to this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Completed bookings cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);
        // no listing.quantity restoration needed anymore — cancelled bookings are
        // simply excluded from the overlap query, freeing the slot automatically

        return toDto(booking);
    }

    @Override
    @Transactional
    public BookingResponseDto markCashPaymentReceived(UUID bookingId, UUID ownerId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getListing().getOwner().getUserid().equals(ownerId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        if (booking.getPaymentMethod() != PaymentMethod.CASH) {
            throw new IllegalArgumentException("This booking is not a cash payment");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot mark a cancelled booking as paid");
        }

        if (Boolean.TRUE.equals(booking.getIsPaid())) {
            throw new IllegalArgumentException("Payment already marked as received");
        }

        booking.setIsPaid(true);
        bookingRepo.save(booking);

        return toDto(booking);
    }

    private BookingResponseDto toDto(Booking booking) {
        List<ListingImage> images = booking.getListing().getImages();
        String imageUrl = (images != null && !images.isEmpty())
                ? images.get(0).getImageUrl()
                : null;

        var existingTournament = tournamentRepo.findByBooking_Id(booking.getId());

        return new BookingResponseDto(
                booking.getId(),
                booking.getListing().getId(),
                booking.getListing().getTitle(),
                imageUrl,
                booking.getListing().getCategory(),
                booking.getUser().getUserid(),
                booking.getUser().getName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getQuantity(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getPaymentMethod(),
                booking.getIsPaid(),
                reviewRepo.existsByBooking_Id(booking.getId()),
                existingTournament.isPresent(),
                existingTournament.map(Tournament::getId).orElse(null),
                booking.getCreatedAt()
        );
    }
}