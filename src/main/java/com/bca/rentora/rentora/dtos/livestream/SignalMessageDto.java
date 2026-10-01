package com.bca.rentora.rentora.dtos.livestream;

public record SignalMessageDto(
        String type,       // "offer" | "answer" | "ice-candidate" | "join" | "leave"
        String senderId,    // UUID of the sending user, as string
        String targetId,    // UUID of the intended recipient, null for broadcast (e.g. "join")
        String payload       // JSON-stringified SDP or ICE candidate — kept opaque to the backend
) {}