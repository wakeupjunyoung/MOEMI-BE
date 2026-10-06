package com.example.moemi.hazard;

import java.time.LocalDateTime;

public enum HazardState {
    ACTIVE,
    /** Starts in the future. */
    SCHEDULED,
    /** Already over; shown apart from current information. */
    ENDED;

    public static HazardState of(LocalDateTime startAt, LocalDateTime endAt, LocalDateTime now) {
        if (endAt != null && endAt.isBefore(now)) {
            return ENDED;
        }
        if (startAt != null && startAt.isAfter(now)) {
            return SCHEDULED;
        }
        return ACTIVE;
    }
}
