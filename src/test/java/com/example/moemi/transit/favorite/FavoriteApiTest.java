package com.example.moemi.transit.favorite;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import com.example.moemi.common.web.DeviceId;
import com.example.moemi.transit.Arrival;
import com.example.moemi.transit.City;
import com.example.moemi.transit.Route;
import com.example.moemi.transit.TransitFetchException;
import com.example.moemi.transit.tago.TagoClient;

@SpringBootTest
@AutoConfigureMockMvc
class FavoriteApiTest {

    private static final String DEVICE = "device-a";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @MockitoBean
    private TagoClient tagoClient;

    @BeforeEach
    void setUp() {
        given(tagoClient.fetchCities()).willReturn(List.of(new City("24", "광주광역시")));
    }

    @AfterEach
    void cleanUp() {
        favoriteRepository.deleteAll();
    }

    @Test
    void addListReorderAndRemove() throws Exception {
        Number home = readId(add(DEVICE, stopRoute("GJB5000001", "우리집 앞 정류장", "GJB300", "302", "우리집 앞"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("STOP_ROUTE"))
                .andExpect(jsonPath("$.cityCode").value("24"))
                .andExpect(jsonPath("$.direction").value("광주역 방면"))
                .andExpect(jsonPath("$.alias").value("우리집 앞"))
                .andExpect(jsonPath("$.sortOrder").value(0)));
        Number route = readId(add(DEVICE, """
                {"type":"ROUTE","routeId":"GJB400","routeNo":"첨단09","nodeId":"IGNORED"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeId").value(nullValue()))
                .andExpect(jsonPath("$.sortOrder").value(1)));

        mvc.perform(put("/api/transit/favorites/order").header(DeviceId.HEADER, DEVICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"favoriteIds\":[" + route + "," + home + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].routeNo", contains("첨단09", "302")));

        mvc.perform(delete("/api/transit/favorites/" + route).header(DeviceId.HEADER, DEVICE))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/transit/favorites").header(DeviceId.HEADER, DEVICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(home));
    }

    @Test
    void rejectsDuplicatesAndIncompleteFavorites() throws Exception {
        String body = stopRoute("GJB5000001", "광주역", "GJB300", "302", null);
        add(DEVICE, body).andExpect(status().isOk());

        add(DEVICE, body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FAVORITE_DUPLICATE"));
        add(DEVICE, "{\"type\":\"STOP\",\"nodeId\":\"GJB5000002\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(get("/api/transit/favorites"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DEVICE_ID_REQUIRED"));
    }

    @Test
    void reorderNeedsEveryFavoriteOnce() throws Exception {
        Number id = readId(add(DEVICE, stopRoute("GJB5000001", "광주역", "GJB300", "302", null)));
        add(DEVICE, stopRoute("GJB5000002", "광주역", "GJB300", "302", null));

        mvc.perform(put("/api/transit/favorites/order").header(DeviceId.HEADER, DEVICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"favoriteIds\":[" + id + "," + id + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FAVORITE_ORDER"));
    }

    @Test
    void anotherDevicesFavoriteIsNotFound() throws Exception {
        Number id = readId(add(DEVICE, stopRoute("GJB5000001", "광주역", "GJB300", "302", null)));

        mvc.perform(delete("/api/transit/favorites/" + id).header(DeviceId.HEADER, "device-b"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FAVORITE_NOT_FOUND"));
    }

    @Test
    void realtimeKeepsEveryFavoriteWhateverTagoAnswers() throws Exception {
        given(tagoClient.fetchRoute("24", "RT300")).willReturn(Optional.of(
                new Route("24", "RT300", "302", "간선버스", "송정역", "광주역", "05:30", "22:30", 12)));
        given(tagoClient.fetchArrivals("24", "RT-STOP-1")).willReturn(List.of(
                new Arrival("RT300", "302", "간선버스", "일반차량", 2, 185),
                new Arrival("RT999", "999", "지선버스", "일반차량", 1, 30)));
        given(tagoClient.fetchRoute("24", "RT-GONE")).willReturn(Optional.empty());
        given(tagoClient.fetchArrivals("24", "RT-STOP-2")).willThrow(new TransitFetchException("TAGO responded 500"));

        add(DEVICE, stopRoute("RT-STOP-1", "우리집 앞", "RT300", "302", null));
        add(DEVICE, "{\"type\":\"ROUTE\",\"routeId\":\"RT-GONE\",\"routeNo\":\"777\"}");
        add(DEVICE, "{\"type\":\"STOP\",\"nodeId\":\"RT-STOP-2\",\"nodeName\":\"학교 앞\"}");

        mvc.perform(get("/api/transit/favorites/realtime").header(DeviceId.HEADER, DEVICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].favorite.nodeName").value("우리집 앞"))
                .andExpect(jsonPath("$[0].realtime.status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].realtime.fetchedAt").exists())
                .andExpect(jsonPath("$[0].realtime.arrivals", hasSize(1)))
                .andExpect(jsonPath("$[0].realtime.arrivals[0].arrivalMinutes").value(3))
                .andExpect(jsonPath("$[0].realtime.arrivals[0].expectedAt").exists())
                .andExpect(jsonPath("$[1].favorite.routeNo").value("777"))
                .andExpect(jsonPath("$[1].realtime.status").value("ROUTE_NOT_FOUND"))
                .andExpect(jsonPath("$[2].favorite.nodeName").value("학교 앞"))
                .andExpect(jsonPath("$[2].realtime.status").value("FAILED"));
    }

    private ResultActions add(String device, String body) throws Exception {
        return mvc.perform(post("/api/transit/favorites").header(DeviceId.HEADER, device)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String stopRoute(String nodeId, String nodeName, String routeId, String routeNo, String alias) {
        return """
                {"type":"STOP_ROUTE","nodeId":"%s","nodeName":"%s","nodeNo":"5012",
                 "routeId":"%s","routeNo":"%s","routeType":"간선버스","direction":"광주역 방면",%s}
                """.formatted(nodeId, nodeName, routeId, routeNo, alias == null ? "\"alias\":null" : "\"alias\":\"" + alias + "\"");
    }

    private static Number readId(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
