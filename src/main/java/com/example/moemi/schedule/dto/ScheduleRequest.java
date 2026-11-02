package com.example.moemi.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.example.moemi.reminder.ReminderAnchor;
import com.example.moemi.schedule.RepeatType;
import com.example.moemi.schedule.Schedule;
import com.example.moemi.schedule.ScheduleDraft;
import com.example.moemi.schedule.travel.TravelMode;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A schedule as the user confirmed it. Suggested values (from /parse or /travel-time) must be sent
 * here explicitly; nothing is saved from a suggestion alone.
 *
 * @param travelMinutes    the travel time the user plans with, e.g. 30 for a 25-minute estimate
 * @param travelMode       WALK or BUS; {@code null} when not set
 * @param repeatType       {@code null} means NONE
 * @param reminderEnabled  feature 6: reminders on/off for this schedule; {@code null} follows the default setting
 * @param reminderAnchor   feature 6: DEPARTURE ("출발 N분 전") or START; {@code null} follows the default setting
 * @param reminderOffsetsMinutes feature 6: minutes before the anchor, e.g. [60, 10]; empty follows the default
 * @param overlapConfirmed send {@code true} after the user has seen the conflict warning; otherwise a
 *                         conflicting schedule is rejected with 409 SCHEDULE_CONFLICT
 */
public record ScheduleRequest(
        @NotBlank(message = "일정 제목을 입력해 주세요.")
        @Size(max = Schedule.TITLE_MAX_LENGTH, message = "제목은 100자 이하여야 합니다.") String title,
        @NotNull(message = "날짜를 입력해 주세요.") LocalDate date,
        @NotNull(message = "시작 시각을 입력해 주세요.") LocalTime startTime,
        LocalTime endTime,
        @Size(max = Schedule.PLACE_MAX_LENGTH, message = "장소는 100자 이하여야 합니다.") String place,
        @DecimalMin(value = "-90", message = "placeLat는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90", message = "placeLat는 90 이하여야 합니다.") Double placeLat,
        @DecimalMin(value = "-180", message = "placeLng는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180", message = "placeLng는 180 이하여야 합니다.") Double placeLng,
        @Size(max = Schedule.MEMO_MAX_LENGTH, message = "메모는 500자 이하여야 합니다.") String memo,
        RepeatType repeatType,
        LocalDate repeatUntil,
        @Min(value = 0, message = "준비 시간은 0분 이상이어야 합니다.")
        @Max(value = 600, message = "준비 시간은 600분 이하여야 합니다.") Integer prepMinutes,
        @Min(value = 0, message = "이동 시간은 0분 이상이어야 합니다.")
        @Max(value = 600, message = "이동 시간은 600분 이하여야 합니다.") Integer travelMinutes,
        TravelMode travelMode,
        Boolean reminderEnabled,
        ReminderAnchor reminderAnchor,
        List<Integer> reminderOffsetsMinutes,
        Boolean overlapConfirmed) {

    public ScheduleDraft toDraft() {
        return new ScheduleDraft(title.trim(), date, startTime, endTime, blankToNull(place), placeLat, placeLng,
                blankToNull(memo), repeatType == null ? RepeatType.NONE : repeatType, repeatUntil, prepMinutes,
                travelMinutes, travelMode);
    }

    public boolean isOverlapConfirmed() {
        return Boolean.TRUE.equals(overlapConfirmed);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
