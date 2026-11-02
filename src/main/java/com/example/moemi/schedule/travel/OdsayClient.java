package com.example.moemi.schedule.travel;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.moemi.hazard.GeoPoint;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Calls ODsay public transit routing (searchPubTransPathT) restricted to buses: which buses to
 * take between two points and how long it takes door to door.
 */
@Component
public class OdsayClient {

    /** ODsay SearchPathType: 0 = all, 1 = subway, 2 = bus. */
    private static final int BUS_ONLY = 2;
    /** ODsay: origin and destination are within 700 m, so it does not search transit. */
    private static final String ERROR_TOO_CLOSE = "-98";
    /** ODsay subPath trafficType for a bus ride. */
    private static final int TRAFFIC_BUS = 2;

    private final OdsayProperties properties;
    private final RestClient restClient;

    public OdsayClient(OdsayProperties properties) {
        this.properties = properties;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * The route ODsay ranks first among bus-only routes.
     *
     * @return empty when ODsay finds no bus route
     * @throws TravelFetchException when ODsay cannot be reached, rejects the key, or no key is configured
     */
    public Optional<BusRoute> route(GeoPoint origin, GeoPoint destination) {
        if (!properties.hasApiKey()) {
            throw new TravelFetchException("ODsay API key is not configured (moemi.odsay.api-key)");
        }
        // Template variables are fully encoded, so '+' and '/' in the key survive.
        URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path("/searchPubTransPathT")
                .queryParam("SX", coordinate(origin.lng()))
                .queryParam("SY", coordinate(origin.lat()))
                .queryParam("EX", coordinate(destination.lng()))
                .queryParam("EY", coordinate(destination.lat()))
                .queryParam("SearchPathType", BUS_ONLY)
                .queryParam("apiKey", "{apiKey}")
                .encode()
                .buildAndExpand(properties.apiKey().trim())
                .toUri();
        return parse(fetch(uri));
    }

    static Optional<BusRoute> parse(byte[] body) {
        JsonNode root;
        try {
            root = JsonMapper.shared().readTree(body);
        } catch (JacksonException e) {
            throw new TravelFetchException("ODsay response is not JSON");
        }
        JsonNode error = root.path("error");
        if (!error.isMissingNode()) {
            // ODsay sends either an object or an array of {code, message|msg}.
            JsonNode first = error.isArray() ? error.path(0) : error;
            String code = first.path("code").asString("");
            if (ERROR_TOO_CLOSE.equals(code)) {
                return Optional.of(BusRoute.TOO_CLOSE);
            }
            throw new TravelFetchException("ODsay error code=" + code + " "
                    + first.path("message").asString(first.path("msg").asString("")));
        }
        JsonNode path = root.path("result").path("path").path(0);
        JsonNode info = path.path("info");
        if (info.isMissingNode() || !info.has("totalTime")) {
            return Optional.empty();
        }
        List<String> busNumbers = new ArrayList<>();
        for (JsonNode subPath : path.path("subPath")) {
            if (subPath.path("trafficType").asInt(0) == TRAFFIC_BUS) {
                String busNo = subPath.path("lane").path(0).path("busNo").asString("");
                if (!busNo.isBlank()) {
                    busNumbers.add(busNo);
                }
            }
        }
        return Optional.of(new BusRoute(
                info.path("totalTime").asInt(0),
                info.path("busTransitCount").asInt(busNumbers.size()),
                info.path("totalWalk").asInt(0),
                List.copyOf(busNumbers),
                false));
    }

    private byte[] fetch(URI uri) {
        byte[] body;
        try {
            body = restClient.get().uri(uri).retrieve().body(byte[].class);
        } catch (RestClientResponseException e) {
            throw new TravelFetchException("ODsay responded " + e.getStatusCode().value());
        } catch (RestClientException e) {
            // The exception message contains the request URI, which carries the API key.
            throw new TravelFetchException("ODsay request failed: " + e.getClass().getSimpleName());
        }
        if (body == null || body.length == 0) {
            throw new TravelFetchException("ODsay returned an empty body");
        }
        return body;
    }

    private static String coordinate(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    /**
     * @param totalMinutes door-to-door time including walks and waits
     * @param busCount     number of buses taken
     * @param totalWalkM   walking distance within the route
     * @param busNumbers   the buses in riding order, e.g. ["송정19", "1187"]
     * @param tooClose     the points are too close for a bus; walk instead
     */
    public record BusRoute(int totalMinutes, int busCount, int totalWalkM, List<String> busNumbers, boolean tooClose) {

        static final BusRoute TOO_CLOSE = new BusRoute(0, 0, 0, List.of(), true);

        /** "송정19 → 1187 (환승 1회)", or "1187" for a single bus. */
        public String summary() {
            if (busNumbers.isEmpty()) {
                return TravelMode.BUS.getLabel();
            }
            String buses = String.join(" → ", busNumbers);
            return busCount > 1 ? buses + " (환승 " + (busCount - 1) + "회)" : buses;
        }
    }
}
