package com.example.moemi.schedule.dto;

import com.example.moemi.schedule.travel.TravelMode;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Coordinates come from the app: the destination from a map pick or Android's Geocoder, the origin
 * from the phone's location.
 *
 * @param mode {@code null} for the user's default mode (see /travel-settings)
 */
public record TravelTimeRequest(
        @NotNull(message = "destLat를 입력해 주세요.")
        @DecimalMin(value = "-90", message = "destLat는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90", message = "destLat는 90 이하여야 합니다.") Double destLat,
        @NotNull(message = "destLng를 입력해 주세요.")
        @DecimalMin(value = "-180", message = "destLng는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180", message = "destLng는 180 이하여야 합니다.") Double destLng,
        @NotNull(message = "originLat를 입력해 주세요.")
        @DecimalMin(value = "-90", message = "originLat는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90", message = "originLat는 90 이하여야 합니다.") Double originLat,
        @NotNull(message = "originLng를 입력해 주세요.")
        @DecimalMin(value = "-180", message = "originLng는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180", message = "originLng는 180 이하여야 합니다.") Double originLng,
        TravelMode mode) {
}
