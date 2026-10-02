package com.bca.rentora.rentora.dtos.forecast;

import java.util.List;
import java.util.UUID;

// What the React app receives: one per category, or one per listing when grouped by product
public record ForecastDto(
        String category,
        Integer weekOfYear,
        Boolean available,          // false if the Python service couldn't be reached
        Integer predictedBookings,
        Double predictedRaw,
        Boolean lowConfidence,
        List<Integer> last4Bookings,
        Integer currentBookings,
        Integer currentCustomers,
        UUID listingId,             // null when grouped by category
        String listingTitle         // null when grouped by category
) {}