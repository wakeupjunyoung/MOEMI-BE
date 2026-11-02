package com.example.moemi.schedule;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.example.moemi.common.config.ClockConfig;

/**
 * Pins "now" to Thursday 2026-10-29 10:00 KST so relative dates and reminder times are predictable.
 */
@TestConfiguration
public class FixedClockConfig {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 10, 0);

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW.atZone(ClockConfig.KST).toInstant(), ClockConfig.KST);
    }
}
