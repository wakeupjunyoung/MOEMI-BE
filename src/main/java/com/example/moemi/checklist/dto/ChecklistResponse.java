package com.example.moemi.checklist.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.moemi.checklist.Checklist;
import com.example.moemi.checklist.RecommendationStatus;

/**
 * @param scheduleId    the linked schedule, or {@code null} for a whole-day checklist
 * @param recommendedAt last time recommendations were refreshed (마지막 갱신 시각)
 */
public record ChecklistResponse(
        Long id,
        LocalDate date,
        Long scheduleId,
        String regionCode,
        RecommendationStatus recommendationStatus,
        LocalDateTime recommendedAt,
        int totalCount,
        int checkedCount,
        int remainingCount,
        List<ChecklistItemResponse> items) {

    public static ChecklistResponse from(Checklist checklist) {
        int total = checklist.getItems().size();
        int checked = checklist.checkedCount();
        return new ChecklistResponse(
                checklist.getId(),
                checklist.getDate(),
                checklist.getScheduleId(),
                checklist.getRegionCode(),
                checklist.getRecommendationStatus(),
                checklist.getRecommendedAt(),
                total,
                checked,
                total - checked,
                checklist.getItems().stream().map(ChecklistItemResponse::from).toList());
    }
}
