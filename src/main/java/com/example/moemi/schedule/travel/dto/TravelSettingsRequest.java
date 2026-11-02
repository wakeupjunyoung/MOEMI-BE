package com.example.moemi.schedule.travel.dto;

import com.example.moemi.schedule.travel.TravelMode;

import jakarta.validation.constraints.NotNull;

/**
 * @param defaultMode    mode used when a request names none
 * @param bufferMinutes  0–60 minutes added to every estimate; {@code null} means 0
 * @param roundUpMinutes round estimates up to 0 (none), 5, 10, 15 or 30 minutes; {@code null} means 0
 */
public record TravelSettingsRequest(
        @NotNull(message = "defaultMode를 입력해 주세요.") TravelMode defaultMode,
        Integer bufferMinutes,
        Integer roundUpMinutes) {
}
