package com.example.moemi.transit.dto;

import java.util.List;

import com.example.moemi.transit.Route;
import com.example.moemi.transit.RouteSearch;
import com.example.moemi.transit.SearchStatus;
import com.example.moemi.transit.Suggestion;
import com.example.moemi.transit.TransitService;

/**
 * Each route shows both termini ({@code startStopName} ↔ {@code endStopName}) so that routes with
 * similar numbers can be told apart; the direction is chosen on the route detail.
 *
 * @param keyword the searched keyword, or {@code null} for the routes of a stop
 */
public record RouteSearchResponse(
        String cityCode,
        String keyword,
        SearchStatus status,
        String source,
        List<Route> routes,
        List<Suggestion> suggestions) {

    public static RouteSearchResponse from(RouteSearch search) {
        return new RouteSearchResponse(search.cityCode(), search.keyword(), search.status(), TransitService.SOURCE,
                search.routes(), search.suggestions());
    }
}
