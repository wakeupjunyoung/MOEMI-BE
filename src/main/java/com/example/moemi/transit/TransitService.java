package com.example.moemi.transit;

import java.time.Clock;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.moemi.common.error.ApiException;
import com.example.moemi.hazard.GeoPoint;
import com.example.moemi.transit.TtlCache.Cached;
import com.example.moemi.transit.tago.TagoClient;
import com.example.moemi.transit.tago.TagoProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Feature 4: bus stops, routes and real-time arrivals from TAGO. Every TAGO call goes through a cache:
 * real-time data for a few seconds, static data for hours (and kept as a fallback when TAGO fails).
 */
@Slf4j
@Service
public class TransitService {

    public static final String SOURCE = "국토교통부 국가대중교통정보센터(TAGO)";

    static final int MAX_SUGGESTIONS = 3;
    private static final List<String> STOP_SUFFIXES = List.of("버스정류장", "정류장", "정류소", "승강장");
    private static final Comparator<Arrival> SOONEST_FIRST = Comparator.comparing(
            Arrival::arrivalSeconds, Comparator.nullsLast(Comparator.naturalOrder()));
    private static final Comparator<Route> BY_ROUTE_NO = Comparator
            .comparing((Route route) -> route.routeNo() == null ? Integer.MAX_VALUE : route.routeNo().length())
            .thenComparing(Route::routeNo, Comparator.nullsLast(Comparator.naturalOrder()));

    private final TagoClient client;
    private final TagoProperties properties;

    private final TtlCache<String, List<City>> cityCache;
    private final TtlCache<Key, List<Stop>> stopSearchCache;
    private final TtlCache<GeoPoint, List<Stop>> nearbyCache;
    private final TtlCache<Key, List<Route>> routeSearchCache;
    private final TtlCache<Key, List<Route>> stopRoutesCache;
    private final TtlCache<Key, Optional<Route>> routeCache;
    private final TtlCache<Key, List<RouteStop>> routeStopsCache;
    private final TtlCache<Key, List<Arrival>> arrivalCache;
    private final TtlCache<Key, List<BusLocation>> busLocationCache;

    public TransitService(TagoClient client, TagoProperties properties, Clock clock) {
        this.client = client;
        this.properties = properties;
        this.cityCache = new TtlCache<>(properties.staticCacheTtl(), clock);
        this.stopSearchCache = new TtlCache<>(properties.staticCacheTtl(), clock);
        this.nearbyCache = new TtlCache<>(properties.staticCacheTtl(), clock);
        this.routeSearchCache = new TtlCache<>(properties.staticCacheTtl(), clock);
        this.stopRoutesCache = new TtlCache<>(properties.staticCacheTtl(), clock);
        this.routeCache = new TtlCache<>(properties.staticCacheTtl(), clock);
        this.routeStopsCache = new TtlCache<>(properties.staticCacheTtl(), clock);
        this.arrivalCache = new TtlCache<>(properties.realtimeCacheTtl(), clock);
        this.busLocationCache = new TtlCache<>(properties.realtimeCacheTtl(), clock);
    }

    /** The given city code, or the configured first supported city when none is given. */
    public String resolveCityCode(String cityCode) {
        return cityCode == null || cityCode.isBlank() ? properties.defaultCityCode() : cityCode.trim();
    }

