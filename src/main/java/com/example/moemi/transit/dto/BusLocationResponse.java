package com.example.moemi.transit.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.moemi.transit.BusLocation;
import com.example.moemi.transit.BusLocationLookup;
import com.example.moemi.transit.RealtimeStatus;
import com.example.moemi.transit.TransitService;

/**
 * Running buses of a route. Match {@link BusLocation#nodeOrder()} with the route detail's stop order
 * to draw them on the stop list.
 */
public record BusLocationResponse(
        String cityCode,
        String routeId,
        RealtimeStatus status,
        String source,
        LocalDateTime fetchedAt,
        List<BusLocation> buses) {

    public static BusLocationResponse from(String cityCode, String routeId, BusLocationLookup lookup) {
        return new BusLocationResponse(cityCode, routeId, lookup.status(), TransitService.SOURCE, lookup.fetchedAt(),
                lookup.buses());
    }
}
