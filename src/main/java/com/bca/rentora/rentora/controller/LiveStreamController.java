package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.dtos.livestream.LiveStreamDto;
import com.bca.rentora.rentora.helpers.AuthHelper;
import com.bca.rentora.rentora.security.JWTService;
import com.bca.rentora.rentora.services.LiveStreamService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/livestreams")
public class LiveStreamController {

    private final LiveStreamService liveStreamService;
    private final AuthHelper authHelper;
    private final JWTService jwtService;

    public LiveStreamController(LiveStreamService liveStreamService, AuthHelper authHelper, JWTService jwtService) {
        this.liveStreamService = liveStreamService;
        this.authHelper = authHelper;
        this.jwtService = jwtService;
    }

    @PostMapping("/{bookingId}/start")
    public ResponseEntity<LiveStreamDto> start(@PathVariable UUID bookingId, HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(liveStreamService.startStream(bookingId, userId));
    }

    @PostMapping("/{bookingId}/end")
    public ResponseEntity<LiveStreamDto> end(@PathVariable UUID bookingId, HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(liveStreamService.endStream(bookingId, userId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<LiveStreamDto>> active() {
        return ResponseEntity.ok(liveStreamService.getActiveStreams());
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<LiveStreamDto> getByBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(liveStreamService.getStreamByBooking(bookingId));
    }
}