    /** Cities with real-time information. */
    public List<City> cities() {
        try {
            return cityCache.get("all", client::fetchCities, true).value();
        } catch (TransitFetchException e) {
            log.warn("TAGO city list failed: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "TRANSIT_UNAVAILABLE", "지역 목록을 불러오지 못했습니다.");
        }
    }

    /**
     * Stops by name, or by stop number when the keyword is all digits. Stops of the same name
     * (both sides of a road) are listed next to each other.
     */
    public StopSearch searchStops(String cityCode, String keyword) {
        String query = keyword.trim();
        try {
            List<Stop> stops = findStops(cityCode, query);
            if (!stops.isEmpty()) {
                return new StopSearch(cityCode, query, SearchStatus.OK, stops, List.of());
            }
            List<Suggestion> suggestions = suggest(stopKeywordCandidates(query), candidate -> findStops(cityCode, candidate));
            return new StopSearch(cityCode, query, SearchStatus.EMPTY, List.of(), suggestions);
        } catch (TransitFetchException e) {
            log.warn("TAGO stop search failed: {}", e.getMessage());
            return StopSearch.failed(cityCode, query);
        }
    }

    public RouteSearch searchRoutes(String cityCode, String keyword) {
        String query = keyword.trim();
        try {
            List<Route> routes = findRoutes(cityCode, query);
            if (!routes.isEmpty()) {
                return new RouteSearch(cityCode, query, SearchStatus.OK, routes, List.of());
            }
            List<Suggestion> suggestions = suggest(routeKeywordCandidates(query), candidate -> findRoutes(cityCode, candidate));
            return new RouteSearch(cityCode, query, SearchStatus.EMPTY, List.of(), suggestions);
        } catch (TransitFetchException e) {
            log.warn("TAGO route search failed: {}", e.getMessage());
            return RouteSearch.failed(cityCode, query);
        }
    }

    /**
     * Stops around the user, for picking "the stop in front of my house" without knowing its name.
     * Points within about 10 m share one cached TAGO call.
     */
    public NearbyStops nearbyStops(GeoPoint center) {
        GeoPoint rounded = new GeoPoint(round(center.lat()), round(center.lng()));
        try {
            List<Stop> stops = nearbyCache.get(rounded, () -> client.findNearbyStops(rounded.lat(), rounded.lng()), true)
                    .value();
            List<NearbyStops.NearbyStop> nearest = stops.stream()
                    .map(stop -> new NearbyStops.NearbyStop(stop, distanceM(center, stop)))
                    .sorted(Comparator.comparingInt(NearbyStops.NearbyStop::distanceM))
                    .toList();
            return new NearbyStops(center, nearest.isEmpty() ? SearchStatus.EMPTY : SearchStatus.OK, nearest);
        } catch (TransitFetchException e) {
            log.warn("TAGO nearby stops failed: {}", e.getMessage());
            return NearbyStops.failed(center);
        }
    }

    /** Routes passing a stop, for the stop detail screen. */
    public RouteSearch routesOfStop(String cityCode, String nodeId) {
        try {
            List<Route> routes = stopRoutesCache.get(new Key(cityCode, nodeId),
                    () -> client.fetchRoutesOfStop(cityCode, nodeId).stream().sorted(BY_ROUTE_NO).toList(), true).value();
            SearchStatus status = routes.isEmpty() ? SearchStatus.EMPTY : SearchStatus.OK;
            return new RouteSearch(cityCode, null, status, routes, List.of());
        } catch (TransitFetchException e) {
            log.warn("TAGO routes of stop failed: {}", e.getMessage());
            return RouteSearch.failed(cityCode, null);
        }
    }

    public RouteDetail routeDetail(String cityCode, String routeId) {
        Key key = new Key(cityCode, routeId);
        try {
            Cached<Optional<Route>> route = routeCache.get(key, () -> client.fetchRoute(cityCode, routeId), true);
            if (route.value().isEmpty()) {
                return RouteDetail.withoutRoute(RouteDetailStatus.NOT_FOUND);
            }
            Cached<List<RouteStop>> stops = routeStopsCache.get(key, () -> client.fetchRouteStops(cityCode, routeId), true);
            RouteDetailStatus status = route.stale() || stops.stale() ? RouteDetailStatus.STALE : RouteDetailStatus.OK;
            Route found = route.value().get();
            return new RouteDetail(status, route.fetchedAt(), found, RouteDirection.group(stops.value(), found.endStopName()));
        } catch (TransitFetchException e) {
            log.warn("TAGO route detail failed: {}", e.getMessage());
            return RouteDetail.withoutRoute(RouteDetailStatus.FAILED);
        }
    }

    /**
     * @return whether TAGO still knows the route, or empty when that cannot be told right now
     */
    public Optional<Boolean> routeExists(String cityCode, String routeId) {
        try {
            return Optional.of(routeCache.get(new Key(cityCode, routeId),
                    () -> client.fetchRoute(cityCode, routeId), true).value().isPresent());
        } catch (TransitFetchException e) {
            log.warn("TAGO route check failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * @param routeId only this route's buses, or {@code null} for every route at the stop
     */
    public ArrivalLookup arrivals(String cityCode, String nodeId, String routeId) {
        if (!supportsRealtime(cityCode)) {
            return ArrivalLookup.without(RealtimeStatus.NOT_SUPPORTED);
        }
        Cached<List<Arrival>> cached;
        try {
            // One call per stop serves every route, so favorites of different routes at a stop share it.
            cached = arrivalCache.get(new Key(cityCode, nodeId), () -> client.fetchArrivals(cityCode, nodeId), false);
        } catch (TransitFetchException e) {
            log.warn("TAGO arrivals failed: {}", e.getMessage());
            return ArrivalLookup.without(RealtimeStatus.FAILED);
        }
        List<Arrival> arrivals = cached.value().stream()
                .filter(arrival -> routeId == null || routeId.equals(arrival.routeId()))
                .sorted(SOONEST_FIRST)
                .toList();
        RealtimeStatus status = arrivals.isEmpty() ? RealtimeStatus.NO_BUS : RealtimeStatus.AVAILABLE;
        return new ArrivalLookup(status, cached.fetchedAt(), arrivals);
    }

    public BusLocationLookup busLocations(String cityCode, String routeId) {
        if (!supportsRealtime(cityCode)) {
            return BusLocationLookup.without(RealtimeStatus.NOT_SUPPORTED);
        }
        Cached<List<BusLocation>> cached;
        try {
            cached = busLocationCache.get(new Key(cityCode, routeId),
                    () -> client.fetchBusLocations(cityCode, routeId), false);
        } catch (TransitFetchException e) {
            log.warn("TAGO bus locations failed: {}", e.getMessage());
            return BusLocationLookup.without(RealtimeStatus.FAILED);
        }
        RealtimeStatus status = cached.value().isEmpty() ? RealtimeStatus.NO_BUS : RealtimeStatus.AVAILABLE;
        return new BusLocationLookup(status, cached.fetchedAt(), cached.value());
    }

    /** Unknown when the city list cannot be read; the real-time call then decides. */
    private boolean supportsRealtime(String cityCode) {
        try {
            List<City> cities = cityCache.get("all", client::fetchCities, true).value();
            return cities.isEmpty() || cities.stream().anyMatch(city -> cityCode.equals(city.code()));
        } catch (TransitFetchException e) {
            return true;
        }
    }

    private List<Stop> findStops(String cityCode, String query) {
        boolean byNumber = query.chars().allMatch(Character::isDigit);
        return stopSearchCache.get(new Key(cityCode, (byNumber ? "no:" : "nm:") + query),
                () -> client.searchStops(cityCode, byNumber ? null : query, byNumber ? query : null).stream()
                        .sorted(Comparator.comparing(Stop::name, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(Stop::number, Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList(),
                true).value();
    }

    /** About 11 m at Korean latitudes. */
    private static double round(double degrees) {
        return Math.round(degrees * 10_000) / 10_000.0;
    }

    private static int distanceM(GeoPoint center, Stop stop) {
        if (stop.lat() == null || stop.lng() == null) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.round(center.distanceTo(new GeoPoint(stop.lat(), stop.lng())));
    }

    private List<Route> findRoutes(String cityCode, String query) {
        return routeSearchCache.get(new Key(cityCode, query),
                () -> client.searchRoutes(cityCode, query).stream().sorted(BY_ROUTE_NO).toList(), true).value();
    }

    /** Tries each candidate and keeps those with results. A failing candidate is skipped. */
    private static List<Suggestion> suggest(List<String> candidates, Function<String, List<?>> search) {
        return candidates.stream()
                .map(candidate -> {
                    try {
                        return new Suggestion(candidate, search.apply(candidate).size());
                    } catch (TransitFetchException e) {
                        return new Suggestion(candidate, 0);
                    }
                })
                .filter(suggestion -> suggestion.resultCount() > 0)
                .toList();
    }

    /** Common slips in stop names: spaces, a "정류장" suffix, one extra character at the end. */
    static List<String> stopKeywordCandidates(String query) {
        String compact = query.replaceAll("\\s+", "");
        Stream.Builder<String> candidates = Stream.builder();
        candidates.add(compact);
        STOP_SUFFIXES.stream()
                .filter(suffix -> compact.endsWith(suffix) && compact.length() > suffix.length())
                .findFirst()
                .ifPresent(suffix -> candidates.add(compact.substring(0, compact.length() - suffix.length())));
        if (compact.length() >= 3) {
            candidates.add(compact.substring(0, compact.length() - 1));
        }
        return distinctCandidates(query, candidates.build());
    }

    /** Common slips in route numbers: "302번", "첨단 09", "302-1", one extra digit. */
    static List<String> routeKeywordCandidates(String query) {
        String compact = query.replaceAll("[\\s번]", "");
        Stream.Builder<String> candidates = Stream.builder();
        candidates.add(compact);
        candidates.add(compact.replace("-", ""));
        candidates.add(compact.replaceAll("\\D", ""));
        if (compact.length() >= 3) {
            candidates.add(compact.substring(0, compact.length() - 1));
        }
        return distinctCandidates(query, candidates.build());
    }

    private static List<String> distinctCandidates(String query, Stream<String> candidates) {
        Set<String> distinct = new LinkedHashSet<>();
        candidates.filter(candidate -> !candidate.isBlank() && !candidate.equals(query)).forEach(distinct::add);
        return distinct.stream().limit(MAX_SUGGESTIONS).toList();
    }

    private record Key(String cityCode, String id) {
    }
}
