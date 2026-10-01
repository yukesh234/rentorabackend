package com.bca.rentora.rentora.dtos.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class LoginReq {
    private final String email;
    private final String password;
}
