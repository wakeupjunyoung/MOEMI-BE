package com.example.moemi.hazard;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.moemi.hazard.its.ItsEvent;
import com.example.moemi.hazard.its.ItsEventClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Feature 3: hazards (construction, closures, accidents) around a point.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HazardService {

    public static final String SOURCE = "국가교통정보센터 돌발상황정보";

    /** Rough bounds of South Korea, which the ITS source covers. */
    static final BoundingBox SERVICE_AREA = new BoundingBox(33.0, 38.7, 124.5, 132.0);

    private static final Comparator<Hazard> CURRENT_FIRST_THEN_NEAREST = Comparator
            .comparing((Hazard hazard) -> hazard.state() == HazardState.ENDED)
            .thenComparingInt(Hazard::distanceM);

    private final ItsEventClient itsClient;
    private final Clock clock;

    /**
     * @param types hazard types to keep; empty for all
     */
    public HazardSearch search(GeoPoint center, int radiusM, Set<HazardType> types) {
        if (!SERVICE_AREA.contains(center)) {
            return HazardSearch.withoutResult(HazardSearchStatus.UNSUPPORTED_REGION, radiusM);
        }

        List<ItsEvent> events;
        try {
            events = itsClient.fetch(BoundingBox.around(center, radiusM));
        } catch (HazardFetchException e) {
            log.warn("Hazard fetch failed: {}", e.getMessage());
            return HazardSearch.withoutResult(HazardSearchStatus.FAILED, radiusM);
        }

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        List<Hazard> hazards = events.stream()
                .map(event -> Hazard.from(event, center, now))
                .filter(hazard -> hazard.distanceM() <= radiusM)
                .filter(hazard -> types.isEmpty() || types.contains(hazard.type()))
                // The same incident can be listed twice; the app uses ids as list keys.
                .collect(Collectors.toMap(Hazard::id, Function.identity(), (first, duplicate) -> first, LinkedHashMap::new))
                .values().stream()
                .sorted(CURRENT_FIRST_THEN_NEAREST)
                .toList();

        HazardSearchStatus status = hazards.isEmpty() ? HazardSearchStatus.EMPTY : HazardSearchStatus.OK;
        return new HazardSearch(status, now, radiusM, hazards);
    }
}
