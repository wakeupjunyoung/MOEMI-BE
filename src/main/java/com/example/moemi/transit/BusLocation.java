package com.example.moemi.transit;

/**
 * A running bus of a route.
 *
 * @param nodeId    the stop the bus is at or just passed
 * @param nodeOrder that stop's position along the route, matching {@link RouteStop#order()}
 */
public record BusLocation(String vehicleNo, String nodeId, String nodeName, Integer nodeOrder, Double lat, Double lng) {
}
