package com.example.moemi.hazard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.moemi.hazard.its.ItsEvent;
import com.example.moemi.hazard.its.ItsEventClient;

class HazardServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 14, 0);
    private static final GeoPoint CENTER = new GeoPoint(35.19, 126.82);

    private final ItsEventClient client = mock(ItsEventClient.class);
    private HazardService service;

    @BeforeEach
    void setUp() {
        service = new HazardService(client, Clock.fixed(NOW.atZone(KST).toInstant(), KST));
    }

    @Test
    void keepsHazardsWithinRadiusCurrentFirstThenNearest() {
        ItsEvent farAccident = event("교통사고", null, null, 0.008, null);           // ~890m
        ItsEvent nearWork = event("작업", null, "2차로 차단", 0.003, null);            // ~330m
        ItsEvent endedNearest = event("공사", null, null, 0.001, NOW.minusDays(1));   // ~110m, ended
        ItsEvent outside = event("공사", null, null, 0.02, null);                     // ~2.2km
        when(client.fetch(any())).thenReturn(List.of(farAccident, nearWork, endedNearest, outside));

        HazardSearch search = service.search(CENTER, 1000, Set.of());

        assertThat(search.status()).isEqualTo(HazardSearchStatus.OK);
        assertThat(search.fetchedAt()).isEqualTo(NOW);
        assertThat(search.hazards()).extracting(Hazard::type)
                .containsExactly(HazardType.CONSTRUCTION, HazardType.ACCIDENT, HazardType.CONSTRUCTION);
        assertThat(search.hazards()).extracting(Hazard::state)
                .containsExactly(HazardState.ACTIVE, HazardState.ACTIVE, HazardState.ENDED);
        assertThat(search.hazards().get(0).distanceM()).isBetween(300, 360);
    }

    @Test
    void filtersByType() {
        when(client.fetch(any())).thenReturn(List.of(
                event("교통사고", null, null, 0.001, null),
                event("기타돌발", "전면통제", null, 0.002, null)));

        HazardSearch search = service.search(CENTER, 1000, Set.of(HazardType.CONTROL));

        assertThat(search.hazards()).singleElement().extracting(Hazard::type).isEqualTo(HazardType.CONTROL);
    }

    @Test
    void sameIncidentListedTwiceAppearsOnce() {
        ItsEvent accident = event("교통사고", null, null, 0.001, null);
        when(client.fetch(any())).thenReturn(List.of(accident, accident));

        assertThat(service.search(CENTER, 1000, Set.of()).hazards()).hasSize(1);
    }

    @Test
    void nothingNearbyIsEmptyNotFailure() {
        when(client.fetch(any())).thenReturn(List.of());

        HazardSearch search = service.search(CENTER, 1000, Set.of());

        assertThat(search.status()).isEqualTo(HazardSearchStatus.EMPTY);
        assertThat(search.fetchedAt()).isEqualTo(NOW);
    }

    @Test
    void sourceErrorIsFailed() {
        when(client.fetch(any())).thenThrow(new HazardFetchException("ITS eventInfo responded 500"));

        HazardSearch search = service.search(CENTER, 1000, Set.of());

        assertThat(search.status()).isEqualTo(HazardSearchStatus.FAILED);
        assertThat(search.hazards()).isEmpty();
    }

    @Test
    void outsideKoreaIsUnsupportedWithoutCallingTheSource() {
        HazardSearch search = service.search(new GeoPoint(35.68, 139.76), 1000, Set.of());

        assertThat(search.status()).isEqualTo(HazardSearchStatus.UNSUPPORTED_REGION);
        verifyNoInteractions(client);
    }

    @Test
    void classifiesSourceTypes() {
        assertThat(HazardType.classify("교통사고", null, null)).isEqualTo(HazardType.ACCIDENT);
        assertThat(HazardType.classify("공사", "전면통제", null)).isEqualTo(HazardType.CONSTRUCTION);
        assertThat(HazardType.classify("작업", null, null)).isEqualTo(HazardType.CONSTRUCTION);
        assertThat(HazardType.classify("재난", "전면통제", null)).isEqualTo(HazardType.CONTROL);
        assertThat(HazardType.classify("기상", null, null)).isEqualTo(HazardType.OTHER);
    }

    /** An incident {@code northDegrees} north of the center. */
    private static ItsEvent event(String eventType, String blockType, String blockedLanes, double northDegrees,
            LocalDateTime endAt) {
        GeoPoint location = new GeoPoint(CENTER.lat() + northDegrees, CENTER.lng());
        return new ItsEvent("국도", eventType, eventType, NOW.minusHours(2), endAt, location, "link-" + northDegrees,
                "국도1호선", "1", "상행", blockType, blockedLanes, eventType + " 원문");
    }
}
