package com.example.moemi.lifeindex;

/**
 * Indices for a single date, used by features that build on the calendar (e.g. the checklist).
 *
 * @param day the date's indices, or {@code null} when the lookup failed or the date has no data
 */
public record DayLookup(LifeIndexStatus status, DailyLifeIndex day) {

    public boolean failed() {
        return status == LifeIndexStatus.FAILED;
    }
}
