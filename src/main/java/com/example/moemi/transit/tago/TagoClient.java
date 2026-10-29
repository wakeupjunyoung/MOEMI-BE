package com.example.moemi.transit.tago;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.moemi.transit.Arrival;
import com.example.moemi.transit.BusLocation;
import com.example.moemi.transit.City;
import com.example.moemi.transit.Route;
import com.example.moemi.transit.RouteStop;
import com.example.moemi.transit.Stop;
import com.example.moemi.transit.TransitFetchException;

import tools.jackson.databind.JsonNode;

/**
 * Calls the four TAGO bus services. Each one has to be approved separately on data.go.kr,
 * but they all accept the same service key.
 */
@Component
public class TagoClient {

    static final String STOP_SERVICE = "BusSttnInfoInqireService";
    static final String ROUTE_SERVICE = "BusRouteInfoInqireService";
    static final String ARRIVAL_SERVICE = "ArvlInfoInqireService";
    static final String BUS_LOCATION_SERVICE = "BusLcInfoInqireService";

    private static final int SEARCH_ROWS = 100;
    /** Enough for both directions of the longest city routes. */
    private static final int ALL_ROWS = 500;

    private final TagoProperties properties;
    private final RestClient restClient;

    public TagoClient(TagoProperties properties) {
        this.properties = properties;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /** Cities with real-time arrivals. */
    public List<City> fetchCities() {
        return call(ARRIVAL_SERVICE, "getCtyCodeList", Map.of(), SEARCH_ROWS).stream()
                .map(TagoMapper::city)
                .toList();
    }

    /** Stops whose name contains {@code name}, or whose number contains {@code number}; pass one of them. */
    public List<Stop> searchStops(String cityCode, String name, String number) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("cityCode", cityCode);
        if (name != null) {
            params.put("nodeNm", name);
        }
        if (number != null) {
            params.put("nodeNo", number);
        }
        return call(STOP_SERVICE, "getSttnNoList", params, SEARCH_ROWS).stream()
                .map(item -> TagoMapper.stop(cityCode, item))
                .toList();
    }

    /**
     * Stops within about 500 m of a point, of whichever city it is in. TAGO picks the radius;
     * each stop carries its own city code.
     */
    public List<Stop> findNearbyStops(double lat, double lng) {
        return call(STOP_SERVICE, "getCrdntPrxmtSttnList", Map.of("gpsLati", coordinate(lat), "gpsLong", coordinate(lng)),
                SEARCH_ROWS).stream()
                .map(TagoMapper::nearbyStop)
                .toList();
    }

    /** Routes that pass a stop. */
    public List<Route> fetchRoutesOfStop(String cityCode, String nodeId) {
        // This operation spells the stop parameter in lower case.
        return call(STOP_SERVICE, "getSttnThrghRouteList", Map.of("cityCode", cityCode, "nodeid", nodeId), ALL_ROWS)
                .stream()
                .map(item -> TagoMapper.route(cityCode, item))
                .toList();
    }

    /** Routes whose number contains {@code routeNo}. */
    public List<Route> searchRoutes(String cityCode, String routeNo) {
        return call(ROUTE_SERVICE, "getRouteNoList", Map.of("cityCode", cityCode, "routeNo", routeNo), SEARCH_ROWS)
                .stream()
                .map(item -> TagoMapper.route(cityCode, item))
                .toList();
    }

    /** @return the route, or empty when TAGO no longer knows it (discontinued or renumbered) */
    public Optional<Route> fetchRoute(String cityCode, String routeId) {
        return call(ROUTE_SERVICE, "getRouteInfoIem", Map.of("cityCode", cityCode, "routeId", routeId), 10)
                .stream()
                .findFirst()
                .map(item -> TagoMapper.route(cityCode, item));
    }

    public List<RouteStop> fetchRouteStops(String cityCode, String routeId) {
        return call(ROUTE_SERVICE, "getRouteAcctoThrghSttnList", Map.of("cityCode", cityCode, "routeId", routeId),
                ALL_ROWS).stream()
                .map(TagoMapper::routeStop)
                .toList();
    }

    /** Every bus on its way to a stop, of all routes. */
    public List<Arrival> fetchArrivals(String cityCode, String nodeId) {
        return call(ARRIVAL_SERVICE, "getSttnAcctoArvlPrearngeInfoList", Map.of("cityCode", cityCode, "nodeId", nodeId),
                ALL_ROWS).stream()
                .map(TagoMapper::arrival)
                .toList();
    }

    public List<BusLocation> fetchBusLocations(String cityCode, String routeId) {
        return call(BUS_LOCATION_SERVICE, "getRouteAcctoBusLcList", Map.of("cityCode", cityCode, "routeId", routeId),
                ALL_ROWS).stream()
                .map(TagoMapper::busLocation)
                .toList();
    }

    private List<JsonNode> call(String service, String operation, Map<String, String> params, int rows) {
        if (!properties.hasServiceKey()) {
            throw new TransitFetchException("TAGO service key is not configured (moemi.tago.service-key)");
        }
        // Template variables are fully encoded, so '+', '/' and '=' in the key and Korean keywords survive.
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .pathSegment(service, operation)
                .queryParam("serviceKey", "{serviceKey}");
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("serviceKey", properties.decodedServiceKey());
        params.forEach((name, value) -> {
            builder.queryParam(name, "{" + name + "}");
            variables.put(name, value);
        });
        URI uri = builder
                .queryParam("_type", "json")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", rows)
                .encode()
                .buildAndExpand(variables)
                .toUri();

        byte[] body;
        try {
            body = restClient.get().uri(uri).retrieve().body(byte[].class);
        } catch (RestClientResponseException e) {
            throw new TransitFetchException("TAGO " + operation + " responded " + e.getStatusCode().value());
        } catch (RestClientException e) {
            // The exception message contains the request URI, which carries the service key.
            throw new TransitFetchException("TAGO " + operation + " request failed: " + e.getClass().getSimpleName());
        }
        if (body == null || body.length == 0) {
            throw new TransitFetchException("TAGO " + operation + " returned an empty body");
        }
        return TagoResponseParser.items(operation, body);
    }

    private static String coordinate(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
