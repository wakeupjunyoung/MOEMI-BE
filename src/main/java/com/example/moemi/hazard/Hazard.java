package com.example.moemi.hazard;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.example.moemi.hazard.its.ItsEvent;

/**
 * A hazard near the searched point. The original text and classification from the source are kept,
 * since the app should not decide on its own how dangerous an incident is.
 *
 * @param id stable across searches, derived from the incident's fields (the source has no id)
 */
public record Hazard(
        String id,
        HazardType type,
        HazardState state,
        String title,
        String description,
        GeoPoint location,
        int distanceM,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String roadName,
        String direction,
        String blockType,
        String blockedLanes,
        String sourceEventType,
        String sourceEventDetailType) {

    public static Hazard from(ItsEvent event, GeoPoint center, LocalDateTime now) {
        return new Hazard(
                idOf(event),
                HazardType.classify(event.eventType(), event.blockType(), event.blockedLanes()),
                HazardState.of(event.startAt(), event.endAt(), now),
                titleOf(event),
                event.message(),
                event.location(),
                (int) Math.round(center.distanceTo(event.location())),
                event.startAt(),
                event.endAt(),
                event.roadName(),
                event.direction(),
                event.blockType(),
                event.blockedLanes(),
                event.eventType(),
                event.eventDetailType());
    }

    private static String idOf(ItsEvent event) {
        String key = Stream.of(event.linkId(), event.startAt(), event.location(), event.eventType(),
                        event.eventDetailType(), event.message())
                .map(String::valueOf)
                .collect(Collectors.joining("|"));
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** e.g. "호남선 도로포장"; falls back to whatever the source provides. */
    private static String titleOf(ItsEvent event) {
        String kind = event.eventDetailType() != null ? event.eventDetailType() : event.eventType();
        if (event.roadName() != null && kind != null) {
            return event.roadName() + " " + kind;
        }
        return Stream.of(event.roadName(), kind)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("돌발상황");
    }
}
