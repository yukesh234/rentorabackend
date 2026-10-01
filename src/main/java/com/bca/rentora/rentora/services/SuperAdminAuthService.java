package com.bca.rentora.rentora.services;

import com.bca.rentora.rentora.dtos.auth.LoginReq;
import com.bca.rentora.rentora.dtos.auth.SuperAdminTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface SuperAdminAuthService {
    SuperAdminTokenResponse login(LoginReq loginReq, HttpServletResponse resp);
    SuperAdminTokenResponse refresh(HttpServletRequest request, HttpServletResponse resp);
    void logout(HttpServletRequest request, HttpServletResponse resp);
}