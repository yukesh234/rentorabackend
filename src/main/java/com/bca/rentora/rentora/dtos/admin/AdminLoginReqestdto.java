package com.bca.rentora.rentora.dtos.admin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class AdminLoginReqestdto {
    private final String username;
    private final String password;
}
