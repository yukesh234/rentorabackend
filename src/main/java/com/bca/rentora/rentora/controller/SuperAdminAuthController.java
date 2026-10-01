package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.dtos.admin.AdminUserSummaryDto;
import com.bca.rentora.rentora.dtos.admin.PendingListingDto;
import com.bca.rentora.rentora.dtos.auth.LoginReq;
import com.bca.rentora.rentora.dtos.auth.SuperAdminTokenResponse;
import com.bca.rentora.rentora.services.AdminModerationService;
import com.bca.rentora.rentora.services.SuperAdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/super-admin/auth")
@RequiredArgsConstructor
public class SuperAdminAuthController {

    private final SuperAdminAuthService superAdminAuthService;

    @PostMapping("/login")
    public ResponseEntity<SuperAdminTokenResponse> login(
            @RequestBody LoginReq loginReq, HttpServletResponse response) {
        return ResponseEntity.ok(superAdminAuthService.login(loginReq, response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<SuperAdminTokenResponse> refresh(
            HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(superAdminAuthService.refresh(request, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        superAdminAuthService.logout(request, response);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}