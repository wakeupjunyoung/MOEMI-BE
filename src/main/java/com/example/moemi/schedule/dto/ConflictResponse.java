package com.example.moemi.schedule.dto;

import java.time.LocalDate;

import com.example.moemi.schedule.ScheduleConflict;

/**
 * @param earlierScheduleId {@code null} for a schedule that is not saved yet
 * @param laterScheduleId   {@code null} for a schedule that is not saved yet
 * @param message           the warning to show, in Korean
 */
public record ConflictResponse(
        ScheduleConflict.Type type,
        LocalDate date,
        Long earlierScheduleId,
        String earlierTitle,
        Long laterScheduleId,
        String laterTitle,
        String message) {

    public static ConflictResponse from(ScheduleConflict conflict) {
        String earlier = conflict.earlier().schedule().getTitle();
        String later = conflict.later().schedule().getTitle();
        String message = switch (conflict.type()) {
            case OVERLAP -> "'" + earlier + "'과(와) '" + later + "' 일정 시간이 겹칩니다.";
            case NOT_ENOUGH_TIME -> "'" + earlier + "'이(가) 끝나기 전에 '" + later + "' 준비·이동을 시작해야 합니다.";
        };
        return new ConflictResponse(conflict.type(), conflict.earlier().date(),
                conflict.earlier().schedule().getId(), earlier,
                conflict.later().schedule().getId(), later, message);
    }
}
