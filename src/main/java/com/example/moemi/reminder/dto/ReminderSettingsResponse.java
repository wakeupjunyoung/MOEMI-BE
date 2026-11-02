package com.example.moemi.reminder.dto;

import java.util.List;

import com.example.moemi.reminder.ReminderAnchor;

/**
 * @param enabled               reminders for schedules that have no setting of their own
 * @param anchor                what the offsets count back from
 * @param defaultOffsetsMinutes when to remind for those schedules, largest first
 */
public record ReminderSettingsResponse(boolean enabled, ReminderAnchor anchor, List<Integer> defaultOffsetsMinutes) {
}
