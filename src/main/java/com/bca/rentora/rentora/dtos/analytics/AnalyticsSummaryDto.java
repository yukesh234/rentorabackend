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
        Double cancellationRate // 0.0 - 1.0
) {}