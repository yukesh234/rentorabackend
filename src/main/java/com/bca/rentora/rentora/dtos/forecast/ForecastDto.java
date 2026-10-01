package com.bca.rentora.rentora.dtos.forecast;

import java.util.List;

// What the React app receives, one per category
public record ForecastDto(
        String category,
        Integer weekOfYear,
        Boolean available,          // false if the Python service couldn't be reached
        Integer predictedBookings,
        Double predictedRaw,
        Boolean lowConfidence,
        List<Integer> last4Bookings,
        Integer currentBookings,
        Integer currentCustomers
) {}