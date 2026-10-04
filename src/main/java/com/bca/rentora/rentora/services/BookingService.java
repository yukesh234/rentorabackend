package com.bca.rentora.rentora.services;

import com.bca.rentora.rentora.dtos.booking.BookedSlotDto;
import com.bca.rentora.rentora.dtos.booking.BookingCreateDto;
import com.bca.rentora.rentora.dtos.booking.BookingResponseDto;

import java.util.List;
import java.util.UUID;

public interface BookingService {
    BookingResponseDto createBooking(BookingCreateDto dto, UUID userId);
    BookingResponseDto getBookingById(UUID id, UUID userId);
    List<BookingResponseDto> getMyBookings(UUID userId);
    List<BookingResponseDto> getBookingsForOwner(UUID ownerId);
    BookingResponseDto cancelBooking(UUID id, UUID userId);
    BookingResponseDto markCashPaymentReceived(UUID bookingId, UUID ownerId);
    List<BookedSlotDto> getBookedSlots(UUID listingId);
    BookingResponseDto markRefunded(UUID bookingId, UUID ownerId);
}