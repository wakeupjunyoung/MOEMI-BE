package com.example.moemi.schedule.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.example.moemi.reminder.ReminderAnchor;
import com.example.moemi.schedule.RepeatType;
import com.example.moemi.schedule.Schedule;
import com.example.moemi.schedule.ScheduleConflict;
import com.example.moemi.schedule.travel.TravelMode;

/**
 * @param reminderEnabled        {@code null} when following the device's default
 * @param reminderOffsetsMinutes empty when following the device's default
 * @param conflicts conflicts the user confirmed when saving; shown as a warning, empty otherwise
 */
public record ScheduleResponse(
        Long id,
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
        TravelMode travelMode,
        Boolean reminderEnabled,
        ReminderAnchor reminderAnchor,
        List<Integer> reminderOffsetsMinutes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ConflictResponse> conflicts) {

    public static ScheduleResponse from(Schedule schedule) {
        return from(schedule, List.of());
    }

    public static ScheduleResponse from(Schedule schedule, List<ScheduleConflict> conflicts) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getPlace(),
                schedule.getPlaceLat(),
                schedule.getPlaceLng(),
                schedule.getMemo(),
                schedule.getRepeatType(),
                schedule.getRepeatUntil(),
                schedule.getPrepMinutes(),
                schedule.getTravelMinutes(),
                schedule.getTravelMode(),
                schedule.getReminderEnabled(),
                schedule.getReminderAnchor(),
                List.copyOf(schedule.getReminderOffsets()),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt(),
                conflicts.stream().map(ConflictResponse::from).toList());
    }
}
