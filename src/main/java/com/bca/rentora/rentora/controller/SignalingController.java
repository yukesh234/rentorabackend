package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.dtos.livestream.ChatMessageDto;
import com.bca.rentora.rentora.dtos.livestream.SignalMessageDto;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Controller
public class SignalingController {

    private final SimpMessagingTemplate messagingTemplate;

    public SignalingController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
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
     */
    @MessageMapping("/stream/{bookingId}/chat")
    public void relayChat(@DestinationVariable String bookingId, ChatMessageDto message) {
        ChatMessageDto stamped = new ChatMessageDto(
                message.senderId(),
                message.senderName(),
                message.content(),
                Instant.now()
        );
        messagingTemplate.convertAndSend("/topic/stream/" + bookingId + "/chat", stamped);
    }
}