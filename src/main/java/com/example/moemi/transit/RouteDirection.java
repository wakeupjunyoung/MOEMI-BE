package com.example.moemi.transit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One direction of a route with its stops in order, named after where it ends ("광주역 방면").
 * A stop of the same name on the other side of the road belongs to the other direction.
 */
public record RouteDirection(String upDownCode, String name, List<RouteStop> stops) {

    private static final String NO_CODE = "";

    /**
     * Splits a route's stops by direction. Cities that send direction codes (updowncd) are split by
     * code. Others, such as Gwangju, list a round trip as one sequence; it is split right after the
     * first stop named like the route's terminus, where the bus turns back.
     *
     * @param endStopName the route's terminus (endnodenm), or {@code null} when unknown
     */
    public static List<RouteDirection> group(List<RouteStop> stops, String endStopName) {
        List<RouteStop> ordered = stops.stream().sorted(Comparator.comparingInt(RouteStop::order)).toList();
        Map<String, List<RouteStop>> byCode = new LinkedHashMap<>();
        ordered.forEach(stop -> byCode.computeIfAbsent(
                stop.upDownCode() == null ? NO_CODE : stop.upDownCode(), code -> new ArrayList<>()).add(stop));

        List<RouteDirection> directions = new ArrayList<>();
        if (byCode.size() == 1) {
            String code = byCode.keySet().iterator().next();
            splitAtTerminus(ordered, endStopName).forEach(part -> directions.add(direction(code, part)));
        } else {
            byCode.forEach((code, directionStops) -> directions.add(direction(code, directionStops)));
        }
        return directions;
    }

    private static List<List<RouteStop>> splitAtTerminus(List<RouteStop> ordered, String endStopName) {
        for (int i = 0; i < ordered.size() - 1; i++) {
            if (ordered.get(i).name() != null && ordered.get(i).name().equals(endStopName)) {
                return List.of(ordered.subList(0, i + 1), ordered.subList(i + 1, ordered.size()));
            }
        }
        return ordered.isEmpty() ? List.of() : List.of(ordered);
    }

    private static RouteDirection direction(String code, List<RouteStop> directionStops) {
        String terminus = directionStops.get(directionStops.size() - 1).name();
        return new RouteDirection(code.isEmpty() ? null : code, terminus + " 방면", List.copyOf(directionStops));
    }
}
