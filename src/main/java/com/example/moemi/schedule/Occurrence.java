package com.example.moemi.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A schedule on one date. A repeating schedule has one occurrence per matching date.
 */
public record Occurrence(Schedule schedule, LocalDate date) {

    public LocalDateTime startAt() {
        return date.atTime(schedule.getStartTime());
    }

    /** {@code null} when the schedule has no end time. */
    public LocalDateTime endAt() {
        return schedule.getEndTime() == null ? null : date.atTime(schedule.getEndTime());
    }

    /** When to leave: the start minus travel time. */
    public LocalDateTime departAt() {
        return startAt().minusMinutes(minutes(schedule.getTravelMinutes()));
    }

    /** When to start getting ready: the departure minus preparation time. */
    public LocalDateTime prepStartAt() {
        return departAt().minusMinutes(minutes(schedule.getPrepMinutes()));
    }

    /** The end, or the start for a schedule without an end; used to compare with what follows. */
    LocalDateTime effectiveEnd() {
        return endAt() != null ? endAt() : startAt();
    }

    private static int minutes(Integer value) {
        return value == null ? 0 : value;
    }
}
