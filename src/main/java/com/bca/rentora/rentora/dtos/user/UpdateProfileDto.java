package com.bca.rentora.rentora.dtos.user;


import jakarta.validation.constraints.NotBlank;

public record UpdateProfileDto(
        @NotBlank(message = "Name cannot be empty")
        String name
) {}
