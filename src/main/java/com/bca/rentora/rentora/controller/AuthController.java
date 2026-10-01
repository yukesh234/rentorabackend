package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.dtos.auth.LoginReq;
import com.bca.rentora.rentora.dtos.auth.TokenResponse;
import com.bca.rentora.rentora.dtos.auth.UserDto;
import com.bca.rentora.rentora.dtos.auth.UserRequestDto;
import com.bca.rentora.rentora.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody UserRequestDto userRequestDto){
        return ResponseEntity.ok().body(authService.Register(userRequestDto));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse>login(@RequestBody LoginReq loginReq, HttpServletResponse resp){
        return ResponseEntity.ok().body(authService.Login(loginReq,resp));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> Refresh(HttpServletRequest req, HttpServletResponse resp){
        return ResponseEntity.ok().body(authService.refresh(req,resp));
    }

    @PostMapping("/logout")
    public ResponseEntity Logout(HttpServletRequest req, HttpServletResponse resp){
        authService.logout(req,resp);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
