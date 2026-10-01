package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.auth.LoginReq;
import com.bca.rentora.rentora.dtos.auth.SuperAdminTokenResponse;
import com.bca.rentora.rentora.security.CookieService;
import com.bca.rentora.rentora.security.JWTService;
import com.bca.rentora.rentora.services.SuperAdminAuthService;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

@Service
public class SuperAdminAuthServiceImpl implements SuperAdminAuthService {

    private final JWTService jwtService;
    private final CookieService cookieService;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;

    public SuperAdminAuthServiceImpl(
            JWTService jwtService,
            CookieService cookieService,
            @Value("${security.superadmin.email}") String adminEmail,
            @Value("${security.superadmin.password}") String adminPassword,
            @Value("${security.superadmin.name}") String adminName) {
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
        this.adminPassword = adminPassword == null ? "" : adminPassword;
        this.adminName = adminName;
    }

    // fail fast at startup if the env vars are missing
    @PostConstruct
    void validateConfig() {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "SUPER_ADMIN_EMAIL and SUPER_ADMIN_PASSWORD must be set");
        }
    }

    @Override
    public SuperAdminTokenResponse login(LoginReq loginReq, HttpServletResponse resp) {
        String email = loginReq.getEmail() == null ? "" : loginReq.getEmail().trim().toLowerCase();
        String password = loginReq.getPassword() == null ? "" : loginReq.getPassword();

        // constant-time compare; '&' (not '&&') so both checks always run
        boolean emailOk = MessageDigest.isEqual(
                email.getBytes(StandardCharsets.UTF_8), adminEmail.getBytes(StandardCharsets.UTF_8));
        boolean passOk = MessageDigest.isEqual(
                password.getBytes(StandardCharsets.UTF_8), adminPassword.getBytes(StandardCharsets.UTF_8));

        if (!(emailOk & passOk)) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return issueTokens(resp);
    }

    @Override
    public SuperAdminTokenResponse refresh(HttpServletRequest request, HttpServletResponse resp) {
        String refreshToken = readRefreshCookie(request);
        try {
            if (!jwtService.isRefreshToken(refreshToken) || !jwtService.isSuperAdminToken(refreshToken)) {
                throw new BadCredentialsException("Invalid refresh token");
            }
        } catch (JwtException | IllegalArgumentException e) { // expired, tampered, malformed
            throw new BadCredentialsException("Invalid refresh token");
        }
        return issueTokens(resp);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse resp) {
        // stateless: nothing to revoke in DB, just clear the cookie
        cookieService.clearCookie(resp);
        cookieService.addNoStoreHeader(resp);
        SecurityContextHolder.clearContext();
    }

    private SuperAdminTokenResponse issueTokens(HttpServletResponse resp) {
        String access = jwtService.generateSuperAdminAccessToken(adminEmail);
        String refresh = jwtService.generateSuperAdminRefreshToken(adminEmail);

        cookieService.attachCookie(resp, refresh, (int) jwtService.getRefreshTokenttl());
        cookieService.addNoStoreHeader(resp);

        return SuperAdminTokenResponse.builder()
                .access_token(access)
                .refresh_token(refresh)
                .email(adminEmail)
                .name(adminName)
                .build();
    }

    private String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new BadCredentialsException("No refresh token found");
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieService.getCookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new BadCredentialsException("No refresh token found"));
    }
}