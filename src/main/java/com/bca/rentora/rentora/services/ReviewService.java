package com.bca.rentora.rentora.services;

import com.bca.rentora.rentora.dtos.review.ReviewCreateDto;
import com.bca.rentora.rentora.dtos.review.ReviewResponseDto;

import java.util.List;
import java.util.UUID;

public interface ReviewService {
    ReviewResponseDto submitReview(ReviewCreateDto dto, UUID userId);
    List<ReviewResponseDto> getReviewsForListing(UUID listingId);
}