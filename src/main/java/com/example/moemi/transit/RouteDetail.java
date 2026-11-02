package com.example.moemi.transit;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A route with its stops split by direction.
 *
 * @param route     {@code null} unless OK or STALE
 * @param fetchedAt when the route was fetched from TAGO; {@code null} unless OK or STALE
 */
public record RouteDetail(RouteDetailStatus status, LocalDateTime fetchedAt, Route route,
        List<RouteDirection> directions) {

    static RouteDetail withoutRoute(RouteDetailStatus status) {
        return new RouteDetail(status, null, null, List.of());
    }
}
