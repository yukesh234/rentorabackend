package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.core.service.EsewaService;
import com.bca.rentora.rentora.dtos.booking.BookedSlotDto;
import com.bca.rentora.rentora.entity.User;
import com.bca.rentora.rentora.helpers.AuthHelper;
import com.bca.rentora.rentora.dtos.booking.BookingCreateDto;
import com.bca.rentora.rentora.dtos.booking.BookingResponseDto;

import com.bca.rentora.rentora.security.JWTService;
import com.bca.rentora.rentora.services.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final AuthHelper authUtil;
    private final JWTService jwtService;
    public BookingController(BookingService bookingService, AuthHelper authHelper, JWTService jwtService) {
        this.bookingService = bookingService;
        this.authUtil = authHelper;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<BookingResponseDto> create(@RequestBody BookingCreateDto dto,
                                                     HttpServletRequest request) {
        System.out.println("Create Booking route hit");
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(bookingService.createBooking(dto, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDto> getById(@PathVariable UUID id,
                                                      HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(bookingService.getBookingById(id, userId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<BookingResponseDto>> myBookings(HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(bookingService.getMyBookings(userId));
    }

    @GetMapping("/owner")
    public ResponseEntity<List<BookingResponseDto>> ownerBookings(HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(bookingService.getBookingsForOwner(userId));
    }


    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDto> cancel(@PathVariable UUID id,
                                                     HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(bookingService.cancelBooking(id, userId));
    }

    @PatchMapping("/{id}/mark-paid")
    public ResponseEntity<BookingResponseDto> markPaid(@PathVariable UUID id,
                                                       HttpServletRequest request) {
        String JWT = request.getHeader("Authorization");
        String token = JWT.substring(7);
        UUID userId = jwtService.getUserIdFromJwt(token);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(bookingService.markCashPaymentReceived(id, userId));
    }
    @GetMapping("/listing/{listingId}/booked-slots")
    public ResponseEntity<List<BookedSlotDto>> getBookedSlots(@PathVariable UUID listingId) {
        return ResponseEntity.ok(bookingService.getBookedSlots(listingId));
    }
}