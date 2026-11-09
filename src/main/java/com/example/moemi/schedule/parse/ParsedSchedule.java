package com.example.moemi.schedule.parse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * What could be read from a sentence. Anything not clearly stated is left {@code null}; nothing is
 * guessed, so the user fills or confirms it before saving.
 *
 * @param startTimeOptions when the time is ambiguous ("3시"), the readings to choose from (03:00, 15:00)
 * @param missingFields    required fields that are still empty: {@code title}, {@code date}, {@code startTime}
 * @param notes            what the user should check, in Korean
 */
public record ParsedSchedule(
        String title,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String place,
        List<LocalTime> startTimeOptions,
        List<String> missingFields,
        List<String> notes) {
}
