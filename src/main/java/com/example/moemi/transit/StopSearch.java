package com.example.moemi.transit;

import java.util.List;

public record StopSearch(String cityCode, String keyword, SearchStatus status, List<Stop> stops,
        List<Suggestion> suggestions) {

    static StopSearch failed(String cityCode, String keyword) {
        return new StopSearch(cityCode, keyword, SearchStatus.FAILED, List.of(), List.of());
    }
}
