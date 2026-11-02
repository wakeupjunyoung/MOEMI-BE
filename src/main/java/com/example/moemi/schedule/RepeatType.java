package com.example.moemi.schedule;

import java.time.LocalDate;

/**
 * How a schedule repeats after its first date. Edits apply to the whole series.
 */
public enum RepeatType {
    NONE,
    DAILY,
    /** Same weekday as the first date. */
    WEEKLY,
    /** Same day of month as the first date; months without that day are skipped. */
    MONTHLY;

    /** Whether a series starting on {@code first} has an occurrence on {@code date} (not before {@code first}). */
    boolean matches(LocalDate first, LocalDate date) {
        if (date.isBefore(first)) {
            return false;
        }
        return switch (this) {
            case NONE -> date.equals(first);
            case DAILY -> true;
            case WEEKLY -> date.getDayOfWeek() == first.getDayOfWeek();
            case MONTHLY -> date.getDayOfMonth() == first.getDayOfMonth();
        };
    }
}
