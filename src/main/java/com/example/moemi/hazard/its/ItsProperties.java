package com.example.moemi.hazard.its;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the National Transport Information Center open API (its.go.kr).
 */
@ConfigurationProperties("moemi.its")
public record ItsProperties(
        String apiKey,
        @DefaultValue("https://openapi.its.go.kr:9443") String baseUrl,
        @DefaultValue("5s") Duration timeout) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
