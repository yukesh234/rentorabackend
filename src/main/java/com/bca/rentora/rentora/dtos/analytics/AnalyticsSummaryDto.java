package com.bca.rentora.rentora.dtos.analytics;

import java.math.BigDecimal;

public record AnalyticsSummaryDto(
        Long totalBookings,
        Long confirmedBookings,
        Long pendingBookings,
        Long cancelledBookings,
        Long completedBookings,
        BigDecimal totalRevenue,
        BigDecimal outstandingCash,
        BigDecimal refundsOwed,
        Double cancellationRate,      // 0.0 - 1.0
        BigDecimal averageBookingValue,
        Long uniqueRenters,
        Double repeatRenterRate,      // 0.0 - 1.0, renters with 2+ bookings
        Long cashBookings,
        Long esewaBookings,
        Long previousTotalBookings,   // null when the range has no start date (all time)
        BigDecimal previousRevenue    // null when the range has no start date (all time)
) {}