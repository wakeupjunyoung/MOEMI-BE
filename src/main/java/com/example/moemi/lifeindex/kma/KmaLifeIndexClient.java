package com.example.moemi.lifeindex.kma;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.moemi.lifeindex.LifeIndexFetchException;
import com.example.moemi.lifeindex.LifeIndexType;

/**
 * Calls KMA's living weather index service (생활기상지수 조회서비스 4.0, LivingWthrIdxServiceV5).
 */
@Component
public class KmaLifeIndexClient {

    private final KmaProperties properties;
    private final RestClient restClient;

    public KmaLifeIndexClient(KmaProperties properties) {
        this.properties = properties;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * @param announcedAt the announcement to read; KMA announces every 3 hours (00, 03, ..., 21)
     * @return the forecast, or empty when that announcement has no data (yet)
     */
    public Optional<IndexForecast> fetch(LifeIndexType type, String areaNo, LocalDateTime announcedAt) {
        if (!properties.hasServiceKey()) {
            throw new LifeIndexFetchException("KMA service key is not configured (moemi.kma.service-key)");
        }
        // Template variables are fully encoded, so '+', '/' and '=' in the key survive.
        URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .pathSegment(type.getOperation())
                .queryParam("serviceKey", "{serviceKey}")
                .queryParam("areaNo", "{areaNo}")
                .queryParam("time", "{time}")
                .queryParam("dataType", "JSON")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 10)
                .encode()
                .buildAndExpand(properties.serviceKey(), areaNo, announcedAt.format(KmaResponseParser.ANNOUNCE_TIME))
                .toUri();

        byte[] body;
        try {
            body = restClient.get().uri(uri).retrieve().body(byte[].class);
        } catch (RestClientResponseException e) {
            throw new LifeIndexFetchException("KMA " + type.getOperation() + " responded " + e.getStatusCode().value());
        } catch (RestClientException e) {
            // The exception message contains the request URI, which carries the service key.
            throw new LifeIndexFetchException("KMA " + type.getOperation() + " request failed: " + e.getClass().getSimpleName());
        }
        if (body == null) {
            throw new LifeIndexFetchException("KMA " + type.getOperation() + " returned an empty body");
        }
        return KmaResponseParser.parse(type, body);
    }
}
