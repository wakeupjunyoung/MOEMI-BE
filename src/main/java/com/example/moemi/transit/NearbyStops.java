package com.example.moemi.transit;

import java.util.List;

import com.example.moemi.hazard.GeoPoint;

/**
 * Stops around a point, nearest first.
 */
public record NearbyStops(GeoPoint center, SearchStatus status, List<NearbyStop> stops) {

    static NearbyStops failed(GeoPoint center) {
        return new NearbyStops(center, SearchStatus.FAILED, List.of());
    }

    /**
     * @param distanceM straight-line distance from the requested point
     */
    public record NearbyStop(Stop stop, int distanceM) {
    }
}
