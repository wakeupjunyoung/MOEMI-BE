package com.example.moemi.transit;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Running buses of a route.
 *
 * @param fetchedAt when TAGO was called (조회 시각); {@code null} unless AVAILABLE or NO_BUS
 */
public record BusLocationLookup(RealtimeStatus status, LocalDateTime fetchedAt, List<BusLocation> buses) {

    static BusLocationLookup without(RealtimeStatus status) {
        return new BusLocationLookup(status, null, List.of());
    }
}
