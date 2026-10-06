package com.example.moemi.lifeindex;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Indices for a date range in one region.
 *
 * @param fetchedAt when the indices were fetched from KMA, or {@code null} when {@code status} is FAILED
 * @param days      one entry per date in the range; empty when {@code status} is FAILED
 */
public record LifeIndexCalendar(Region region, LifeIndexStatus status, LocalDateTime fetchedAt, List<Day> days) {

    /**
     * @param index the date's indices, or {@code null} unless {@code availability} is AVAILABLE
     */
    public record Day(LocalDate date, DayAvailability availability, DailyLifeIndex index) {
    }
}
