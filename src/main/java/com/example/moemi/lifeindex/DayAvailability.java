package com.example.moemi.lifeindex;

/**
 * Whether a calendar date has indices. {@code NO_DATA} is not an error: KMA only forecasts a few days ahead.
 */
public enum DayAvailability {
    PAST,
    AVAILABLE,
    NO_DATA
}
