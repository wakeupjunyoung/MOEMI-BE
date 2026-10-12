package com.example.moemi.hazard.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.moemi.hazard.HazardSearch;
import com.example.moemi.hazard.HazardSearchStatus;
import com.example.moemi.hazard.HazardService;

/**
 * @param fetchedAt when the source was queried (갱신 시각); {@code null} for UNSUPPORTED_REGION and FAILED
 */
public record HazardSearchResponse(
        HazardSearchStatus status,
        String source,
        LocalDateTime fetchedAt,
        int radiusM,
        List<HazardResponse> hazards) {

    public static HazardSearchResponse from(HazardSearch search) {
        return new HazardSearchResponse(
                search.status(),
                HazardService.SOURCE,
                search.fetchedAt(),
                search.radiusM(),
                search.hazards().stream().map(HazardResponse::from).toList());
    }
}
