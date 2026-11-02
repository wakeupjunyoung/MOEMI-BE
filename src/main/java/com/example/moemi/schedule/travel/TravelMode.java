package com.example.moemi.schedule.travel;

/**
 * How the user gets to a schedule's place. The app is bus-centered, so only walking and buses.
 */
public enum TravelMode {
    WALK("도보"),
    BUS("버스");

    private final String label;

    TravelMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
