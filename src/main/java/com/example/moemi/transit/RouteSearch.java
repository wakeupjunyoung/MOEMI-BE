package com.example.moemi.transit;

import java.util.List;

/**
 * @param keyword the searched keyword, or {@code null} for the routes of a stop
 */
public record RouteSearch(String cityCode, String keyword, SearchStatus status, List<Route> routes,
        List<Suggestion> suggestions) {

    static RouteSearch failed(String cityCode, String keyword) {
        return new RouteSearch(cityCode, keyword, SearchStatus.FAILED, List.of(), List.of());
    }
}
