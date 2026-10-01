package com.bca.rentora.rentora.security;

import com.bca.rentora.rentora.entity.Provider;
import com.bca.rentora.rentora.entity.RefreshToken;
import com.bca.rentora.rentora.entity.User;
import com.bca.rentora.rentora.repo.RefToken;
import com.bca.rentora.rentora.repo.UserRepo;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Logger;

@Component
public class Oauthsec implements AuthenticationSuccessHandler {
    private final UserRepo userRepo;
    private final JWTService jwtService;
    private final RefToken refresh;
    private final CookieService cookieService;
    private final Logger logger = Logger.getLogger(Oauthsec.class.getName());

    public Oauthsec(UserRepo userRepo, JWTService jwtService, RefToken refresh, CookieService cookieService) {
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.refresh = refresh;
        this.cookieService = cookieService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String image = oAuth2User.getAttribute("picture");

        if (email == null) {
            logger.warning("Google OAuth2 login did not return an email attribute");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by Google");
            return;
        }

        User user = userRepo.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .password(null) // no password for OAuth2 users
                    .provider(Provider.GOOGLE)
                    .enable(true)
                    .profilePicture(image)
                    .roles(new HashSet<>())
                    .build();
            return userRepo.save(newUser);
        });

        // generate refresh token + persist it (same flow as normal login)
        String jti = UUID.randomUUID().toString();
        RefreshToken refTokenDB = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenttl()))
                .revoked(false)
                .build();
        refresh.save(refTokenDB);

//        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefereshToken(user, jti);

        cookieService.attachCookie(response, refreshToken, (int) jwtService.getRefreshTokenttl());
        cookieService.addNoStoreHeader(response);

        logger.info("OAuth2 login success for: " + email);

        response.sendRedirect("http://localhost:5173/oauth2/success");
    }
}
