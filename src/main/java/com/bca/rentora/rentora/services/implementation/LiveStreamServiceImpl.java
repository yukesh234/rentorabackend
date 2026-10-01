package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.livestream.LiveStreamDto;
import com.bca.rentora.rentora.entity.Booking;
import com.bca.rentora.rentora.entity.LiveStream;
import com.bca.rentora.rentora.exceptions.ResourceNotFoundException;
import com.bca.rentora.rentora.repo.BookingRepo;
import com.bca.rentora.rentora.repo.LiveStreamRepo;
import com.bca.rentora.rentora.services.LiveStreamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LiveStreamServiceImpl implements LiveStreamService {

    private final LiveStreamRepo liveStreamRepo;
    private final BookingRepo bookingRepo;

    private static final Duration EARLY_START_WINDOW = Duration.ofMinutes(30);

    public LiveStreamServiceImpl(LiveStreamRepo liveStreamRepo, BookingRepo bookingRepo) {
        this.liveStreamRepo = liveStreamRepo;
        this.bookingRepo = bookingRepo;
    }

    @Override
    @Transactional
    public LiveStreamDto startStream(UUID bookingId, UUID renterId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getUserid().equals(renterId)) {
            throw new IllegalArgumentException("You do not own this booking");
        }

        Instant now = Instant.now();
        Instant earliestAllowed = booking.getStartTime().minus(EARLY_START_WINDOW);
        if (now.isBefore(earliestAllowed)) {
            throw new IllegalArgumentException("Too early to go live for this booking");
        }
        if (now.isAfter(booking.getEndTime())) {
            throw new IllegalArgumentException("This booking's time window has already ended");
        }

        LiveStream stream = liveStreamRepo.findByBooking_Id(bookingId)
                .orElseGet(() -> {
                    LiveStream s = new LiveStream();
                    s.setBooking(booking);
                    return s;
                });

        if (Boolean.TRUE.equals(stream.getIsLive())) {
            throw new IllegalArgumentException("Stream is already live");
        }

        stream.setIsLive(true);
        stream.setStartedAt(now);
        stream.setEndedAt(null);
        liveStreamRepo.save(stream);

        return toDto(stream);
    }

    @Override
    @Transactional
    public LiveStreamDto endStream(UUID bookingId, UUID renterId) {
        LiveStream stream = liveStreamRepo.findByBooking_Id(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));

        if (!stream.getBooking().getUser().getUserid().equals(renterId)) {
            throw new IllegalArgumentException("You do not own this booking");
        }

        stream.setIsLive(false);
        stream.setEndedAt(Instant.now());
        liveStreamRepo.save(stream);

        return toDto(stream);
    }

    @Override
    public List<LiveStreamDto> getActiveStreams() {
        return liveStreamRepo.findByIsLiveTrue()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public LiveStreamDto getStreamByBooking(UUID bookingId) {
        LiveStream stream = liveStreamRepo.findByBooking_Id(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));
        return toDto(stream);
    }

    private LiveStreamDto toDto(LiveStream stream) {
        return new LiveStreamDto(
                stream.getId(),
                stream.getBooking().getId(),
                stream.getBooking().getListing().getTitle(),
                stream.getBooking().getUser().getUserid(),
                stream.getBooking().getUser().getName(),
                stream.getIsLive(),
                stream.getStartedAt()
        );
    }
}