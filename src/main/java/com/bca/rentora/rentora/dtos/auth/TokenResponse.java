package com.bca.rentora.rentora.dtos.auth;

import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String access_token;
    private String refresh_token;
    private String token_type;
    private UserDto user;
    private long expires_in;

    public static TokenResponse of(
            String accessToken,
            String refreshToken,
            String tokenType,
            UserDto user,
            long expiresIn) {

        return new TokenResponse(
                accessToken,
                refreshToken,
                tokenType,
                user,
                expiresIn
        );
    }
}
