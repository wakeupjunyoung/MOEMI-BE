package com.example.moemi.transit.favorite.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.moemi.transit.RealtimeStatus;
import com.example.moemi.transit.dto.ArrivalResponse;

/**
 * A favorite with its real-time state, for the home screen. The favorite is always present;
 * only {@code realtime} reflects whether TAGO answered.
 */
public record FavoriteRealtimeResponse(FavoriteResponse favorite, Realtime realtime) {

    /**
     * @param fetchedAt       when TAGO was called (조회 시각); {@code null} unless AVAILABLE or NO_BUS
     * @param arrivals        soonest buses for STOP and STOP_ROUTE favorites
     * @param runningBusCount buses on the road for ROUTE favorites; {@code null} otherwise
     */
    public record Realtime(
            RealtimeStatus status,
            LocalDateTime fetchedAt,
            List<ArrivalResponse> arrivals,
            Integer runningBusCount) {

        public static Realtime without(RealtimeStatus status) {
            return new Realtime(status, null, List.of(), null);
        }
    }
}
