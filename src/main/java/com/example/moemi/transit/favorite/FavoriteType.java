package com.example.moemi.transit.favorite;

/**
 * What a favorite points at.
 */
public enum FavoriteType {
    /** A stop: arrivals of every route there. */
    STOP(true, false),
    /** A route: its running buses. */
    ROUTE(false, true),
    /** One route at one stop, in one direction ("우리집 앞 정류장 - 302번"): that route's arrivals. */
    STOP_ROUTE(true, true);

    private final boolean hasStop;
    private final boolean hasRoute;

    FavoriteType(boolean hasStop, boolean hasRoute) {
        this.hasStop = hasStop;
        this.hasRoute = hasRoute;
    }

    public boolean hasStop() {
        return hasStop;
    }

    public boolean hasRoute() {
        return hasRoute;
    }
}
