package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.dtos.analytics.*;
import com.bca.rentora.rentora.helpers.AuthHelper;
import com.bca.rentora.rentora.security.JWTService;
import com.bca.rentora.rentora.services.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AuthHelper authHelper;
    private final JWTService jwtService;

    public AnalyticsController(AnalyticsService analyticsService, AuthHelper authHelper, JWTService jwtService) {
        this.analyticsService = analyticsService;
        this.authHelper = authHelper;
        this.jwtService = jwtService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDto> getSummary(
            @RequestParam(required = false) UUID listingId,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(analyticsService.getSummary(userId, listingId, startDate, endDate));
    }

    @GetMapping("/bookings-over-time")
    public ResponseEntity<List<TimeSeriesPointDto>> getBookingsOverTime(
            @RequestParam(required = false) UUID listingId,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(analyticsService.getBookingsOverTime(userId, listingId, startDate, endDate));
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<List<CategoryBreakdownDto>> getCategoryBreakdown(
            @RequestParam(required = false) UUID listingId,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(analyticsService.getCategoryBreakdown(userId, listingId, startDate, endDate));
    }

    @GetMapping("/busiest-days")
    public ResponseEntity<List<WeekdayPointDto>> getBusiestDays(
            @RequestParam(required = false) UUID listingId,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(analyticsService.getBusiestDays(userId, listingId, startDate, endDate));
    }

    @GetMapping("/top-listings")
    public ResponseEntity<List<TopListingDto>> getTopListings(
            @RequestParam(defaultValue = "5") int limit,
            HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(analyticsService.getTopListings(userId, limit));
    }

    @GetMapping("/listing-options")
    public ResponseEntity<List<ListingOptionDto>> getListingOptions(HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(analyticsService.getOwnerListingOptions(userId));
    }
}