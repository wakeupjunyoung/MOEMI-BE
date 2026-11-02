package com.example.moemi.reminder;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moemi.common.error.ApiException;
import com.example.moemi.common.web.DeviceId;
import com.example.moemi.reminder.dto.ReminderSettingsRequest;
import com.example.moemi.reminder.dto.ReminderSettingsResponse;
import com.example.moemi.reminder.dto.ScheduleReminderRequest;
import com.example.moemi.reminder.dto.ScheduleReminderResponse;
import com.example.moemi.reminder.dto.UpcomingRemindersResponse;
import com.example.moemi.reminder.dto.UpcomingRemindersResponse.Reminder;
import com.example.moemi.reminder.dto.UpcomingRemindersResponse.Target;
import com.example.moemi.schedule.ChecklistLinks;
import com.example.moemi.schedule.Occurrence;
import com.example.moemi.schedule.Schedule;
import com.example.moemi.schedule.ScheduleRepository;
import com.example.moemi.schedule.ScheduleService;
import com.example.moemi.schedule.dto.ChecklistSummary;

import lombok.RequiredArgsConstructor;

/**
 * Feature 6: reminders before schedules. Reminders are never stored; they are worked out from the
 * schedules each time, so editing or deleting a schedule changes or removes its reminders at once.
 */
@Service
@RequiredArgsConstructor
public class ReminderService {

    static final int MAX_DAYS = 14;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ID_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final ReminderSettingsRepository settingsRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;
    private final ChecklistLinks checklistLinks;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ReminderSettingsResponse settings(DeviceId deviceId) {
        Defaults defaults = defaults(deviceId);
        return new ReminderSettingsResponse(defaults.enabled(), defaults.anchor(), defaults.offsets());
    }

    @Transactional
    public ReminderSettingsResponse updateSettings(DeviceId deviceId, ReminderSettingsRequest request) {
        List<Integer> offsets = ReminderOffsets.normalize(request.defaultOffsetsMinutes());
        ReminderAnchor anchor = request.anchor() != null ? request.anchor() : ReminderSettings.DEFAULT_ANCHOR;
        ReminderSettings settings = settingsRepository.findByDeviceId(deviceId.value())
                .orElseGet(() -> settingsRepository.save(ReminderSettings.create(deviceId.value())));
        settings.change(request.enabled(), anchor, offsets);
        return new ReminderSettingsResponse(settings.isEnabled(), settings.getAnchor(),
                List.copyOf(settings.getDefaultOffsets()));
    }

    @Transactional
    public ScheduleReminderResponse updateScheduleReminder(DeviceId deviceId, Long scheduleId,
            ScheduleReminderRequest request) {
        List<Integer> offsets = ReminderOffsets.normalize(request.offsetsMinutes());
        Schedule schedule = scheduleRepository.findByIdAndDeviceId(scheduleId, deviceId.value())
                .orElseThrow(() -> ApiException.notFound("SCHEDULE_NOT_FOUND", "일정을 찾을 수 없습니다."));
        schedule.changeReminder(request.enabled(), request.anchor(), offsets, now());
        Defaults defaults = defaults(deviceId);
        return new ScheduleReminderResponse(schedule.getId(), schedule.getReminderEnabled(), schedule.getReminderAnchor(),
                List.copyOf(schedule.getReminderOffsets()), defaults.enabledFor(schedule), defaults.anchorFor(schedule),
                defaults.offsetsFor(schedule));
    }

    /**
     * Reminders firing from now until {@code days} days later. Reminders whose time has passed are
     * left out, so a schedule edited to start sooner never fires a late reminder.
     */
    @Transactional(readOnly = true)
    public UpcomingRemindersResponse upcoming(DeviceId deviceId, int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw ApiException.badRequest("INVALID_RANGE", "days는 1에서 " + MAX_DAYS + " 사이여야 합니다.");
        }
        LocalDateTime now = now();
        LocalDateTime until = now.plusDays(days);
        Defaults defaults = defaults(deviceId);

        // A reminder up to a week before a schedule can fire inside the window for a schedule after it.
        LocalDate lastDate = until.toLocalDate().plusDays(ReminderOffsets.MAX_MINUTES / (24 * 60));
        List<Occurrence> occurrences = scheduleService.occurrences(deviceId.value(), now.toLocalDate(), lastDate).stream()
                .filter(occurrence -> occurrence.startAt().isAfter(now))
                .filter(occurrence -> defaults.enabledFor(occurrence.schedule()))
                .toList();
        Map<ChecklistLinks.Key, ChecklistSummary> checklists = checklistLinks.find(deviceId.value(),
                occurrences.stream().map(Occurrence::date).distinct().toList());

