package com.example.moemi.transit;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Buses on their way to a stop, soonest first.
 *
 * @param fetchedAt when TAGO was called (조회 시각); {@code null} unless AVAILABLE or NO_BUS
 */
public record ArrivalLookup(RealtimeStatus status, LocalDateTime fetchedAt, List<Arrival> arrivals) {

    static ArrivalLookup without(RealtimeStatus status) {
        return new ArrivalLookup(status, null, List.of());
    }
}
