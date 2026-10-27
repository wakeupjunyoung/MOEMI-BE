package com.example.moemi.hazard.its;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Locale;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.moemi.hazard.BoundingBox;
import com.example.moemi.hazard.HazardFetchException;

/**
 * Calls the ITS incident API (돌발상황정보, eventInfo), which covers expressways, national and local roads nationwide.
 */
@Component
public class ItsEventClient {

    private final ItsProperties properties;
    private final RestClient restClient;

    public ItsEventClient(ItsProperties properties) {
        this.properties = properties;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /** All incidents of every road and event type inside the box. */
    public List<ItsEvent> fetch(BoundingBox box) {
        if (!properties.hasApiKey()) {
            throw new HazardFetchException("ITS API key is not configured (moemi.its.api-key)");
        }
        URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path("/eventInfo")
                .queryParam("apiKey", "{apiKey}")
                .queryParam("type", "all")
                .queryParam("eventType", "all")
                .queryParam("minX", coordinate(box.minLng()))
                .queryParam("maxX", coordinate(box.maxLng()))
                .queryParam("minY", coordinate(box.minLat()))
                .queryParam("maxY", coordinate(box.maxLat()))
                // XML is the documented default and the format whose structure has been checked.
                .queryParam("getType", "xml")
                .encode()
                .buildAndExpand(properties.apiKey())
                .toUri();

        byte[] body;
        try {
            body = restClient.get().uri(uri).retrieve().body(byte[].class);
        } catch (RestClientResponseException e) {
            throw new HazardFetchException("ITS eventInfo responded " + e.getStatusCode().value());
        } catch (RestClientException e) {
            // The exception message contains the request URI, which carries the API key.
            throw new HazardFetchException("ITS eventInfo request failed: " + e.getClass().getSimpleName());
        }
        if (body == null) {
            throw new HazardFetchException("ITS eventInfo returned an empty body");
        }
        return ItsEventParser.parse(body);
    }

    private static String coordinate(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
