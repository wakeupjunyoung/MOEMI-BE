package com.example.moemi.transit;

/**
 * A bus route.
 *
 * @param routeNo       the number riders know, e.g. {@code 302} or {@code 첨단09}
 * @param routeType     TAGO's route type name, e.g. {@code 간선버스}
 * @param firstBusTime  first departure as {@code HH:mm}; may be missing
 * @param lastBusTime   last departure as {@code HH:mm}; may be missing
 * @param intervalMin   weekday headway in minutes; only known from the route detail
 */
public record Route(
        String cityCode,
        String routeId,
        String routeNo,
        String routeType,
        String startStopName,
        String endStopName,
        String firstBusTime,
        String lastBusTime,
        Integer intervalMin) {
}
