package com.bca.rentora.rentora.services;

import com.bca.rentora.rentora.dtos.auth.LoginReq;
import com.bca.rentora.rentora.dtos.auth.TokenResponse;
import com.bca.rentora.rentora.dtos.auth.UserDto;
import com.bca.rentora.rentora.dtos.auth.UserRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public interface AuthService {
    UserDto Register(UserRequestDto userRequestDto);
    TokenResponse Login(LoginReq loginReq, HttpServletResponse resp);
    TokenResponse refresh(HttpServletRequest request, HttpServletResponse resp);
    void logout(HttpServletRequest req, HttpServletResponse resp);
}
