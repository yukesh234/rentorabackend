package com.bca.rentora.rentora.dtos.livestream;


import java.time.Instant;

public record ChatMessageDto(
        String senderId,
        String senderName,
        String content,
        Instant sentAt
) {}