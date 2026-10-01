package com.bca.rentora.rentora.helpers;

import com.bca.rentora.rentora.dtos.auth.LoginReq;
import com.bca.rentora.rentora.entity.User;
import com.bca.rentora.rentora.repo.UserRepo;
import com.bca.rentora.rentora.security.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Getter
@Setter
@Component
@AllArgsConstructor
@NoArgsConstructor

public class AuthHelper {
    @Autowired
    private AuthenticationManager authenticationManager;
    private JWTService jwtService;
    private UserRepo userRepo;

    public Authentication authenicate(LoginReq loginReq) {
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginReq.getEmail(),loginReq.getPassword()));
    }

    public String getAccessToken(HttpServletRequest request) {
        String jwt = request.getHeader("Authorization");
        if(jwt == null || !jwt.startsWith("Bearer ")) {
            return null;
        }
        return jwt.substring(7);
    }

    public UUID resolveCurrentUserId(HttpServletRequest request) {
        String token = this.getAccessToken(request);
        try {
            String email = jwtService.getEmailFromJWT(token);
            System.out.println(email);
            return userRepo.findByEmail(email)
                    .map(User::getUserid)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
