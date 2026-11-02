package com.example.moemi.reminder;

import java.util.ArrayList;
import java.util.List;

import com.example.moemi.common.web.DeviceId;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Feature 6: a device's default reminder settings, used by schedules that have none of their own.
 * A device without a row uses the DEFAULT_ constants: on, one hour before departure.
 */
@Entity
@Table(name = "reminder_settings", uniqueConstraints = @UniqueConstraint(name = "uk_reminder_settings_device", columnNames = "device_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReminderSettings {

    public static final boolean DEFAULT_ENABLED = true;
    /** Reminders are about having time to get ready, so they count back from when to leave. */
    public static final ReminderAnchor DEFAULT_ANCHOR = ReminderAnchor.DEPARTURE;
    public static final List<Integer> DEFAULT_OFFSETS = List.of(60);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = DeviceId.MAX_LENGTH)
    private String deviceId;

    @Column(nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReminderAnchor anchor;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reminder_default_offset", joinColumns = @JoinColumn(name = "settings_id"))
    @Column(name = "minutes_before", nullable = false)
    private List<Integer> defaultOffsets = new ArrayList<>();

    public static ReminderSettings create(String deviceId) {
        ReminderSettings settings = new ReminderSettings();
        settings.deviceId = deviceId;
        settings.enabled = DEFAULT_ENABLED;
        settings.anchor = DEFAULT_ANCHOR;
        settings.defaultOffsets.addAll(DEFAULT_OFFSETS);
        return settings;
    }

    /** @param offsets validated, non-empty */
    public void change(boolean enabled, ReminderAnchor anchor, List<Integer> offsets) {
        this.enabled = enabled;
        this.anchor = anchor;
        this.defaultOffsets.clear();
        this.defaultOffsets.addAll(offsets);
    }
}
