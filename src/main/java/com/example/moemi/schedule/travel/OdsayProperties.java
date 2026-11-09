package com.example.moemi.schedule.travel;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for ODsay LAB public transit routing, used for the BUS travel mode.
 *
 * @param apiKey the ODsay "Server" platform key; ODsay only accepts calls from the IPs registered for it
 */
@ConfigurationProperties("moemi.odsay")
public record OdsayProperties(
        String apiKey,
        @DefaultValue("https://api.odsay.com/v1/api") String baseUrl,
        @DefaultValue("5s") Duration timeout) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
