package com.example.moemi.transit.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.moemi.transit.ArrivalLookup;
import com.example.moemi.transit.RealtimeStatus;
import com.example.moemi.transit.TransitService;

/**
 * @param routeId   the route the arrivals were filtered by, or {@code null} for all routes
 * @param fetchedAt when TAGO was called (조회 시각); {@code null} unless AVAILABLE or NO_BUS
 */
public record ArrivalLookupResponse(
        String cityCode,
        String nodeId,
        String routeId,
        RealtimeStatus status,
        String source,
        LocalDateTime fetchedAt,
        List<ArrivalResponse> arrivals) {

    public static ArrivalLookupResponse from(String cityCode, String nodeId, String routeId, ArrivalLookup lookup) {
        return new ArrivalLookupResponse(cityCode, nodeId, routeId, lookup.status(), TransitService.SOURCE,
                lookup.fetchedAt(),
                lookup.arrivals().stream().map(arrival -> ArrivalResponse.from(arrival, lookup.fetchedAt())).toList());
    }
}
