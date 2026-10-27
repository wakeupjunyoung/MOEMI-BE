package com.example.moemi.lifeindex.kma;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for KMA's living weather index service on data.go.kr.
 *
 * @param serviceKey the "Decoding" service key issued by data.go.kr
 * @param cacheTtl   how long a region's fetched indices are reused before calling KMA again
 */
@ConfigurationProperties("moemi.kma")
public record KmaProperties(
        String serviceKey,
        @DefaultValue("https://apis.data.go.kr/1360000/LivingWthrIdxServiceV5") String baseUrl,
        @DefaultValue("30m") Duration cacheTtl,
        @DefaultValue("5s") Duration timeout) {

    public boolean hasServiceKey() {
        return serviceKey != null && !serviceKey.isBlank();
    }
}
