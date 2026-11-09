package com.example.moemi.schedule;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moemi.common.error.ApiException;
import com.example.moemi.common.web.DeviceId;
import com.example.moemi.hazard.GeoPoint;
import com.example.moemi.reminder.ReminderOffsets;
import com.example.moemi.schedule.dto.CalendarDayResponse;
import com.example.moemi.schedule.dto.ConflictResponse;
import com.example.moemi.schedule.dto.ParseRequest;
import com.example.moemi.schedule.dto.ScheduleCandidateResponse;
import com.example.moemi.schedule.dto.ScheduleRequest;
import com.example.moemi.schedule.dto.ScheduleResponse;
import com.example.moemi.schedule.dto.TimelineResponse;
import com.example.moemi.schedule.dto.TravelTimeRequest;
import com.example.moemi.schedule.parse.ParsedSchedule;
import com.example.moemi.schedule.parse.ScheduleTextParser;
import com.example.moemi.schedule.travel.TravelEstimate;
import com.example.moemi.schedule.travel.TravelTimeService;

import lombok.RequiredArgsConstructor;

/**
 * Feature 5: schedules per device, the daily timeline, conflict warnings, and suggestions from a
 * sentence. Suggestions are never saved by themselves; only POST/PUT with the user's values save.
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    static final int MAX_RANGE_DAYS = 62;
    /** A repeating schedule is checked for conflicts over its next occurrences within this many days. */
    static final int CONFLICT_CHECK_DAYS = 31;
    static final int SUGGESTED_PREP_MINUTES = 30;
    private static final String UNTITLED = "새 일정";

    private final ScheduleRepository scheduleRepository;
    private final ChecklistLinks checklistLinks;
    private final TravelTimeService travelTimeService;
    private final Clock clock;

    /**
     * Saves a schedule. When it conflicts with others, the user must have confirmed the warning
     * ({@code overlapConfirmed}); the schedule is then saved and the conflicts returned as a warning.
     */
    @Transactional
    public ScheduleResponse create(DeviceId deviceId, ScheduleRequest request) {
        ScheduleDraft draft = validated(request.toDraft());
        List<Integer> offsets = ReminderOffsets.normalize(request.reminderOffsetsMinutes());
        Schedule schedule = Schedule.create(deviceId.value(), draft, now());
        schedule.changeReminder(request.reminderEnabled(), request.reminderAnchor(), offsets, now());
        List<ScheduleConflict> conflicts = conflictsOf(deviceId.value(), schedule);
        requireConfirmed(conflicts, request.isOverlapConfirmed());
        scheduleRepository.save(schedule);
        return ScheduleResponse.from(schedule, conflicts);
    }

    /** Replaces the whole schedule (every occurrence of a repeating one). Same conflict rule as create. */
    @Transactional
    public ScheduleResponse update(DeviceId deviceId, Long scheduleId, ScheduleRequest request) {
        ScheduleDraft draft = validated(request.toDraft());
        List<Integer> offsets = ReminderOffsets.normalize(request.reminderOffsetsMinutes());
        Schedule schedule = getOwned(deviceId, scheduleId);
        schedule.update(draft, now());
        schedule.changeReminder(request.reminderEnabled(), request.reminderAnchor(), offsets, now());
        List<ScheduleConflict> conflicts = conflictsOf(deviceId.value(), schedule);
        // Throwing rolls the change back.
        requireConfirmed(conflicts, request.isOverlapConfirmed());
        return ScheduleResponse.from(schedule, conflicts);
    }

    /** Conflicts the request would cause, without saving; for warning before the user saves. */
    @Transactional(readOnly = true)
    public List<ConflictResponse> check(DeviceId deviceId, Long scheduleId, ScheduleRequest request) {
        ScheduleDraft draft = validated(request.toDraft());
        Schedule candidate = Schedule.create(deviceId.value(), draft, now());
        return conflictsOf(deviceId.value(), candidate, scheduleId).stream().map(ConflictResponse::from).toList();
    }

    @Transactional
    public void delete(DeviceId deviceId, Long scheduleId) {
        scheduleRepository.delete(getOwned(deviceId, scheduleId));
    }

    @Transactional(readOnly = true)
    public ScheduleResponse get(DeviceId deviceId, Long scheduleId) {
        return ScheduleResponse.from(getOwned(deviceId, scheduleId));
    }

    /** Schedules of each date in the range, for the month calendar. Dates without schedules are left out. */
    @Transactional(readOnly = true)
    public List<CalendarDayResponse> calendar(DeviceId deviceId, LocalDate from, LocalDate to) {
        requireRange(from, to);
        Map<LocalDate, List<Occurrence>> byDate = occurrences(deviceId.value(), from, to).stream()
                .collect(Collectors.groupingBy(Occurrence::date, TreeMap::new, Collectors.toList()));
        return byDate.entrySet().stream().map(e -> CalendarDayResponse.from(e.getKey(), e.getValue())).toList();
    }

    @Transactional(readOnly = true)
    public TimelineResponse timeline(DeviceId deviceId, LocalDate date) {
        List<Occurrence> occurrences = occurrences(deviceId.value(), date, date);
        return TimelineResponse.from(date, occurrences, checklistLinks.find(deviceId.value(), List.of(date)));
    }

    /**
     * A schedule candidate read from a sentence, with suggested preparation time. Nothing is saved;
     * parse failures only leave fields empty. A sentence carries a place name but no coordinates, so
     * the travel time comes back as NO_DESTINATION until the app resolves the place and calls /travel-time.
     */
    public ScheduleCandidateResponse parse(DeviceId deviceId, ParseRequest request) {
        ParsedSchedule parsed = ScheduleTextParser.parse(request.text(), LocalDate.now(clock));
        TravelEstimate travel = travelTimeService.estimate(deviceId.value(),
                point(request.originLat(), request.originLng()), null, null);

        List<ConflictResponse> conflicts = List.of();
        if (parsed.date() != null && parsed.startTime() != null) {
            ScheduleDraft draft = new ScheduleDraft(parsed.title() != null ? parsed.title() : UNTITLED, parsed.date(),
                    parsed.startTime(), parsed.endTime(), parsed.place(), null, null, null, RepeatType.NONE, null,
                    SUGGESTED_PREP_MINUTES, null, null);
            Schedule candidate = Schedule.create(deviceId.value(), draft, now());
            conflicts = conflictsOf(deviceId.value(), candidate).stream().map(ConflictResponse::from).toList();
        }
        return ScheduleCandidateResponse.from(request.text(), parsed, SUGGESTED_PREP_MINUTES, travel, conflicts);
    }

    /**
     * A suggested travel time for the edit screen. The user may keep {@code suggestedMinutes} or enter
     * their own, and saves it with the schedule as {@code travelMinutes}.
     */
    public TravelEstimate travelTime(DeviceId deviceId, TravelTimeRequest request) {
        return travelTimeService.estimate(deviceId.value(), point(request.originLat(), request.originLng()),
                point(request.destLat(), request.destLng()), request.mode());
    }

    /** Every occurrence of the device's schedules from {@code from} to {@code to}, in time order. */
    @Transactional(readOnly = true)
    public List<Occurrence> occurrences(String deviceId, LocalDate from, LocalDate to) {
        List<Schedule> schedules = scheduleRepository.findByDeviceIdAndDateLessThanEqual(deviceId, to).stream()
                .filter(schedule -> schedule.occursOnOrAfter(from))
                .toList();
        List<Occurrence> occurrences = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            for (Schedule schedule : schedules) {
                if (schedule.occursOn(day)) {
                    occurrences.add(new Occurrence(schedule, day));
                }
            }
        }
        occurrences.sort(Comparator.comparing(Occurrence::startAt));
        return occurrences;
    }

    private List<ScheduleConflict> conflictsOf(String deviceId, Schedule candidate) {
        return conflictsOf(deviceId, candidate, candidate.getId());
    }

    /**
     * Conflicts between the candidate and the device's other schedules: on its date, or for a repeating
     * schedule on its occurrences in the next {@value #CONFLICT_CHECK_DAYS} days.
     *
     * @param excludeId the schedule being edited, which must not conflict with itself
     */
    private List<ScheduleConflict> conflictsOf(String deviceId, Schedule candidate, Long excludeId) {
        LocalDate from = candidate.getRepeatType() == RepeatType.NONE
                ? candidate.getDate()
                : later(candidate.getDate(), LocalDate.now(clock));
        LocalDate to = candidate.getRepeatType() == RepeatType.NONE ? from : from.plusDays(CONFLICT_CHECK_DAYS - 1);

        List<Occurrence> others = occurrences(deviceId, from, to).stream()
                .filter(o -> o.schedule() != candidate)
                .filter(o -> excludeId == null || !excludeId.equals(o.schedule().getId()))
                .toList();
        List<ScheduleConflict> conflicts = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (!candidate.occursOn(day)) {
                continue;
            }
            Occurrence mine = new Occurrence(candidate, day);
            LocalDate date = day;
            List<Occurrence> sameDay = new ArrayList<>(others.stream().filter(o -> o.date().equals(date)).toList());
            sameDay.add(mine);
            ScheduleConflict.find(sameDay).stream().filter(c -> c.involves(mine)).forEach(conflicts::add);
        }
        return conflicts;
    }

    private static void requireConfirmed(List<ScheduleConflict> conflicts, boolean confirmed) {
        if (conflicts.isEmpty() || confirmed) {
            return;
        }
        throw new ApiException(HttpStatus.CONFLICT, "SCHEDULE_CONFLICT",
                ConflictResponse.from(conflicts.get(0)).message()
                        + (conflicts.size() > 1 ? " 외 " + (conflicts.size() - 1) + "건" : "")
                        + " 확인 후 overlapConfirmed=true로 다시 저장해 주세요.");
    }

    private static ScheduleDraft validated(ScheduleDraft draft) {
        if (draft.endTime() != null && !draft.endTime().isAfter(draft.startTime())) {
            throw ApiException.badRequest("INVALID_TIME", "종료 시각은 시작 시각보다 늦어야 합니다.");
        }
        if (draft.repeatType() != RepeatType.NONE && draft.repeatUntil() != null
                && draft.repeatUntil().isBefore(draft.date())) {
            throw ApiException.badRequest("INVALID_REPEAT", "반복 종료일은 일정 날짜 이후여야 합니다.");
        }
        if ((draft.placeLat() == null) != (draft.placeLng() == null)) {
            throw ApiException.badRequest("INVALID_REQUEST", "placeLat와 placeLng는 함께 입력해야 합니다.");
        }
        return draft;
    }

    private static void requireRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw ApiException.badRequest("INVALID_RANGE", "to는 from보다 빠를 수 없습니다.");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
            throw ApiException.badRequest("INVALID_RANGE", "한 번에 최대 " + MAX_RANGE_DAYS + "일까지 조회할 수 있습니다.");
        }
    }

    /** Another device's schedule is reported as missing, not forbidden, so ids cannot be probed. */
    private Schedule getOwned(DeviceId deviceId, Long scheduleId) {
        return scheduleRepository.findByIdAndDeviceId(scheduleId, deviceId.value())
                .orElseThrow(() -> ApiException.notFound("SCHEDULE_NOT_FOUND", "일정을 찾을 수 없습니다."));
    }

    private static GeoPoint point(Double lat, Double lng) {
        return lat == null || lng == null ? null : new GeoPoint(lat, lng);
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
