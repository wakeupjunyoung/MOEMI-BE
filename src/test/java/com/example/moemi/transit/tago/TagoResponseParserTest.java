package com.example.moemi.transit.tago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.moemi.transit.Arrival;
import com.example.moemi.transit.Route;
import com.example.moemi.transit.TransitFetchException;

import tools.jackson.databind.JsonNode;

class TagoResponseParserTest {

    @Test
    void parsesItemArray() {
        String json = """
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},
                 "body":{"items":{"item":[
                  {"arrprevstationcnt":3,"arrtime":245,"nodeid":"GJB5000001","nodenm":"광주역","routeid":"GJB300","routeno":302,"routetp":"간선버스","vehicletp":"일반차량"},
                  {"arrprevstationcnt":"7","arrtime":"610","nodeid":"GJB5000001","nodenm":"광주역","routeid":"GJB400","routeno":"첨단09","routetp":"지선버스","vehicletp":"저상버스"}
                 ]},"numOfRows":10,"pageNo":1,"totalCount":2}}}
                """;

        List<Arrival> arrivals = items(json).stream().map(TagoMapper::arrival).toList();

        // Numbers come as JSON numbers in one item and as strings in the other.
        assertThat(arrivals).containsExactly(
                new Arrival("GJB300", "302", "간선버스", "일반차량", 3, 245),
                new Arrival("GJB400", "첨단09", "지선버스", "저상버스", 7, 610));
    }

    @Test
    void singleItemObjectIsAccepted() {
        String json = """
                {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":{"citycode":24,"cityname":"광주광역시"}},"totalCount":1}}}
                """;

        assertThat(items(json)).singleElement().extracting(TagoMapper::city)
                .satisfies(city -> {
                    assertThat(city.code()).isEqualTo("24");
                    assertThat(city.name()).isEqualTo("광주광역시");
                });
    }

    @Test
    void emptyItemsStringIsNoResult() {
        String json = """
                {"response":{"header":{"resultCode":"00"},"body":{"items":"","numOfRows":10,"pageNo":1,"totalCount":0}}}
                """;

        assertThat(items(json)).isEmpty();
    }

    @Test
    void errorCodeThrows() {
        String json = """
                {"response":{"header":{"resultCode":"10","resultMsg":"INVALID_REQUEST_PARAMETER_ERROR"}}}
                """;

        assertThatThrownBy(() -> items(json))
                .isInstanceOf(TransitFetchException.class)
                .hasMessageContaining("resultCode=10");
    }

    @Test
    void gatewayErrorEnvelopeThrows() {
        String json = """
                {"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"SERVICE ERROR","returnAuthMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR","returnReasonCode":"30"}}}
                """;

        assertThatThrownBy(() -> items(json))
                .isInstanceOf(TransitFetchException.class)
                .hasMessageContaining("SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
    }

    @Test
    void xmlGatewayErrorThrowsWithItsMessage() {
        String xml = """
                <OpenAPI_ServiceResponse><cmmMsgHeader><errMsg>SERVICE ERROR</errMsg>
                <returnAuthMsg>LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR</returnAuthMsg>
                <returnReasonCode>22</returnReasonCode></cmmMsgHeader></OpenAPI_ServiceResponse>
                """;

        assertThatThrownBy(() -> items(xml))
                .isInstanceOf(TransitFetchException.class)
                .hasMessageContaining("LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR");
    }

    @Test
    void mapsRouteWithServiceHours() {
        String json = """
                {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":
                 {"routeid":"GJB300","routeno":302,"routetp":"간선버스","startnodenm":"송정역","endnodenm":"광주역",
                  "startvehicletime":"0530","endvehicletime":2230,"intervaltime":12}}}}}
                """;

        Route route = TagoMapper.route("24", items(json).get(0));

        assertThat(route).isEqualTo(new Route("24", "GJB300", "302", "간선버스", "송정역", "광주역", "05:30", "22:30", 12));
    }

    @Test
    void formatsVehicleTimes() {
        assertThat(TagoMapper.time("530")).isEqualTo("05:30");
        assertThat(TagoMapper.time("2305")).isEqualTo("23:05");
        assertThat(TagoMapper.time("05:30")).isEqualTo("05:30");
        assertThat(TagoMapper.time(null)).isNull();
    }

    private static List<JsonNode> items(String body) {
        return TagoResponseParser.items("test", body.getBytes(StandardCharsets.UTF_8));
    }
}
