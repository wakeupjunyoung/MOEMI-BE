package com.example.moemi.reminder.dto;

import java.util.List;

import com.example.moemi.reminder.ReminderAnchor;

/**
 * The schedule's own values ({@code null}/empty when following the default) and what actually applies.
 */
public record ScheduleReminderResponse(
        Long scheduleId,
        Boolean enabled,
        ReminderAnchor anchor,
        List<Integer> offsetsMinutes,
        boolean effectiveEnabled,
        ReminderAnchor effectiveAnchor,
        List<Integer> effectiveOffsetsMinutes) {
}
