package com.example.moemi.reminder.dto;

import java.util.List;

import com.example.moemi.reminder.ReminderAnchor;

/**
 * Each field {@code null} (or an empty list) follows the device's default setting.
 *
 * @param enabled        {@code true}/{@code false} for this schedule
 * @param anchor         DEPARTURE or START
 * @param offsetsMinutes minutes before the anchor
 */
public record ScheduleReminderRequest(Boolean enabled, ReminderAnchor anchor, List<Integer> offsetsMinutes) {
}
