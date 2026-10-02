package com.bca.rentora.rentora.services;

import com.bca.rentora.rentora.dtos.forecast.ForecastDto;

import java.util.List;
import java.util.UUID;

public interface ForecastService {
    // groupBy: "category" (default) or "listing" (one forecast per product)
    List<ForecastDto> getForecast(UUID ownerId, UUID listingId,
                                  boolean festival, boolean promo, boolean schoolHoliday,
                                  String groupBy);
}