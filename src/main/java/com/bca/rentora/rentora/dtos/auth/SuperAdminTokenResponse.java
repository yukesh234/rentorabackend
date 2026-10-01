package com.bca.rentora.rentora.dtos.auth;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminTokenResponse {
    private String access_token;
    private String refresh_token;
    private String email;
    private String name;
}