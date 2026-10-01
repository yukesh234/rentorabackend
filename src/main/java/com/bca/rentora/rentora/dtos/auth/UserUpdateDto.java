package com.bca.rentora.rentora.dtos.auth;


import com.bca.rentora.rentora.entity.Provider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDto {
    private boolean enable;
    private String password;
    private String name;
    private Provider provider;
}
