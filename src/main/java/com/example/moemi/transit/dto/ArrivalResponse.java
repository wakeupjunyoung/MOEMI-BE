package com.example.moemi.transit.dto;

import java.time.LocalDateTime;

import com.example.moemi.transit.Arrival;

/**
 * @param arrivalSeconds seconds until arrival as of {@code fetchedAt} of the enclosing response
 * @param arrivalMinutes the same, rounded down ("곧 도착" when 0)
 * @param expectedAt     {@code fetchedAt + arrivalSeconds}; lets the app count down between refreshes
 */
public record ArrivalResponse(
        String routeId,
        String routeNo,
        String routeType,
        String vehicleType,
        Integer remainingStops,
        Integer arrivalSeconds,
        Integer arrivalMinutes,
        LocalDateTime expectedAt) {

    public static ArrivalResponse from(Arrival arrival, LocalDateTime fetchedAt) {
        Integer seconds = arrival.arrivalSeconds();
        return new ArrivalResponse(
                arrival.routeId(),
                arrival.routeNo(),
                arrival.routeType(),
                arrival.vehicleType(),
                arrival.remainingStops(),
                seconds,
                seconds == null ? null : seconds / 60,
                seconds == null || fetchedAt == null ? null : fetchedAt.plusSeconds(seconds));
    }
}
