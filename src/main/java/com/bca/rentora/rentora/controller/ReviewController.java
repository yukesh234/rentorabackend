package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.dtos.review.ReviewCreateDto;
import com.bca.rentora.rentora.dtos.review.ReviewResponseDto;
import com.bca.rentora.rentora.helpers.AuthHelper;
import com.bca.rentora.rentora.security.JWTService;
import com.bca.rentora.rentora.services.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final JWTService jwtService;


    public ReviewController(ReviewService reviewService, JWTService jwtService) {
        this.reviewService = reviewService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponseDto> submit(@Valid @RequestBody ReviewCreateDto dto,
                                                    HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(reviewService.submitReview(dto, userId));
    }

    @GetMapping("/listing/{listingId}")
    public ResponseEntity<List<ReviewResponseDto>> getForListing(@PathVariable UUID listingId) {
        return ResponseEntity.ok(reviewService.getReviewsForListing(listingId));
    }
}