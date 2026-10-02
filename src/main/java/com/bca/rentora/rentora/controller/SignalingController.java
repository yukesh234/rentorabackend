package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.dtos.livestream.ChatMessageDto;
import com.bca.rentora.rentora.dtos.livestream.SignalMessageDto;
import com.bca.rentora.rentora.repo.LiveStreamRepo;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.UUID;

@Controller
public class SignalingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final LiveStreamRepo liveStreamRepo;

    public SignalingController(SimpMessagingTemplate messagingTemplate, LiveStreamRepo liveStreamRepo) {
        this.messagingTemplate = messagingTemplate;
        this.liveStreamRepo = liveStreamRepo;
    }

    /**
     * WebRTC signaling relay — server never inspects the SDP/ICE payload,
     * just forwards it to the right topic.
     *
     * Client sends to:  /app/stream/{bookingId}/signal
     * Server broadcasts to: /topic/stream/{bookingId}/signal
     *
     * Every participant (broadcaster + all viewers) subscribes to the same
     * topic and filters messages client-side by checking if `targetId`
     * matches their own userId (or is null, for broadcast messages like "join").
     *
     * NOTE: broadcasting every signal to everyone on the topic is simple but
     * wasteful at scale — fine for a capstone's viewer counts, not for
     * production. A per-user queue (/user/queue/signal) would be the
     * production-grade approach.
     */
    @MessageMapping("/stream/{bookingId}/signal")
    public void relaySignal(@DestinationVariable String bookingId, SignalMessageDto message) {
        messagingTemplate.convertAndSend("/topic/stream/" + bookingId + "/signal", message);
    }

    /**
     * Live chat — client sends to /app/stream/{bookingId}/chat,
     * server broadcasts to /topic/stream/{bookingId}/chat.
     * Messages are dropped unless the stream is currently live.
     */
    @MessageMapping("/stream/{bookingId}/chat")
    public void relayChat(@DestinationVariable String bookingId, ChatMessageDto message) {
        if (!isLive(bookingId)) {
            return;
        }
        ChatMessageDto stamped = new ChatMessageDto(
                message.senderId(),
                message.senderName(),
                message.content(),
                Instant.now()
        );
        messagingTemplate.convertAndSend("/topic/stream/" + bookingId + "/chat", stamped);
    }

    private boolean isLive(String bookingId) {
        try {
            return liveStreamRepo.findByBooking_Id(UUID.fromString(bookingId))
                    .map(s -> Boolean.TRUE.equals(s.getIsLive()))
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            return false; // not a valid booking id
        }
    }
}