package com.example.moemi.reminder;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.moemi.common.web.DeviceId;
import com.example.moemi.reminder.dto.ReminderSettingsRequest;
import com.example.moemi.reminder.dto.ReminderSettingsResponse;
import com.example.moemi.reminder.dto.ScheduleReminderRequest;
import com.example.moemi.reminder.dto.ScheduleReminderResponse;
import com.example.moemi.reminder.dto.UpcomingRemindersResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Feature 6. The server works out which reminders are due; the app delivers them as local
 * notifications. The app should call /upcoming at launch and after every schedule change, then
 * schedule what is listed and cancel what is not, so deleted or edited schedules leave no stale reminder.
 */
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    /** Default reminder setting (설정 메뉴). */
    @GetMapping("/settings")
    public ReminderSettingsResponse settings(DeviceId deviceId) {
        return reminderService.settings(deviceId);
    }

    @PutMapping("/settings")
    public ReminderSettingsResponse updateSettings(DeviceId deviceId, @Valid @RequestBody ReminderSettingsRequest request) {
        return reminderService.updateSettings(deviceId, request);
    }

    /** Reminder on/off and timing for one schedule (also settable when saving the schedule). */
    @PutMapping("/schedules/{scheduleId}")
    public ScheduleReminderResponse updateScheduleReminder(DeviceId deviceId, @PathVariable Long scheduleId,
            @RequestBody ScheduleReminderRequest request) {
        return reminderService.updateScheduleReminder(deviceId, scheduleId, request);
    }

    /** Reminders still to fire, e.g. {@code GET /api/reminders/upcoming?days=7}. */
    @GetMapping("/upcoming")
    public UpcomingRemindersResponse upcoming(DeviceId deviceId, @RequestParam(defaultValue = "7") int days) {
        return reminderService.upcoming(deviceId, days);
    }
}
