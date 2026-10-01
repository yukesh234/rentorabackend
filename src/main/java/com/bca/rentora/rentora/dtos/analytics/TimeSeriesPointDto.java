package com.bca.rentora.rentora.dtos.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeSeriesPointDto(
        LocalDate date,
        Long bookingCount,
        BigDecimal revenue
) {}