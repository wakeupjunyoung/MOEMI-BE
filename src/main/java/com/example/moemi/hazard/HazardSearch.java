package com.example.moemi.hazard;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param fetchedAt when the source was queried; {@code null} for UNSUPPORTED_REGION and FAILED
 * @param hazards   current hazards first, then by distance
 */
public record HazardSearch(HazardSearchStatus status, LocalDateTime fetchedAt, int radiusM, List<Hazard> hazards) {

    static HazardSearch withoutResult(HazardSearchStatus status, int radiusM) {
        return new HazardSearch(status, null, radiusM, List.of());
    }
}
