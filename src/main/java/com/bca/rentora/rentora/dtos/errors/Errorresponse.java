package com.bca.rentora.rentora.dtos.errors;

import org.springframework.http.HttpStatus;

public record Errorresponse(
        String message,
        HttpStatus status
) {
}
