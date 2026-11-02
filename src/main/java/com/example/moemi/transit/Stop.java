package com.example.moemi.transit;

/**
 * A bus stop.
 *
 * @param nodeId TAGO's stop id, used by every other call (e.g. {@code GGB...})
 * @param number the number printed on the stop sign (정류소 번호); may be missing
 */
public record Stop(String cityCode, String nodeId, String name, String number, Double lat, Double lng) {
}
