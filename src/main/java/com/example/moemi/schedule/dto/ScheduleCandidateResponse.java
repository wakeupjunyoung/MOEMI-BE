package com.example.moemi.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.example.moemi.schedule.parse.ParsedSchedule;
import com.example.moemi.schedule.travel.TravelEstimate;

/**
 * A suggestion read from a sentence. It is <b>not saved</b>: the app shows it for review, the user
 * fills what is missing and edits the suggested times, then saves it with POST /api/schedules.
 *
 * @param startTimeOptions readings to choose from when the time was ambiguous ("3시")
 * @param missingFields    required fields still empty; the app asks the user to fill them
 * @param suggestedPrepMinutes preparation time to propose; the user may change it
 * @param travel           suggested travel time; check its status before using {@code minutes}
 * @param conflicts        existing schedules it would conflict with, when date and time are known
 */
public record ScheduleCandidateResponse(
        String text,
        String title,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String place,
        List<LocalTime> startTimeOptions,
        List<String> missingFields,
        List<String> notes,
        int suggestedPrepMinutes,
        TravelEstimate travel,
        List<ConflictResponse> conflicts) {

    public static ScheduleCandidateResponse from(String text, ParsedSchedule parsed, int suggestedPrepMinutes,
            TravelEstimate travel, List<ConflictResponse> conflicts) {
        return new ScheduleCandidateResponse(text, parsed.title(), parsed.date(), parsed.startTime(), parsed.endTime(),
                parsed.place(), parsed.startTimeOptions(), parsed.missingFields(), parsed.notes(), suggestedPrepMinutes,
                travel, conflicts);
    }
}
