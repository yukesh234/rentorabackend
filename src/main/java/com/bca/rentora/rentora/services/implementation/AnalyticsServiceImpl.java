package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.analytics.*;
import com.bca.rentora.rentora.entity.Booking;
import com.bca.rentora.rentora.entity.BookingStatus;
import com.bca.rentora.rentora.entity.PaymentMethod;
import com.bca.rentora.rentora.repo.AnalyticsRepo;
import com.bca.rentora.rentora.repo.ListingRepo;
import com.bca.rentora.rentora.repo.ReviewRepo;
import com.bca.rentora.rentora.services.AnalyticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepo analyticsRepo;
    private final ReviewRepo reviewRepo;
    private final ListingRepo listingRepo;
    private static final Instant EPOCH_START = Instant.EPOCH;

    private Instant orDefaultStart(Instant startDate) {
        return startDate != null ? startDate : EPOCH_START;
    }

    private Instant orDefaultEnd(Instant endDate) {
        return endDate != null ? endDate : Instant.now();
    }

    private static final ZoneId ZONE = ZoneId.of("Asia/Kathmandu");

    public AnalyticsServiceImpl(AnalyticsRepo analyticsRepo, ReviewRepo reviewRepo, ListingRepo listingRepo) {
        this.analyticsRepo = analyticsRepo;
        this.reviewRepo = reviewRepo;
        this.listingRepo = listingRepo;
    }

    @Override
    public AnalyticsSummaryDto getSummary(UUID ownerId, UUID listingId, Instant startDate, Instant endDate) {
        List<Booking> bookings = analyticsRepo.findForOwner(ownerId, listingId, orDefaultStart(startDate), orDefaultEnd(endDate));

        long total = bookings.size();
        long confirmed = count(bookings, BookingStatus.CONFIRMED);
        long pending = count(bookings, BookingStatus.PENDING);
        long cancelled = count(bookings, BookingStatus.CANCELLED);
        long completed = count(bookings, BookingStatus.COMPLETED);

        BigDecimal totalRevenue = bookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .filter(b -> b.getPaymentMethod() == PaymentMethod.ESEWA || Boolean.TRUE.equals(b.getIsPaid()))
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstandingCash = bookings.stream()
                .filter(b -> b.getPaymentMethod() == PaymentMethod.CASH)
                .filter(b -> !Boolean.TRUE.equals(b.getIsPaid()))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refundsOwed = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                .filter(b -> b.getPaymentMethod() == PaymentMethod.ESEWA)
                .filter(b -> Boolean.TRUE.equals(b.getIsPaid()))
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double cancellationRate = total == 0 ? 0.0 : (double) cancelled / total;

        return new AnalyticsSummaryDto(
                total, confirmed, pending, cancelled, completed,
                totalRevenue, outstandingCash, refundsOwed, cancellationRate
        );
    }

    @Override
    public List<TimeSeriesPointDto> getBookingsOverTime(UUID ownerId, UUID listingId, Instant startDate, Instant endDate) {
        List<Booking> bookings = analyticsRepo.findForOwner(ownerId, listingId, orDefaultStart(startDate), orDefaultEnd(endDate));

        Map<LocalDate, List<Booking>> byDate = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getCreatedAt().atZone(ZONE).toLocalDate()));

        LocalDate start = startDate != null
                ? startDate.atZone(ZONE).toLocalDate()
                : bookings.stream()
                .map(b -> b.getCreatedAt().atZone(ZONE).toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now(ZONE));
        LocalDate end = endDate != null ? endDate.atZone(ZONE).toLocalDate() : LocalDate.now(ZONE);

        List<TimeSeriesPointDto> series = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            List<Booking> dayBookings = byDate.getOrDefault(d, List.of());
            BigDecimal dayRevenue = dayBookings.stream()
                    .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                    .map(Booking::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            series.add(new TimeSeriesPointDto(d, (long) dayBookings.size(), dayRevenue));
        }
        return series;
    }

    @Override
    public List<CategoryBreakdownDto> getCategoryBreakdown(UUID ownerId, UUID listingId, Instant startDate, Instant endDate) {
        List<Booking> bookings = analyticsRepo.findForOwner(ownerId, listingId, startDate, endDate);

        Map<String, Long> counts = bookings.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getListing().getCategory().name(),
                        Collectors.counting()
                ));

        return counts.entrySet().stream()
                .map(e -> new CategoryBreakdownDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CategoryBreakdownDto::category))
                .collect(Collectors.toList());
    }

    @Override
    public List<TopListingDto> getTopListings(UUID ownerId, int limit) {
        List<Booking> bookings = analyticsRepo.findForOwner(ownerId, null, EPOCH_START, Instant.now());

        Map<UUID, List<Booking>> byListing = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getListing().getId()));

        return byListing.entrySet().stream()
                .map(entry -> {
                    UUID listingId = entry.getKey();
                    List<Booking> listingBookings = entry.getValue();
                    String title = listingBookings.get(0).getListing().getTitle();

                    BigDecimal revenue = listingBookings.stream()
                            .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                            .map(Booking::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    List<com.bca.rentora.rentora.entity.Review> reviews = reviewRepo.findByListing_Id(listingId);
                    Double avgRating = reviews.isEmpty()
                            ? null
                            : reviews.stream().mapToInt(com.bca.rentora.rentora.entity.Review::getRating).average().orElse(0);

                    return new TopListingDto(listingId, title, (long) listingBookings.size(), revenue, avgRating);
                })
                .sorted(Comparator.comparing(TopListingDto::bookingCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<ListingOptionDto> getOwnerListingOptions(UUID ownerId) {
        return listingRepo.findByOwner_useridAndDeletedAtIsNull(ownerId)
                .stream()
                .map(l -> new ListingOptionDto(l.getId(), l.getTitle()))
                .collect(Collectors.toList());
    }

    private long count(List<Booking> bookings, BookingStatus status) {
        return bookings.stream().filter(b -> b.getStatus() == status).count();
    }
}