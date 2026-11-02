package com.example.moemi.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.example.moemi.schedule.Occurrence;

/**
 * The schedules of one date, for marking the month calendar.
 */
public record CalendarDayResponse(LocalDate date, List<Item> schedules) {

    public static CalendarDayResponse from(LocalDate date, List<Occurrence> occurrences) {
        return new CalendarDayResponse(date, occurrences.stream()
                .map(o -> new Item(o.schedule().getId(), o.schedule().getTitle(), o.schedule().getStartTime(),
                        o.schedule().getEndTime(), o.schedule().getPlace()))
                .toList());
    }

    public record Item(Long scheduleId, String title, LocalTime startTime, LocalTime endTime, String place) {
    }
}
