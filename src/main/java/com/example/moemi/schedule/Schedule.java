package com.example.moemi.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.example.moemi.common.web.DeviceId;
import com.example.moemi.reminder.ReminderAnchor;
import com.example.moemi.schedule.travel.TravelMode;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Feature 5: a schedule on a date and time, optionally repeating, with the preparation and travel
 * time needed before it. Only ever saved with the user's confirmation, never from a suggestion alone.
 */
@Entity
@Table(name = "user_schedule", indexes = @Index(name = "idx_user_schedule_device_date", columnList = "device_id, schedule_date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    public static final int TITLE_MAX_LENGTH = 100;
    public static final int PLACE_MAX_LENGTH = 100;
    public static final int MEMO_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = DeviceId.MAX_LENGTH)
    private String deviceId;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    /** The first (or only) date. */
    @Column(name = "schedule_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    private LocalTime endTime;

    @Column(length = PLACE_MAX_LENGTH)
    private String place;

    private Double placeLat;

    private Double placeLng;

    @Column(length = MEMO_MAX_LENGTH)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RepeatType repeatType;

    private LocalDate repeatUntil;

    private Integer prepMinutes;

    private Integer travelMinutes;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TravelMode travelMode;

    /** Feature 6: reminders for this schedule on/off; {@code null} follows the device's default. */
    private Boolean reminderEnabled;

    /** Feature 6: whether offsets count from departure or start; {@code null} follows the device's default. */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ReminderAnchor reminderAnchor;

    /** Feature 6: minutes before the start to remind, largest first; empty follows the device's default. */
    @ElementCollection
    @CollectionTable(name = "user_schedule_reminder_offset", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "minutes_before", nullable = false)
    private List<Integer> reminderOffsets = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static Schedule create(String deviceId, ScheduleDraft draft, LocalDateTime now) {
        Schedule schedule = new Schedule();
        schedule.deviceId = deviceId;
        schedule.createdAt = now;
        schedule.update(draft, now);
        return schedule;
    }

    public void update(ScheduleDraft draft, LocalDateTime now) {
        this.title = draft.title();
        this.date = draft.date();
        this.startTime = draft.startTime();
        this.endTime = draft.endTime();
        this.place = draft.place();
        this.placeLat = draft.placeLat();
        this.placeLng = draft.placeLng();
        this.memo = draft.memo();
        this.repeatType = draft.repeatType();
        this.repeatUntil = draft.repeatType() == RepeatType.NONE ? null : draft.repeatUntil();
        this.prepMinutes = draft.prepMinutes();
        this.travelMinutes = draft.travelMinutes();
        this.travelMode = draft.travelMode();
        this.updatedAt = now;
    }

    /**
     * @param enabled {@code null} to follow the device's default
     * @param anchor  {@code null} to follow the device's default
     * @param offsets minutes before the anchor, already validated; empty to follow the default
     */
    public void changeReminder(Boolean enabled, ReminderAnchor anchor, List<Integer> offsets, LocalDateTime now) {
        this.reminderEnabled = enabled;
        this.reminderAnchor = anchor;
        this.reminderOffsets.clear();
        this.reminderOffsets.addAll(offsets);
        this.updatedAt = now;
    }

    public boolean occursOn(LocalDate day) {
        if (repeatUntil != null && day.isAfter(repeatUntil)) {
            return false;
        }
        return repeatType.matches(date, day);
    }

    /** Whether any occurrence can fall on or after {@code day}. */
    public boolean occursOnOrAfter(LocalDate day) {
        if (repeatType == RepeatType.NONE) {
            return !date.isBefore(day);
        }
        return repeatUntil == null || !repeatUntil.isBefore(day);
    }
}
