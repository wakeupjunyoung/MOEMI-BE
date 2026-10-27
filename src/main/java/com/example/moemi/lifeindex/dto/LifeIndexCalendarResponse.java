package com.example.moemi.lifeindex.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.moemi.lifeindex.LifeIndexCalendar;
import com.example.moemi.lifeindex.LifeIndexService;
import com.example.moemi.lifeindex.LifeIndexStatus;

/**
 * @param fetchedAt last time the indices were fetched from KMA (마지막 갱신 시각); {@code null} when FAILED
 */
public record LifeIndexCalendarResponse(
        String regionCode,
        String regionName,
        LifeIndexStatus status,
        String source,
        LocalDateTime fetchedAt,
        List<DayResponse> days) {

    public static LifeIndexCalendarResponse from(LifeIndexCalendar calendar) {
        return new LifeIndexCalendarResponse(
                calendar.region().getCode(),
                calendar.region().getDisplayName(),
                calendar.status(),
                LifeIndexService.SOURCE,
                calendar.fetchedAt(),
                calendar.days().stream().map(DayResponse::from).toList());
    }
}
