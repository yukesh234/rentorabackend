package com.bca.rentora.rentora.dtos.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Body sent to the Python API
public record PredictRequestDto(
        String category,
        @JsonProperty("week_of_year") int weekOfYear,
        int month,
        @JsonProperty("current_customers") int currentCustomers,
        @JsonProperty("last4_bookings") List<Integer> last4Bookings,
        @JsonProperty("last4_customers") List<Integer> last4Customers,
        @JsonProperty("is_festival_week") int isFestivalWeek,
        @JsonProperty("is_promo_week") int isPromoWeek,
        @JsonProperty("is_school_holiday_week") int isSchoolHolidayWeek
) {}