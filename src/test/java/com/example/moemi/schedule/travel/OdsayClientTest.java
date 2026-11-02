package com.example.moemi.schedule.travel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class OdsayClientTest {

    @Test
    void readsTheFirstPathWithItsBusesInOrder() {
        String json = """
                {"result":{"searchType":0,"path":[
                  {"pathType":2,"info":{"totalTime":38,"payment":1450,"busTransitCount":2,"subwayTransitCount":0,"totalWalk":520},
                   "subPath":[
                    {"trafficType":3,"distance":200,"sectionTime":3},
                    {"trafficType":2,"sectionTime":15,"lane":[{"busNo":"송정19","type":1}]},
                    {"trafficType":3,"distance":50,"sectionTime":1},
                    {"trafficType":2,"sectionTime":12,"lane":[{"busNo":"1187","type":1},{"busNo":"1000","type":1}]},
                    {"trafficType":3,"distance":270,"sectionTime":4}]},
                  {"pathType":2,"info":{"totalTime":45,"busTransitCount":1,"totalWalk":300},"subPath":[]}]}}
                """;

        OdsayClient.BusRoute route = parse(json).orElseThrow();

        assertThat(route.totalMinutes()).isEqualTo(38);
        assertThat(route.busNumbers()).containsExactly("송정19", "1187");
        assertThat(route.summary()).isEqualTo("송정19 → 1187 (환승 1회)");
        assertThat(route.tooClose()).isFalse();
    }

    @Test
    void tooCloseErrorMeansWalk() {
        assertThat(parse("{\"error\":[{\"code\":\"-98\",\"message\":\"출, 도착지가 700m이내입니다.\"}]}"))
                .contains(OdsayClient.BusRoute.TOO_CLOSE);
    }

    @Test
    void otherErrorsThrowInEitherShape() {
        assertThatThrownBy(() -> parse("{\"error\":{\"code\":\"500\",\"msg\":\"ApiKeyAuthFailed\"}}"))
                .isInstanceOf(TravelFetchException.class)
                .hasMessageContaining("ApiKeyAuthFailed");
        assertThatThrownBy(() -> parse("{\"error\":[{\"code\":\"-99\",\"message\":\"검색결과가 없습니다.\"}]}"))
                .isInstanceOf(TravelFetchException.class);
    }

    @Test
    void noPathIsEmpty() {
        assertThat(parse("{\"result\":{\"path\":[]}}")).isEmpty();
    }

    private static Optional<OdsayClient.BusRoute> parse(String json) {
        return OdsayClient.parse(json.getBytes(StandardCharsets.UTF_8));
    }
}
