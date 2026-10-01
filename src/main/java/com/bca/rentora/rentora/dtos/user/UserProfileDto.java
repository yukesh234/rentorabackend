package com.bca.rentora.rentora.dtos.user;


import java.time.Instant;
import java.util.UUID;

public record UserProfileDto(
        UUID userid,
        String name,
        String email,
        String profilePicture,
        com.bca.rentora.rentora.entity.Provider provider,
        Instant createdAt
) {}