package com.example.moemi.transit.dto;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.moemi.transit.SearchStatus;
import com.example.moemi.transit.Stop;
import com.example.moemi.transit.StopSearch;
import com.example.moemi.transit.Suggestion;
import com.example.moemi.transit.TransitService;

public record StopSearchResponse(
        String cityCode,
        String keyword,
        SearchStatus status,
        String source,
        List<StopResponse> stops,
        List<Suggestion> suggestions) {

    public static StopSearchResponse from(StopSearch search) {
        Map<String, Long> countByName = search.stops().stream()
                .collect(Collectors.groupingBy(stop -> String.valueOf(stop.name()), Collectors.counting()));
        Function<Stop, StopResponse> toResponse = stop -> StopResponse.from(stop,
                countByName.getOrDefault(String.valueOf(stop.name()), 1L) > 1);
        return new StopSearchResponse(search.cityCode(), search.keyword(), search.status(), TransitService.SOURCE,
                search.stops().stream().map(toResponse).toList(), search.suggestions());
    }

    /**
     * @param sameNameExists another stop in the results has the same name; show the number and location
     *                       so the user can tell them apart (e.g. the stop across the road)
     */
    public record StopResponse(
            String cityCode,
            String nodeId,
            String name,
            String number,
            Double lat,
            Double lng,
            boolean sameNameExists) {

        static StopResponse from(Stop stop, boolean sameNameExists) {
            return new StopResponse(stop.cityCode(), stop.nodeId(), stop.name(), stop.number(), stop.lat(), stop.lng(),
                    sameNameExists);
        }
    }
}
