package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.review.ReviewCreateDto;
import com.bca.rentora.rentora.dtos.review.ReviewResponseDto;
import com.bca.rentora.rentora.entity.Booking;
import com.bca.rentora.rentora.entity.BookingStatus;
import com.bca.rentora.rentora.entity.Review;
import com.bca.rentora.rentora.entity.User;
import com.bca.rentora.rentora.exceptions.ResourceNotFoundException;
import com.bca.rentora.rentora.repo.BookingRepo;
import com.bca.rentora.rentora.repo.ReviewRepo;
import com.bca.rentora.rentora.repo.UserRepo;
import com.bca.rentora.rentora.services.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepo reviewRepo;
    private final BookingRepo bookingRepo;
    private final UserRepo userRepo;

    public ReviewServiceImpl(ReviewRepo reviewRepo, BookingRepo bookingRepo, UserRepo userRepo) {
        this.reviewRepo = reviewRepo;
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
    }

    @Override
    @Transactional
    public ReviewResponseDto submitReview(ReviewCreateDto dto, UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepo.findById(dto.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getUserid().equals(userId)) {
            throw new IllegalArgumentException("You can only review your own bookings");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot review a cancelled booking");
        }

        if (booking.getEndTime().isAfter(Instant.now())) {
            throw new IllegalArgumentException("You can only review a booking after it has ended");
        }

        if (reviewRepo.existsByBooking_Id(dto.bookingId())) {
            throw new IllegalArgumentException("You have already reviewed this booking");
        }

        Review review = new Review();
        review.setUser(user);
        review.setListing(booking.getListing());
        review.setBooking(booking);
        review.setRating(dto.rating());
        review.setComment(dto.comment());

        reviewRepo.save(review);

        return toDto(review);
    }

    @Override
    public List<ReviewResponseDto> getReviewsForListing(UUID listingId) {
        return reviewRepo.findByListing_Id(listingId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ReviewResponseDto toDto(Review review) {
        return new ReviewResponseDto(
                review.getId(),
                review.getListing().getId(),
                review.getUser().getUserid(),
                review.getUser().getName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}