package com.example.moemi.transit;

/**
 * A stop on a route.
 *
 * @param order      position along the route (nodeord)
 * @param upDownCode TAGO's direction code (updowncd); stops sharing it form one direction
 */
public record RouteStop(String nodeId, String name, String number, int order, String upDownCode, Double lat, Double lng) {
}
