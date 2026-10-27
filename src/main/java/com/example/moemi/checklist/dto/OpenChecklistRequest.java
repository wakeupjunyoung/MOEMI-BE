package com.example.moemi.checklist.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/**
 * @param scheduleId the schedule to prepare for, or {@code null} for a whole-day checklist
 * @param regionCode region whose life indices drive the recommendations (see GET /api/life-index/regions)
 */
public record OpenChecklistRequest(
        @NotNull(message = "date는 필수입니다.") LocalDate date,
        Long scheduleId,
        String regionCode) {
}
