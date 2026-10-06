package com.example.moemi.lifeindex.kma;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.example.moemi.lifeindex.LifeIndexType;

/**
 * One KMA announcement for one index: values every few hours after {@code issuedAt}.
 *
 * @param coveredUntil the last forecast hour of the announcement, even if its value was empty
 */
public record IndexForecast(
        LifeIndexType type,
        LocalDateTime issuedAt,
        LocalDateTime coveredUntil,
        List<HourlyValue> values) {

    /**
     * The worst value of each date. A date is left out unless the forecast reaches its noon,
     * so that a day known only from its early-morning hours is not reported as "낮음".
     */
    public Map<LocalDate, Double> dailyMax() {
        Map<LocalDate, Double> result = new TreeMap<>();
        for (HourlyValue hourly : values) {
            LocalDate date = hourly.time().toLocalDate();
            if (coveredUntil.isBefore(date.atTime(LocalTime.NOON))) {
                continue;
            }
            result.merge(date, hourly.value(), Math::max);
        }
        return result;
    }

    public record HourlyValue(LocalDateTime time, double value) {
    }
}
