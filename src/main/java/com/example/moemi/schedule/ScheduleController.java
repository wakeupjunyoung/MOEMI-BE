package com.example.moemi.schedule;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.moemi.common.web.DeviceId;
import com.example.moemi.schedule.dto.CalendarDayResponse;
import com.example.moemi.schedule.dto.ConflictResponse;
import com.example.moemi.schedule.dto.ParseRequest;
import com.example.moemi.schedule.dto.ScheduleCandidateResponse;
import com.example.moemi.schedule.dto.ScheduleRequest;
import com.example.moemi.schedule.dto.ScheduleResponse;
import com.example.moemi.schedule.dto.TimelineResponse;
import com.example.moemi.schedule.dto.TravelTimeRequest;
import com.example.moemi.schedule.travel.TravelEstimate;
import com.example.moemi.schedule.travel.TravelTimeService;
import com.example.moemi.schedule.travel.dto.TravelSettingsRequest;
import com.example.moemi.schedule.travel.dto.TravelSettingsResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Every endpoint requires the {@value DeviceId#HEADER} header and only sees that device's schedules.
 * After any change, the app should refresh its reminders from GET /api/reminders/upcoming.
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final TravelTimeService travelTimeService;

    /** Schedules per date for the month view, e.g. {@code GET /api/schedules?from=2026-11-01&to=2026-11-30}. */
    @GetMapping
    public List<CalendarDayResponse> calendar(DeviceId deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return scheduleService.calendar(deviceId, from, to);
    }

    /** One day in time order with preparation and travel blocks and conflict warnings. */
    @GetMapping("/timeline")
    public TimelineResponse timeline(DeviceId deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.timeline(deviceId, date);
    }

    @GetMapping("/{scheduleId}")
    public ScheduleResponse get(DeviceId deviceId, @PathVariable Long scheduleId) {
        return scheduleService.get(deviceId, scheduleId);
    }

    /** 409 SCHEDULE_CONFLICT when it conflicts and {@code overlapConfirmed} is not true. */
    @PostMapping
    public ScheduleResponse create(DeviceId deviceId, @Valid @RequestBody ScheduleRequest request) {
        return scheduleService.create(deviceId, request);
    }

    @PutMapping("/{scheduleId}")
    public ScheduleResponse update(DeviceId deviceId, @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleRequest request) {
        return scheduleService.update(deviceId, scheduleId, request);
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> delete(DeviceId deviceId, @PathVariable Long scheduleId) {
        scheduleService.delete(deviceId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Conflicts the schedule would cause, without saving.
     *
     * @param scheduleId the schedule being edited, so it is not compared with itself; omit when creating
     */
    @PostMapping("/check")
    public List<ConflictResponse> check(DeviceId deviceId, @RequestParam(required = false) Long scheduleId,
            @Valid @RequestBody ScheduleRequest request) {
        return scheduleService.check(deviceId, scheduleId, request);
    }

    /** A candidate read from a sentence, for the user to review. Never saves. */
    @PostMapping("/parse")
    public ScheduleCandidateResponse parse(DeviceId deviceId, @Valid @RequestBody ParseRequest request) {
        return scheduleService.parse(deviceId, request);
    }

    /**
     * A suggested travel time between two points, by the requested or default mode. A place within
     * 10 minutes' walk is always walked. Never saves.
     */
    @PostMapping("/travel-time")
    public TravelEstimate travelTime(DeviceId deviceId, @Valid @RequestBody TravelTimeRequest request) {
        return scheduleService.travelTime(deviceId, request);
    }

    /** Default travel mode, buffer and rounding used for travel time suggestions. */
    @GetMapping("/travel-settings")
    public TravelSettingsResponse travelSettings(DeviceId deviceId) {
        return travelTimeService.settings(deviceId);
    }

    @PutMapping("/travel-settings")
    public TravelSettingsResponse updateTravelSettings(DeviceId deviceId,
            @Valid @RequestBody TravelSettingsRequest request) {
        return travelTimeService.updateSettings(deviceId, request);
    }
}
