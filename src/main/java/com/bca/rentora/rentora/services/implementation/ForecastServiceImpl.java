package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.forecast.ForecastDto;
import com.bca.rentora.rentora.dtos.forecast.PredictRequestDto;
import com.bca.rentora.rentora.dtos.forecast.PredictResponseDto;
import com.bca.rentora.rentora.entity.Booking;
import com.bca.rentora.rentora.entity.BookingStatus;
import com.bca.rentora.rentora.entity.CategoryType;
import com.bca.rentora.rentora.entity.Listing;
import com.bca.rentora.rentora.repo.AnalyticsRepo;
import com.bca.rentora.rentora.repo.ListingRepo;
import com.bca.rentora.rentora.services.ForecastService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ForecastServiceImpl implements ForecastService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kathmandu");

    private final AnalyticsRepo analyticsRepo;
    private final ListingRepo listingRepo;
    private final RestTemplate restTemplate;

    @Value("${forecast.api-url}")
    private String forecastUrl;

    public ForecastServiceImpl(AnalyticsRepo analyticsRepo, ListingRepo listingRepo) {
        this.analyticsRepo = analyticsRepo;
        this.listingRepo = listingRepo;

        // short timeouts so a dead Python service doesn't hang the dashboard
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public List<ForecastDto> getForecast(UUID ownerId, UUID listingId,
                                         boolean festival, boolean promo, boolean schoolHoliday,
                                         String groupBy) {
        List<Listing> listings = listingRepo.findByOwner_useridAndDeletedAtIsNull(ownerId);
        if (listingId != null) {
            listings = listings.stream().filter(l -> l.getId().equals(listingId)).toList();
        }

        LocalDate today = LocalDate.now(ZONE);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Instant from = weekStart.minusWeeks(4).atStartOfDay(ZONE).toInstant();

        List<Booking> bookings = analyticsRepo.findForOwner(ownerId, listingId, from, Instant.now())
                .stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .toList();

        int weekOfYear = weekStart.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int month = today.getMonthValue();

        boolean byProduct = "listing".equalsIgnoreCase(groupBy) || "product".equalsIgnoreCase(groupBy);
        List<ForecastDto> result = new ArrayList<>();

        if (byProduct) {
            // one forecast per listing, using only that listing's bookings
            List<Listing> sorted = listings.stream()
                    .sorted(Comparator.comparing((Listing l) -> l.getTitle() == null ? "" : l.getTitle()))
                    .toList();
            for (Listing listing : sorted) {
                List<Booking> mine = bookings.stream()
                        .filter(b -> b.getListing().getId().equals(listing.getId()))
                        .toList();
                result.add(forecastUnit(listing.getCategory(), listing.getId(), listing.getTitle(),
                        mine, weekStart, weekOfYear, month, festival, promo, schoolHoliday));
            }
        } else {
            // one forecast per category the owner actually has listings in
            Set<CategoryType> categories = listings.stream()
                    .map(Listing::getCategory)
                    .collect(Collectors.toCollection(TreeSet::new));

            for (CategoryType category : categories) {
                List<Booking> catBookings = bookings.stream()
                        .filter(b -> b.getListing().getCategory() == category)
                        .toList();
                result.add(forecastUnit(category, null, null,
                        catBookings, weekStart, weekOfYear, month, festival, promo, schoolHoliday));
            }
        }
        return result;
    }

    // builds the features for one category (or one listing) and asks the Python service
    private ForecastDto forecastUnit(CategoryType category, UUID listingId, String listingTitle,
                                     List<Booking> unitBookings, LocalDate weekStart,
                                     int weekOfYear, int month,
                                     boolean festival, boolean promo, boolean schoolHoliday) {
        List<Integer> lastBookings = new ArrayList<>();
        List<Integer> lastCustomers = new ArrayList<>();
        for (int i = 4; i >= 1; i--) {
            List<Booking> week = inWeek(unitBookings, weekStart.minusWeeks(i));
            lastBookings.add(week.size());
            lastCustomers.add(distinctRenters(week));
        }

        List<Booking> current = inWeek(unitBookings, weekStart);
        int currentCustomers = distinctRenters(current);

        PredictRequestDto req = new PredictRequestDto(
                category.name(), weekOfYear, month, currentCustomers,
                lastBookings, lastCustomers,
                festival ? 1 : 0, promo ? 1 : 0, schoolHoliday ? 1 : 0
        );

        try {
            PredictResponseDto res = restTemplate.postForObject(forecastUrl, req, PredictResponseDto.class);
            if (res == null || res.predictedBookings() == null) {
                return unavailable(category, listingId, listingTitle, weekOfYear,
                        lastBookings, current.size(), currentCustomers);
            }
            return new ForecastDto(category.name(), weekOfYear, true,
                    res.predictedBookings(), res.predictedRaw(), res.lowConfidence(),
                    lastBookings, current.size(), currentCustomers,
                    listingId, listingTitle);
        } catch (RestClientException e) {
            // Python service down or returned an error: report it, don't crash the page
            return unavailable(category, listingId, listingTitle, weekOfYear,
                    lastBookings, current.size(), currentCustomers);
        }
    }

    private ForecastDto unavailable(CategoryType category, UUID listingId, String listingTitle,
                                    int weekOfYear, List<Integer> last4,
                                    int currentBookings, int currentCustomers) {
        return new ForecastDto(category.name(), weekOfYear, false, null, null, null,
                last4, currentBookings, currentCustomers, listingId, listingTitle);
    }

    private List<Booking> inWeek(List<Booking> list, LocalDate weekStart) {
        Instant s = weekStart.atStartOfDay(ZONE).toInstant();
        Instant e = weekStart.plusWeeks(1).atStartOfDay(ZONE).toInstant();
        return list.stream()
                .filter(b -> !b.getCreatedAt().isBefore(s) && b.getCreatedAt().isBefore(e))
                .toList();
    }

    private int distinctRenters(List<Booking> bookings) {
        return (int) bookings.stream().map(b -> b.getUser().getUserid()).distinct().count();
    }
}