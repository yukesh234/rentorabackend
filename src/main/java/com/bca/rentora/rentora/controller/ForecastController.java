package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.dtos.forecast.ForecastDto;
import com.bca.rentora.rentora.helpers.AuthHelper;
import com.bca.rentora.rentora.security.JWTService;
import com.bca.rentora.rentora.services.ForecastService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final ForecastService forecastService;
    private final AuthHelper authHelper;
    private final JWTService jwtService;

    public ForecastController(ForecastService forecastService, AuthHelper authHelper, JWTService jwtService) {
        this.forecastService = forecastService;
        this.authHelper = authHelper;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<List<ForecastDto>> getForecast(
            @RequestParam(required = false) UUID listingId,
            @RequestParam(defaultValue = "false") boolean festival,
            @RequestParam(defaultValue = "false") boolean promo,
            @RequestParam(defaultValue = "false") boolean schoolHoliday,
            HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(
                forecastService.getForecast(userId, listingId, festival, promo, schoolHoliday));
    }
}