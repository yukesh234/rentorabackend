package com.bca.rentora.rentora.dtos.auth;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDto {
    private String email;
    private String password;
    private String name;
}
