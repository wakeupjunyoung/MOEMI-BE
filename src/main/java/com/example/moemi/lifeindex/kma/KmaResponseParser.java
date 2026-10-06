package com.example.moemi.lifeindex.kma;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.moemi.lifeindex.LifeIndexFetchException;
import com.example.moemi.lifeindex.LifeIndexType;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Parses a LivingWthrIdxServiceV5 JSON response:
 * {@code {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":[{"date":"2026100606","h0":"3",...}]}}}}}.
 */
final class KmaResponseParser {

    static final DateTimeFormatter ANNOUNCE_TIME = new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMddHH")
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .toFormatter();

    private static final String NORMAL = "00";
    private static final String NO_DATA = "03";
    private static final Pattern HOUR_FIELD = Pattern.compile("h(\\d+)");

    private KmaResponseParser() {
    }

    /**
     * @return the forecast, or empty when KMA has no data for the requested announcement
     * @throws LifeIndexFetchException when the response is an error or cannot be read
     */
    static Optional<IndexForecast> parse(LifeIndexType type, byte[] body) {
        JsonNode root = readTree(body);
        JsonNode header = root.path("response").path("header");
        String resultCode = header.path("resultCode").asString("");
        if (NO_DATA.equals(resultCode)) {
            return Optional.empty();
        }
        if (!NORMAL.equals(resultCode)) {
            throw new LifeIndexFetchException(
                    "KMA " + type.getOperation() + " resultCode=" + resultCode + " " + header.path("resultMsg").asString(""));
        }

        JsonNode items = root.path("response").path("body").path("items").path("item");
        JsonNode item = items.isArray() ? items.path(0) : items;
        if (!item.isObject()) {
            return Optional.empty();
        }

        LocalDateTime issuedAt = parseIssuedAt(type, item.path("date").asString(""));
        List<IndexForecast.HourlyValue> values = new ArrayList<>();
        int lastHour = 0;
        for (Map.Entry<String, JsonNode> field : item.properties()) {
            Matcher matcher = HOUR_FIELD.matcher(field.getKey());
            if (!matcher.matches()) {
                continue;
            }
            int hour = Integer.parseInt(matcher.group(1));
            lastHour = Math.max(lastHour, hour);
            // Hours without a forecast (e.g. UV at night) come as empty strings.
            parseValue(field.getValue().asString("")).ifPresent(value ->
                    values.add(new IndexForecast.HourlyValue(issuedAt.plusHours(hour), value)));
        }
        return Optional.of(new IndexForecast(type, issuedAt, issuedAt.plusHours(lastHour), values));
    }

    private static JsonNode readTree(byte[] body) {
        try {
            return JsonMapper.shared().readTree(body);
        } catch (JacksonException e) {
            throw new LifeIndexFetchException("KMA response is not JSON");
        }
    }

    private static LocalDateTime parseIssuedAt(LifeIndexType type, String date) {
        try {
            return LocalDateTime.parse(date, ANNOUNCE_TIME);
        } catch (DateTimeParseException e) {
            throw new LifeIndexFetchException("KMA " + type.getOperation() + " has an invalid date: " + date);
        }
    }

    private static Optional<Double> parseValue(String raw) {
        if (raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(raw.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
