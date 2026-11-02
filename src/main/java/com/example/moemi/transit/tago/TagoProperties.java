package com.example.moemi.transit.tago;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the TAGO bus services (국토교통부 국가대중교통정보센터) on data.go.kr.
 *
 * @param serviceKey       the service key issued by data.go.kr; the "Encoding" and "Decoding" forms both work
 * @param defaultCityCode  TAGO city code used when a request has none (24 = 광주광역시)
 * @param realtimeCacheTtl how long arrivals and bus locations are reused before calling TAGO again
 * @param staticCacheTtl   how long search results, routes and stops are reused
 */
@ConfigurationProperties("moemi.tago")
public record TagoProperties(
        String serviceKey,
        @DefaultValue("https://apis.data.go.kr/1613000") String baseUrl,
        @DefaultValue("24") String defaultCityCode,
        @DefaultValue("5s") Duration timeout,
        @DefaultValue("20s") Duration realtimeCacheTtl,
        @DefaultValue("12h") Duration staticCacheTtl) {

    public boolean hasServiceKey() {
        return serviceKey != null && !serviceKey.isBlank();
    }

    /** The plain key. The "Encoding" key is URL-encoded once more by the client, so it is decoded here. */
    public String decodedServiceKey() {
        String key = serviceKey.trim();
        return key.contains("%") ? URLDecoder.decode(key, StandardCharsets.UTF_8) : key;
    }
}
