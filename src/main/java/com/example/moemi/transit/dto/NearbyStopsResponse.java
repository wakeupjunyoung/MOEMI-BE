package com.example.moemi.transit.dto;

import java.util.List;

import com.example.moemi.transit.NearbyStops;
import com.example.moemi.transit.SearchStatus;
import com.example.moemi.transit.TransitService;

public record NearbyStopsResponse(
        double lat,
        double lng,
        SearchStatus status,
        String source,
        List<NearbyStopResponse> stops) {

    public static NearbyStopsResponse from(NearbyStops nearby) {
        return new NearbyStopsResponse(nearby.center().lat(), nearby.center().lng(), nearby.status(),
                TransitService.SOURCE, nearby.stops().stream().map(NearbyStopResponse::from).toList());
    }

    /**
     * @param cityCode the stop's own city; pass it on to arrival lookups and favorites
     */
    public record NearbyStopResponse(
            String cityCode,
            String nodeId,
            String name,
            String number,
            Double lat,
            Double lng,
            int distanceM) {

        static NearbyStopResponse from(NearbyStops.NearbyStop nearby) {
            return new NearbyStopResponse(nearby.stop().cityCode(), nearby.stop().nodeId(), nearby.stop().name(),
                    nearby.stop().number(), nearby.stop().lat(), nearby.stop().lng(), nearby.distanceM());
        }
    }
}
