package com.example.moemi.transit.tago;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.moemi.transit.TransitFetchException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the envelope shared by every TAGO operation:
 * {@code {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":[...]},"totalCount":2}}}}.
 * TAGO sends {@code "items": ""} when there is no result and a single object instead of an array
 * when there is exactly one.
 */
final class TagoResponseParser {

    private static final Set<String> NORMAL = Set.of("00", "0");
    private static final String NO_DATA = "03";
    /** Most specific first: returnAuthMsg names the cause, errMsg is just "SERVICE ERROR". */
    private static final List<Pattern> XML_MESSAGES = List.of(
            Pattern.compile("<returnAuthMsg>([^<]*)</returnAuthMsg>"),
            Pattern.compile("<resultMsg>([^<]*)</resultMsg>"),
            Pattern.compile("<errMsg>([^<]*)</errMsg>"));

    private TagoResponseParser() {
    }

    /**
     * @return the items, empty when TAGO has no result
     * @throws TransitFetchException when the response is an error or cannot be read
     */
    static List<JsonNode> items(String operation, byte[] body) {
        JsonNode root = readTree(operation, body);
        JsonNode gatewayError = root.path("OpenAPI_ServiceResponse");
        if (!gatewayError.isMissingNode()) {
            // data.go.kr answers key and quota errors with this envelope instead of "response".
            JsonNode header = gatewayError.path("cmmMsgHeader");
            throw new TransitFetchException("TAGO " + operation + " gateway error "
                    + header.path("returnAuthMsg").asString(header.path("errMsg").asString("")));
        }

        JsonNode header = root.path("response").path("header");
        String resultCode = header.path("resultCode").asString("");
        if (NO_DATA.equals(resultCode)) {
            return List.of();
        }
        if (!NORMAL.contains(resultCode)) {
            throw new TransitFetchException(
                    "TAGO " + operation + " resultCode=" + resultCode + " " + header.path("resultMsg").asString(""));
        }

        JsonNode item = root.path("response").path("body").path("items").path("item");
        if (item.isObject()) {
            return List.of(item);
        }
        List<JsonNode> items = new ArrayList<>();
        if (item.isArray()) {
            item.forEach(items::add);
        }
        return items;
    }

    /** A field as trimmed text (numbers included), or {@code null} when it is missing or blank. */
    static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asString("").trim();
        return text.isEmpty() ? null : text;
    }

    static Integer integer(JsonNode node, String field) {
        Double value = decimal(node, field);
        return value == null ? null : (int) Math.round(value);
    }

    static Double decimal(JsonNode node, String field) {
        String text = text(node, field);
        if (text == null) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static JsonNode readTree(String operation, byte[] body) {
        String head = new String(body, 0, Math.min(body.length, 64), StandardCharsets.UTF_8).stripLeading();
        if (head.startsWith("<")) {
            // Gateway errors (unregistered key, quota) come as XML even with _type=json.
            String xml = new String(body, StandardCharsets.UTF_8);
            String message = XML_MESSAGES.stream()
                    .map(pattern -> pattern.matcher(xml))
                    .filter(Matcher::find)
                    .map(matcher -> matcher.group(1).trim())
                    .findFirst()
                    .orElse("unknown");
            throw new TransitFetchException("TAGO " + operation + " returned XML: " + message);
        }
        try {
            return JsonMapper.shared().readTree(body);
        } catch (JacksonException e) {
            throw new TransitFetchException("TAGO " + operation + " response is not JSON");
        }
    }
}
