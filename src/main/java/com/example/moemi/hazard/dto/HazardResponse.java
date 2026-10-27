package com.example.moemi.hazard.dto;

import java.time.LocalDateTime;

import com.example.moemi.hazard.Hazard;
import com.example.moemi.hazard.HazardState;
import com.example.moemi.hazard.HazardType;

/**
 * @param description           the source's original text
 * @param startAt               when it started or will start; {@code null} if unknown
 * @param endAt                 when it ended or is planned to end; {@code null} if unknown
 * @param sourceEventType       the source's own classification, shown with the original text
 */
public record HazardResponse(
        String id,
        HazardType type,
        HazardState state,
        String title,
        String description,
        double lat,
        double lng,
        int distanceM,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String roadName,
        String direction,
        String blockType,
        String blockedLanes,
        String sourceEventType,
        String sourceEventDetailType) {

    public static HazardResponse from(Hazard hazard) {
        return new HazardResponse(
                hazard.id(),
                hazard.type(),
                hazard.state(),
                hazard.title(),
                hazard.description(),
                hazard.location().lat(),
                hazard.location().lng(),
                hazard.distanceM(),
                hazard.startAt(),
                hazard.endAt(),
                hazard.roadName(),
                hazard.direction(),
                hazard.blockType(),
                hazard.blockedLanes(),
                hazard.sourceEventType(),
                hazard.sourceEventDetailType());
    }
}
