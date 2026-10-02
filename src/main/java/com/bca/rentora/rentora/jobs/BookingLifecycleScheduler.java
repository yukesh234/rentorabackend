package com.bca.rentora.rentora.jobs;

import com.bca.rentora.rentora.entity.Booking;
import com.bca.rentora.rentora.entity.BookingStatus;
import com.bca.rentora.rentora.entity.PaymentMethod;
import com.bca.rentora.rentora.repo.BookingRepo;
import com.bca.rentora.rentora.services.LiveStreamService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs once a minute:
 *  1. cancels eSewa bookings that were never paid (frees the slot),
 *  2. marks confirmed bookings whose time has passed as COMPLETED,
 *  3. ends live streams whose booking window is over.
 * Each step is isolated so one failure doesn't stop the others.
 */
@Component
public class BookingLifecycleScheduler {

    private static final Logger log = Logger.getLogger(BookingLifecycleScheduler.class.getName());
    private static final Duration UNPAID_ESEWA_TTL = Duration.ofMinutes(30);

    private final BookingRepo bookingRepo;
    private final LiveStreamService liveStreamService;

    public BookingLifecycleScheduler(BookingRepo bookingRepo, LiveStreamService liveStreamService) {
        this.bookingRepo = bookingRepo;
        this.liveStreamService = liveStreamService;
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 60_000)
    public void run() {
        Instant now = Instant.now();

        try {
            List<Booking> stale = bookingRepo.findByStatusAndPaymentMethodAndCreatedAtBefore(
                    BookingStatus.PENDING, PaymentMethod.ESEWA, now.minus(UNPAID_ESEWA_TTL));
            stale.forEach(b -> b.setStatus(BookingStatus.CANCELLED));
            bookingRepo.saveAll(stale);
            if (!stale.isEmpty()) log.info("Cancelled " + stale.size() + " unpaid eSewa booking(s)");
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to expire unpaid bookings", e);
        }

        try {
            List<Booking> ended = bookingRepo.findByStatusAndEndTimeBefore(BookingStatus.CONFIRMED, now);
            ended.forEach(b -> b.setStatus(BookingStatus.COMPLETED));
            bookingRepo.saveAll(ended);
            if (!ended.isEmpty()) log.info("Completed " + ended.size() + " finished booking(s)");
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to complete finished bookings", e);
        }

        try {
            liveStreamService.endExpiredStreams();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to end expired live streams", e);
        }
    }
}