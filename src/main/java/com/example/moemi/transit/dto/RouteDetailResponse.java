package com.example.moemi.transit.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.moemi.transit.Route;
import com.example.moemi.transit.RouteDetail;
import com.example.moemi.transit.RouteDetailStatus;
import com.example.moemi.transit.RouteDirection;
import com.example.moemi.transit.TransitService;

/**
 * @param fetchedAt when the route was fetched from TAGO; {@code null} unless OK or STALE
 */
public record RouteDetailResponse(
        RouteDetailStatus status,
        String source,
        LocalDateTime fetchedAt,
        Route route,
        List<RouteDirection> directions) {

    public static RouteDetailResponse from(RouteDetail detail) {
        return new RouteDetailResponse(detail.status(), TransitService.SOURCE, detail.fetchedAt(), detail.route(),
                detail.directions());
    }
}
