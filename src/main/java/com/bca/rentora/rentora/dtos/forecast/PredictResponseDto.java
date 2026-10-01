package com.bca.rentora.rentora.dtos.forecast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Response from the Python API
@JsonIgnoreProperties(ignoreUnknown = true)
public record PredictResponseDto(
        @JsonProperty("predicted_bookings") Integer predictedBookings,
        @JsonProperty("predicted_raw") Double predictedRaw,
        @JsonProperty("low_confidence") Boolean lowConfidence
) {}