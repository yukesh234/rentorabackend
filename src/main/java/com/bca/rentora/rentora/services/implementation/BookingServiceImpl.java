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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kathmandu");

    private final BookingRepo bookingRepo;
    private final ListingRepo listingRepo;
    private final UserRepo userRepo;
    private final ReviewRepo reviewRepo;
    private final TournamentRepo tournamentRepo;
    private final PaymentRepo paymentRepo;

    public BookingServiceImpl(BookingRepo bookingRepo, ListingRepo listingRepo, UserRepo userRepo,
                              ReviewRepo reviewRepo, TournamentRepo tournamentRepo, PaymentRepo paymentRepo) {
        this.bookingRepo = bookingRepo;
        this.listingRepo = listingRepo;
        this.userRepo = userRepo;
        this.reviewRepo = reviewRepo;
        this.tournamentRepo = tournamentRepo;
        this.paymentRepo = paymentRepo;
    }

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingCreateDto dto, UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Locks the listing row until this transaction ends, so concurrent
        // bookings for the same listing are processed one at a time
        Listing listing = listingRepo.findActiveByIdForUpdate(dto.listingId())
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

        // venues can only be booked inside their opening hours
        validateOpeningHours(listing, dto.startTime(), dto.endTime());

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

        // price = pricePerUnit x duration units x quantity (same as the frontend estimate)
        double hours = Duration.between(dto.startTime(), dto.endTime()).toMinutes() / 60.0;
        String unit = listing.getPriceUnit() == null ? "" : listing.getPriceUnit();
        long units = switch (unit) {
            case "hour" -> (long) Math.ceil(hours);
            case "day" -> (long) Math.ceil(hours / 24);
            case "week" -> (long) Math.ceil(hours / (24 * 7));
            default -> 1L;
        };
        units = Math.max(units, 1L);

        BigDecimal totalAmount = listing.getPricePerUnit()
                .multiply(BigDecimal.valueOf(units))
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

        return toDto(booking);
    }

    /**
     * Venue listings (SPORTS / ENTERTAINMENT) with opening and closing times can only be
     * booked inside those hours, on a single day. UTILITY items have no hours and are skipped.
     */
    private void validateOpeningHours(Listing listing, Instant start, Instant end) {
        LocalTime open = listing.getOpeningTime();
        LocalTime close = listing.getClosingTime();

        if (listing.getCategory() == CategoryType.UTILITY || open == null || close == null) {
            return;
        }

        boolean closesAtMidnight = close.equals(LocalTime.MIDNIGHT);
        // overnight hours (closing earlier than opening) are not supported, so no check is applied
        if (!closesAtMidnight && !close.isAfter(open)) {
            return;
        }

        ZonedDateTime s = start.atZone(ZONE);
        ZonedDateTime e = end.atZone(ZONE);
        LocalDate day = s.toLocalDate();

        ZonedDateTime opensAt = day.atTime(open).atZone(ZONE);
        ZonedDateTime closesAt = closesAtMidnight
                ? day.plusDays(1).atStartOfDay(ZONE)
                : day.atTime(close).atZone(ZONE);

        if (s.isBefore(opensAt) || e.isAfter(closesAt)) {
            throw new IllegalArgumentException(
                    "This venue is open from " + open + " to " + close
                            + ". Please book within opening hours on a single day.");
        }
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

        if (booking.getEndTime().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Past bookings cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

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

    // owner confirms they sent the money back for a cancelled, paid eSewa booking
    @Override
    @Transactional
    public BookingResponseDto markRefunded(UUID bookingId, UUID ownerId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getListing().getOwner().getUserid().equals(ownerId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        boolean refundOwed = booking.getStatus() == BookingStatus.CANCELLED
                && booking.getPaymentMethod() == PaymentMethod.ESEWA
                && Boolean.TRUE.equals(booking.getIsPaid());
        if (!refundOwed) {
            throw new IllegalArgumentException("No refund is owed for this booking");
        }

        if (Boolean.TRUE.equals(booking.getRefunded())) {
            throw new IllegalArgumentException("Refund already marked as sent");
        }

        booking.setRefunded(true);
        bookingRepo.save(booking);

        paymentRepo.findByBooking_Id(bookingId).ifPresent(p -> {
            p.setStatus(PaymentStatus.REFUNDED);
            paymentRepo.save(p);
        });

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
                Boolean.TRUE.equals(booking.getRefunded()),
                reviewRepo.existsByBooking_Id(booking.getId()),
                existingTournament.isPresent(),
                existingTournament.map(Tournament::getId).orElse(null),
                booking.getCreatedAt()
        );
    }
}