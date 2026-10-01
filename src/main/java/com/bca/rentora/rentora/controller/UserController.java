package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.dtos.user.ChangePasswordDto;
import com.bca.rentora.rentora.dtos.user.UpdateProfileDto;
import com.bca.rentora.rentora.dtos.user.UserProfileDto;
import com.bca.rentora.rentora.helpers.AuthHelper;
import com.bca.rentora.rentora.security.JWTService;
import com.bca.rentora.rentora.services.Userservice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final Userservice userService;
    private final AuthHelper authHelper;
    private final JWTService jwtService;

    public UserController(Userservice userService, AuthHelper authHelper, JWTService jwtService) {
        this.userService = userService;
        this.authHelper = authHelper;
        this.jwtService = jwtService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMe(HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateMe(@Valid @RequestBody UpdateProfileDto dto,
                                                   HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.updateProfile(userId, dto));
    }

    @PostMapping(value = "/me/profile-picture", consumes = "multipart/form-data")
    public ResponseEntity<UserProfileDto> uploadPicture(@RequestParam("file") MultipartFile file,
                                                        HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.uploadProfilePicture(userId, file));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordDto dto,
                                               HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        userService.changePassword(userId, dto);
        return ResponseEntity.ok().build();
    }
}