package com.example.moemi.reminder.dto;

import java.util.List;

import com.example.moemi.reminder.ReminderAnchor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * @param anchor                DEPARTURE ("출발 N분 전") or START ("시작 N분 전"); {@code null} means DEPARTURE
 * @param defaultOffsetsMinutes minutes before the anchor, e.g. [60, 10] for 1 hour and 10 minutes before
 */
public record ReminderSettingsRequest(
        @NotNull(message = "enabled를 입력해 주세요.") Boolean enabled,
        ReminderAnchor anchor,
        @NotEmpty(message = "기본 알림 시점을 하나 이상 입력해 주세요.") List<Integer> defaultOffsetsMinutes) {
}
