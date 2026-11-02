package com.example.moemi.reminder.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.moemi.reminder.ReminderAnchor;
import com.example.moemi.schedule.dto.ChecklistSummary;

/**
 * Reminders that have not fired yet, soonest first. The app schedules a local notification for each
 * and cancels any it scheduled before that is no longer listed; it also shows this list in the app,
 * which works even when notification permission is off.
 *
 * @param until reminders firing up to this time are included
 */
public record UpcomingRemindersResponse(LocalDateTime generatedAt, LocalDateTime until, List<Reminder> reminders) {

    public enum Target {
        /** Open the schedule detail. */
        SCHEDULE,
        /** Open the schedule's checklist, which still has unchecked items. */
        CHECKLIST
    }

    /**
     * @param id                stable for the same schedule, date and offset; use it as the notification id
     * @param occurrenceDate    the date of this occurrence (repeating schedules have one per date)
     * @param anchor            {@code minutesBefore} counts back from departure or from the start
     * @param prepStartAt       when to start getting ready; {@code null} without preparation/travel time
     * @param departAt          when to leave; {@code null} without travel time
     * @param checklist         the linked feature 2 checklist, or {@code null}
     * @param notificationTitle e.g. "14:00 강남 미팅 · 출발 1시간 전"
     * @param notificationBody  e.g. "장소: 강남 · 13:20 출발 · 준비물 2개 남음"
     */
    public record Reminder(
            String id,
            Long scheduleId,
            LocalDate occurrenceDate,
            LocalDateTime fireAt,
            ReminderAnchor anchor,
            int minutesBefore,
            String title,
            LocalDateTime startAt,
            String place,
            LocalDateTime prepStartAt,
            LocalDateTime departAt,
            ChecklistSummary checklist,
            Target target,
            String notificationTitle,
            String notificationBody) {
    }
}
