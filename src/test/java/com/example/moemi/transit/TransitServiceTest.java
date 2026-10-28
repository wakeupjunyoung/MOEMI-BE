package com.example.moemi.transit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.moemi.hazard.GeoPoint;
import com.example.moemi.transit.tago.TagoClient;
import com.example.moemi.transit.tago.TagoProperties;

class TransitServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 8, 0);
    private static final String CITY = "24";
    private static final String STOP = "GJB5000001";

    private final TagoClient client = mock(TagoClient.class);
    private final MutableClock clock = new MutableClock(NOW.atZone(KST).toInstant());
    private TransitService service;

    @BeforeEach
    void setUp() {
        TagoProperties properties = new TagoProperties("key", "https://example.test", CITY, Duration.ofSeconds(5),
                Duration.ofSeconds(20), Duration.ofHours(12));
        service = new TransitService(client, properties, clock);
        when(client.fetchCities()).thenReturn(List.of(new City(CITY, "광주광역시")));
    }

    @Test
    void arrivalsAreFilteredByRouteAndSortedSoonestFirst() {
        when(client.fetchArrivals(CITY, STOP)).thenReturn(List.of(
                arrival("GJB300", "302", 600), arrival("GJB400", "첨단09", 60), arrival("GJB300", "302", 120)));

        ArrivalLookup lookup = service.arrivals(CITY, STOP, "GJB300");

        assertThat(lookup.status()).isEqualTo(RealtimeStatus.AVAILABLE);
        assertThat(lookup.fetchedAt()).isEqualTo(NOW);
        assertThat(lookup.arrivals()).extracting(Arrival::arrivalSeconds).containsExactly(120, 600);
    }

    @Test
    void arrivalsOfAStopAreFetchedOnceWithinTheCacheLifetime() {
        when(client.fetchArrivals(CITY, STOP)).thenReturn(List.of(arrival("GJB300", "302", 300)));

        service.arrivals(CITY, STOP, "GJB300");
        service.arrivals(CITY, STOP, null);
        clock.advance(Duration.ofSeconds(19));
        ArrivalLookup cached = service.arrivals(CITY, STOP, "GJB400");

        verify(client, times(1)).fetchArrivals(CITY, STOP);
        assertThat(cached.status()).isEqualTo(RealtimeStatus.NO_BUS);
        assertThat(cached.fetchedAt()).isEqualTo(NOW);

        clock.advance(Duration.ofSeconds(1));
        assertThat(service.arrivals(CITY, STOP, null).fetchedAt()).isEqualTo(NOW.plusSeconds(20));
        verify(client, times(2)).fetchArrivals(CITY, STOP);
    }

    @Test
    void arrivalFailureIsFailedNotAnOldValue() {
        when(client.fetchArrivals(CITY, STOP))
                .thenReturn(List.of(arrival("GJB300", "302", 300)))
                .thenThrow(new TransitFetchException("TAGO responded 500"));
        service.arrivals(CITY, STOP, null);
        clock.advance(Duration.ofMinutes(1));

        ArrivalLookup lookup = service.arrivals(CITY, STOP, null);

        assertThat(lookup.status()).isEqualTo(RealtimeStatus.FAILED);
        assertThat(lookup.arrivals()).isEmpty();
        assertThat(lookup.fetchedAt()).isNull();
    }

    @Test
    void cityWithoutRealtimeIsNotSupportedWithoutCallingArrivals() {
        ArrivalLookup lookup = service.arrivals("39", STOP, null);

        assertThat(lookup.status()).isEqualTo(RealtimeStatus.NOT_SUPPORTED);
        verify(client, never()).fetchArrivals(any(), any());
    }

    @Test
    void routeDetailSplitsStopsByDirectionNamedAfterTheTerminus() {
        when(client.fetchRoute(CITY, "GJB300")).thenReturn(Optional.of(route("GJB300", "302")));
        when(client.fetchRouteStops(CITY, "GJB300")).thenReturn(List.of(
                routeStop("A", "송정역", 1, "0"), routeStop("B", "광주역", 2, "0"),
                routeStop("C", "광주역", 3, "1"), routeStop("D", "송정역", 4, "1")));

        RouteDetail detail = service.routeDetail(CITY, "GJB300");

        assertThat(detail.status()).isEqualTo(RouteDetailStatus.OK);
        assertThat(detail.directions()).extracting(RouteDirection::name).containsExactly("광주역 방면", "송정역 방면");
        assertThat(detail.directions().get(1).stops()).extracting(RouteStop::nodeId).containsExactly("C", "D");
    }

    @Test
    void roundTripWithoutDirectionCodesIsSplitAtTheTerminus() {
        // Gwangju sends no updowncd; the terminus 장등동 appears on both sides of the road.
        when(client.fetchRoute(CITY, "KJB10")).thenReturn(Optional.of(
                new Route(CITY, "KJB10", "송정19", "간선버스", "도산동", "장등동", "05:40", "22:25", 11)));
        when(client.fetchRouteStops(CITY, "KJB10")).thenReturn(List.of(
                routeStop("KJB327", "도산동", 1, null), routeStop("KJB400", "송정역", 2, null),
                routeStop("KJB312", "장등동", 3, null), routeStop("KJB1165", "장등동", 4, null),
                routeStop("KJB401", "송정역", 5, null), routeStop("KJB328", "도산동", 6, null)));

        RouteDetail detail = service.routeDetail(CITY, "KJB10");

        assertThat(detail.directions()).extracting(RouteDirection::name).containsExactly("장등동 방면", "도산동 방면");
        assertThat(detail.directions().get(0).stops()).extracting(RouteStop::nodeId)
                .containsExactly("KJB327", "KJB400", "KJB312");
        assertThat(detail.directions().get(1).stops()).extracting(RouteStop::nodeId)
                .containsExactly("KJB1165", "KJB401", "KJB328");
    }

    @Test
    void discontinuedRouteIsNotFound() {
        when(client.fetchRoute(CITY, "GJB999")).thenReturn(Optional.empty());

        assertThat(service.routeDetail(CITY, "GJB999").status()).isEqualTo(RouteDetailStatus.NOT_FOUND);
        assertThat(service.routeExists(CITY, "GJB999")).contains(false);
    }

    @Test
    void routeDetailFallsBackToTheLastFetchWhenTagoFails() {
        when(client.fetchRoute(CITY, "GJB300"))
                .thenReturn(Optional.of(route("GJB300", "302")))
                .thenThrow(new TransitFetchException("TAGO responded 500"));
        when(client.fetchRouteStops(CITY, "GJB300")).thenReturn(List.of(routeStop("A", "송정역", 1, "0")));
        service.routeDetail(CITY, "GJB300");
        clock.advance(Duration.ofHours(13));

        RouteDetail detail = service.routeDetail(CITY, "GJB300");

        assertThat(detail.status()).isEqualTo(RouteDetailStatus.STALE);
        assertThat(detail.fetchedAt()).isEqualTo(NOW);
        assertThat(detail.route().routeNo()).isEqualTo("302");
    }

    @Test
    void numericKeywordSearchesByStopNumber() {
        when(client.searchStops(CITY, null, "5012")).thenReturn(List.of(stop("GJB1", "광주역", "5012")));

        StopSearch search = service.searchStops(CITY, " 5012 ");

        assertThat(search.status()).isEqualTo(SearchStatus.OK);
        assertThat(search.keyword()).isEqualTo("5012");
        assertThat(search.stops()).extracting(Stop::nodeId).containsExactly("GJB1");
    }

    @Test
    void emptySearchSuggestsSimilarKeywordsWithResults() {
        when(client.searchStops(eq(CITY), any(), isNull())).thenReturn(List.of());
        when(client.searchStops(CITY, "광주역", null)).thenReturn(List.of(stop("GJB1", "광주역", "1"), stop("GJB2", "광주역", "2")));

        StopSearch search = service.searchStops(CITY, "광주역 정류장");

        assertThat(search.status()).isEqualTo(SearchStatus.EMPTY);
        assertThat(search.suggestions()).containsExactly(new Suggestion("광주역", 2));
    }

    @Test
    void nearbyStopsAreNearestFirstAndShareACallForCloseCoordinates() {
        when(client.findNearbyStops(35.1650, 126.9090)).thenReturn(List.of(
                new Stop(CITY, "FAR", "광주역후문", "5100", 35.1680, 126.9090),
                new Stop(CITY, "NEAR", "광주역", "5012", 35.1652, 126.9090)));

        NearbyStops nearby = service.nearbyStops(new GeoPoint(35.16501, 126.90902));
        service.nearbyStops(new GeoPoint(35.16498, 126.90899));

        assertThat(nearby.status()).isEqualTo(SearchStatus.OK);
        assertThat(nearby.stops()).extracting(stop -> stop.stop().nodeId()).containsExactly("NEAR", "FAR");
        assertThat(nearby.stops().get(0).distanceM()).isBetween(15, 30);
        verify(client, times(1)).findNearbyStops(35.1650, 126.9090);
    }

    @Test
    void searchFailureIsFailed() {
        when(client.searchRoutes(CITY, "302")).thenThrow(new TransitFetchException("TAGO request failed"));

        assertThat(service.searchRoutes(CITY, "302").status()).isEqualTo(SearchStatus.FAILED);
    }

    @Test
    void keywordCandidatesCoverCommonSlips() {
        assertThat(TransitService.stopKeywordCandidates("광주 역")).containsExactly("광주역", "광주");
        assertThat(TransitService.stopKeywordCandidates("광주역정류장")).containsExactly("광주역", "광주역정류");
        assertThat(TransitService.routeKeywordCandidates("302번")).containsExactly("302", "30");
        assertThat(TransitService.routeKeywordCandidates("첨단 09")).containsExactly("첨단09", "09", "첨단0");
    }

    private static Arrival arrival(String routeId, String routeNo, int seconds) {
        return new Arrival(routeId, routeNo, "간선버스", "일반차량", seconds / 120, seconds);
    }

    private static Route route(String routeId, String routeNo) {
        return new Route(CITY, routeId, routeNo, "간선버스", "송정역", "송정역", "05:30", "22:30", 12);
    }

    private static RouteStop routeStop(String nodeId, String name, int order, String upDown) {
        return new RouteStop(nodeId, name, null, order, upDown, 35.1, 126.8);
    }

    private static Stop stop(String nodeId, String name, String number) {
        return new Stop(CITY, nodeId, name, number, 35.1, 126.8);
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return KST;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
