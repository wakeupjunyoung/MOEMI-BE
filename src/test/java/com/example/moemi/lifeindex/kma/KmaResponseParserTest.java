package com.example.moemi.lifeindex.kma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.moemi.lifeindex.LifeIndexFetchException;
import com.example.moemi.lifeindex.LifeIndexType;

class KmaResponseParserTest {

    @Test
    void parsesHourlyValuesAndSkipsEmptyOnes() {
        String json = """
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
                 "body":{"dataType":"JSON","items":{"item":[{"code":"A07_2","areaNo":"1233000000","date":"2026100606",
                  "h0":"2","h3":"6","h6":"5","h9":"","h12":"","h24":"4","h27":"8","h33":""}]},
                  "pageNo":1,"numOfRows":10,"totalCount":1}}}
                """;

        IndexForecast forecast = parse(LifeIndexType.UV, json).orElseThrow();

        assertThat(forecast.issuedAt()).isEqualTo(LocalDateTime.of(2026, 10, 6, 6, 0));
        assertThat(forecast.coveredUntil()).isEqualTo(LocalDateTime.of(2026, 10, 7, 15, 0));
        assertThat(forecast.values()).hasSize(5);
        assertThat(forecast.dailyMax())
                .containsEntry(LocalDate.of(2026, 10, 6), 6.0)
                .containsEntry(LocalDate.of(2026, 10, 7), 8.0);
    }

    @Test
    void singleItemObjectIsAccepted() {
        String json = """
                {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":{"date":"2026100603","h3":"50"}}}}}
                """;

        assertThat(parse(LifeIndexType.AIR_STAGNATION, json)).isPresent();
    }

    @Test
    void dayNotCoveredUntilNoonIsLeftOut() {
        // Issued at 06:00, last slot h75 = day 3 at 09:00: day 3 is only known for the morning.
        String json = """
                {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":[{"date":"2026100606",
                 "h0":"1","h72":"0","h75":"1"}]}}}}
                """;

        IndexForecast forecast = parse(LifeIndexType.UV, json).orElseThrow();

        assertThat(forecast.dailyMax()).containsOnlyKeys(LocalDate.of(2026, 10, 6));
    }

    @Test
    void noDataIsEmpty() {
        String json = """
                {"response":{"header":{"resultCode":"03","resultMsg":"NO_DATA"}}}
                """;

        assertThat(parse(LifeIndexType.UV, json)).isEmpty();
    }

    @Test
    void errorCodeThrows() {
        String json = """
                {"response":{"header":{"resultCode":"12","resultMsg":"NO_OPENAPI_SERVICE_ERROR"}}}
                """;

        assertThatThrownBy(() -> parse(LifeIndexType.UV, json))
                .isInstanceOf(LifeIndexFetchException.class)
                .hasMessageContaining("resultCode=12");
    }

    @Test
    void gatewayErrorEnvelopeThrows() {
        // data.go.kr answers an unregistered key with this envelope instead of "response".
        String json = """
                {"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR","returnReasonCode":"30"}}}
                """;

        assertThatThrownBy(() -> parse(LifeIndexType.UV, json)).isInstanceOf(LifeIndexFetchException.class);
    }

    @Test
    void nonJsonThrows() {
        assertThatThrownBy(() -> parse(LifeIndexType.UV, "<OpenAPI_ServiceResponse/>"))
                .isInstanceOf(LifeIndexFetchException.class);
    }

    private static Optional<IndexForecast> parse(LifeIndexType type, String json) {
        return KmaResponseParser.parse(type, json.getBytes(StandardCharsets.UTF_8));
    }
}
