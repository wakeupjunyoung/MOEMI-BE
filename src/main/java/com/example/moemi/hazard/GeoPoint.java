package com.example.moemi.hazard;

/**
 * A WGS84 coordinate.
 */
public record GeoPoint(double lat, double lng) {

    private static final double EARTH_RADIUS_M = 6_371_000;

    /** Great-circle (haversine) distance in meters. */
    public double distanceTo(GeoPoint other) {
        double dLat = Math.toRadians(other.lat - lat);
        double dLng = Math.toRadians(other.lng - lng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a));
    }
}
