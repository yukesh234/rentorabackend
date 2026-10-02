package com.bca.rentora.rentora.services;

import com.bca.rentora.rentora.dtos.livestream.LiveStreamDto;

import java.util.List;
import java.util.UUID;

public interface LiveStreamService {
    LiveStreamDto startStream(UUID bookingId, UUID ownerId);
    LiveStreamDto endStream(UUID bookingId, UUID ownerId);
    List<LiveStreamDto> getActiveStreams();
    LiveStreamDto getStreamByBooking(UUID bookingId);
    // ends every live stream whose booking window has passed (called by the scheduler)
    void endExpiredStreams();
}