        List<Reminder> reminders = new ArrayList<>();
        for (Occurrence occurrence : occurrences) {
            ReminderAnchor anchor = defaults.anchorFor(occurrence.schedule());
            LocalDateTime anchorAt = anchor == ReminderAnchor.DEPARTURE ? occurrence.departAt() : occurrence.startAt();
            ChecklistSummary checklist = checklists.get(ChecklistLinks.Key.of(occurrence));
            for (int minutesBefore : defaults.offsetsFor(occurrence.schedule())) {
                LocalDateTime fireAt = anchorAt.minusMinutes(minutesBefore);
                if (fireAt.isAfter(now) && !fireAt.isAfter(until)) {
                    reminders.add(reminder(occurrence, anchor, minutesBefore, fireAt, checklist));
                }
            }
        }
        reminders.sort(Comparator.comparing(Reminder::fireAt).thenComparing(Reminder::id));
        return new UpcomingRemindersResponse(now, until, reminders);
    }

    private static Reminder reminder(Occurrence occurrence, ReminderAnchor anchor, int minutesBefore,
            LocalDateTime fireAt, ChecklistSummary checklist) {
        Schedule schedule = occurrence.schedule();
        boolean hasTravel = schedule.getTravelMinutes() != null && schedule.getTravelMinutes() > 0;
        boolean hasPrep = hasTravel || schedule.getPrepMinutes() != null && schedule.getPrepMinutes() > 0;
        LocalDateTime departAt = hasTravel ? occurrence.departAt() : null;
        LocalDateTime prepStartAt = hasPrep ? occurrence.prepStartAt() : null;

        String id = "s" + schedule.getId() + "-" + occurrence.date().format(ID_DATE) + "-"
                + anchor.name().charAt(0) + minutesBefore;
        String title = occurrence.startAt().format(TIME) + " " + schedule.getTitle() + " · "
                + ReminderOffsets.phrase(anchor, minutesBefore);
        List<String> body = new ArrayList<>();
        if (schedule.getPlace() != null) {
            body.add("장소: " + schedule.getPlace());
        }
        if (departAt != null) {
            String mode = schedule.getTravelMode() == null ? "" : " (" + schedule.getTravelMode().getLabel() + ")";
            body.add(departAt.format(TIME) + " 출발" + mode);
        } else if (prepStartAt != null) {
            body.add(prepStartAt.format(TIME) + "부터 준비");
        }
        if (checklist != null) {
            body.add(checklist.remainingCount() > 0 ? "준비물 " + checklist.remainingCount() + "개 남음" : "준비물 모두 챙김");
        }
        Target target = checklist != null && checklist.remainingCount() > 0 ? Target.CHECKLIST : Target.SCHEDULE;
        return new Reminder(id, schedule.getId(), occurrence.date(), fireAt, anchor, minutesBefore, schedule.getTitle(),
                occurrence.startAt(), schedule.getPlace(), prepStartAt, departAt, checklist, target, title,
                body.isEmpty() ? occurrence.startAt().format(TIME) + "에 시작합니다." : String.join(" · ", body));
    }

    private Defaults defaults(DeviceId deviceId) {
        return settingsRepository.findByDeviceId(deviceId.value())
                .map(settings -> new Defaults(settings.isEnabled(), settings.getAnchor(), List.copyOf(settings.getDefaultOffsets())))
                .orElse(new Defaults(ReminderSettings.DEFAULT_ENABLED, ReminderSettings.DEFAULT_ANCHOR,
                        ReminderSettings.DEFAULT_OFFSETS));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    /** The device's default; a schedule's own setting, when present, takes precedence. */
    private record Defaults(boolean enabled, ReminderAnchor anchor, List<Integer> offsets) {

        Defaults {
            offsets = offsets.stream().sorted(Comparator.reverseOrder()).toList();
        }

        boolean enabledFor(Schedule schedule) {
            return schedule.getReminderEnabled() != null ? schedule.getReminderEnabled() : enabled;
        }

        ReminderAnchor anchorFor(Schedule schedule) {
            return schedule.getReminderAnchor() != null ? schedule.getReminderAnchor() : anchor;
        }

        List<Integer> offsetsFor(Schedule schedule) {
            return schedule.getReminderOffsets().isEmpty() ? offsets : List.copyOf(schedule.getReminderOffsets());
        }
    }
}
