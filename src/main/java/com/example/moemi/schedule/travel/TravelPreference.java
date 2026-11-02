package com.example.moemi.schedule.travel;

import com.example.moemi.common.web.DeviceId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * How a device wants travel time suggested. A device without a row uses the defaults:
 * bus, no buffer, no rounding.
 */
@Entity
@Table(name = "travel_preference", uniqueConstraints = @UniqueConstraint(name = "uk_travel_preference_device", columnNames = "device_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelPreference {

    public static final TravelMode DEFAULT_MODE = TravelMode.BUS;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = DeviceId.MAX_LENGTH)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TravelMode defaultMode;

    /** Minutes added to every estimate, e.g. 5 for waiting at the stop. */
    @Column(nullable = false)
    private int bufferMinutes;

    /** Estimates are rounded up to a multiple of this (0 = no rounding), e.g. 10 makes 25 → 30. */
    @Column(nullable = false)
    private int roundUpMinutes;

    public static TravelPreference create(String deviceId) {
        TravelPreference preference = new TravelPreference();
        preference.deviceId = deviceId;
        preference.defaultMode = DEFAULT_MODE;
        return preference;
    }

    public static TravelPreference defaults() {
        return create(null);
    }

    public void change(TravelMode defaultMode, int bufferMinutes, int roundUpMinutes) {
        this.defaultMode = defaultMode;
        this.bufferMinutes = bufferMinutes;
        this.roundUpMinutes = roundUpMinutes;
    }

    /** The estimate as the user wants it planned: buffer added, then rounded up. */
    public int adjust(int minutes) {
        int withBuffer = minutes + bufferMinutes;
        if (roundUpMinutes <= 0 || withBuffer == 0) {
            return withBuffer;
        }
        return (withBuffer + roundUpMinutes - 1) / roundUpMinutes * roundUpMinutes;
    }
}
