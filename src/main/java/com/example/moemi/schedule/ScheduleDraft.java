package com.example.moemi.schedule;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.moemi.schedule.travel.TravelMode;

/**
 * The user-confirmed contents of a schedule, as entered or as accepted from a suggestion.
 *
 * @param endTime       {@code null} when the end is open
 * @param repeatUntil   last date of a repeating series, inclusive; {@code null} for no end
 * @param prepMinutes   preparation time before leaving; {@code null} for none
 * @param travelMinutes travel time to the place as the user planned it; {@code null} when unknown or not needed
 * @param travelMode    how the user travels there; {@code null} when not set
 */
public record ScheduleDraft(
        String title,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String place,
        Double placeLat,
        Double placeLng,
        String memo,
        RepeatType repeatType,
        LocalDate repeatUntil,
        Integer prepMinutes,
        Integer travelMinutes,
        TravelMode travelMode) {
}
