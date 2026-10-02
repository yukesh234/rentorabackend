package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.livestream.LiveStreamDto;
import com.bca.rentora.rentora.dtos.livestream.SignalMessageDto;
import com.bca.rentora.rentora.entity.Booking;
import com.bca.rentora.rentora.entity.LiveStream;
import com.bca.rentora.rentora.exceptions.ResourceNotFoundException;
import com.bca.rentora.rentora.repo.BookingRepo;
import com.bca.rentora.rentora.repo.LiveStreamRepo;
import com.bca.rentora.rentora.services.LiveStreamService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;

    private static final Duration EARLY_START_WINDOW = Duration.ofMinutes(30);

    public LiveStreamServiceImpl(LiveStreamRepo liveStreamRepo, BookingRepo bookingRepo,
                                 SimpMessagingTemplate messagingTemplate) {
        this.liveStreamRepo = liveStreamRepo;
        this.bookingRepo = bookingRepo;
        this.messagingTemplate = messagingTemplate;
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

        // already ended: nothing to do, and don't tell the viewers twice
        if (!Boolean.TRUE.equals(stream.getIsLive())) {
            return toDto(stream);
        }

        stream.setIsLive(false);
        stream.setEndedAt(Instant.now());
        liveStreamRepo.save(stream);

        broadcastEnded(stream);

        return toDto(stream);
    }

    @Override
    @Transactional
    public void endExpiredStreams() {
        Instant now = Instant.now();
        for (LiveStream stream : liveStreamRepo.findByIsLiveTrue()) {
            if (stream.getBooking().getEndTime().isBefore(now)) {
                stream.setIsLive(false);
                stream.setEndedAt(now);
                liveStreamRepo.save(stream);
                broadcastEnded(stream);
            }
        }
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

    // tells everyone watching that the stream is over, so the viewer page can show "live ended"
    private void broadcastEnded(LiveStream stream) {
        UUID bookingId = stream.getBooking().getId();
        String broadcasterId = stream.getBooking().getUser().getUserid().toString();
        messagingTemplate.convertAndSend(
                "/topic/stream/" + bookingId + "/signal",
                new SignalMessageDto("stream-ended", broadcasterId, null, ""));
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