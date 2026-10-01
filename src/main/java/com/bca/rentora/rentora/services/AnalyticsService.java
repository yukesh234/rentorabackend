package com.bca.rentora.rentora.services;

import com.bca.rentora.rentora.dtos.analytics.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalyticsService {
    AnalyticsSummaryDto getSummary(UUID ownerId, UUID listingId, Instant startDate, Instant endDate);
    List<TimeSeriesPointDto> getBookingsOverTime(UUID ownerId, UUID listingId, Instant startDate, Instant endDate);
    List<CategoryBreakdownDto> getCategoryBreakdown(UUID ownerId, UUID listingId, Instant startDate, Instant endDate);
    List<TopListingDto> getTopListings(UUID ownerId, int limit);
    List<ListingOptionDto> getOwnerListingOptions(UUID ownerId);
}