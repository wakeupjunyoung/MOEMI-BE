package com.example.moemi.transit;

/**
 * A bus expected at a stop, as of the time it was fetched.
 *
 * @param remainingStops stops left before this one (arrprevstationcnt)
 * @param arrivalSeconds seconds until arrival at fetch time (arrtime)
 */
public record Arrival(
        String routeId,
        String routeNo,
        String routeType,
        String vehicleType,
        Integer remainingStops,
        Integer arrivalSeconds) {
}
