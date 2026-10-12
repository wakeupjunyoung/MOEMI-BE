package com.example.moemi.hazard;

public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {

    private static final double METERS_PER_DEGREE_LAT = 111_320;

    /** The smallest box containing the circle of {@code radiusM} around {@code center}. */
    public static BoundingBox around(GeoPoint center, int radiusM) {
        double dLat = radiusM / METERS_PER_DEGREE_LAT;
        double dLng = radiusM / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(center.lat())));
        return new BoundingBox(center.lat() - dLat, center.lat() + dLat, center.lng() - dLng, center.lng() + dLng);
    }

    public boolean contains(GeoPoint point) {
        return point.lat() >= minLat && point.lat() <= maxLat && point.lng() >= minLng && point.lng() <= maxLng;
    }
}
