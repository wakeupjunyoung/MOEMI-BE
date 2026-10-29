package com.example.moemi.transit;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.moemi.transit.tago.TagoClient;

@SpringBootTest
@AutoConfigureMockMvc
class TransitApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TagoClient tagoClient;

    @Test
    void stopSearchMarksStopsSharingAName() throws Exception {
        given(tagoClient.searchStops("24", "광주역", null)).willReturn(List.of(
                new Stop("24", "GJB1", "광주역", "5012", 35.165, 126.909),
                new Stop("24", "GJB2", "광주역", "5013", 35.166, 126.910),
                new Stop("24", "GJB3", "광주역후문", "5100", 35.167, 126.911)));

        mvc.perform(get("/api/transit/stops").param("keyword", "광주역"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.cityCode").value("24"))
                .andExpect(jsonPath("$.source").value(TransitService.SOURCE))
                .andExpect(jsonPath("$.stops", hasSize(3)))
                .andExpect(jsonPath("$.stops[0].sameNameExists").value(true))
                .andExpect(jsonPath("$.stops[0].number").value("5012"))
                .andExpect(jsonPath("$.stops[2].sameNameExists").value(false));
    }

    @Test
    void arrivalsOfUnsupportedCityAreReportedInTheBody() throws Exception {
        given(tagoClient.fetchCities()).willReturn(List.of(new City("24", "광주광역시")));

        mvc.perform(get("/api/transit/stops/ABC123/arrivals").param("cityCode", "39"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_SUPPORTED"))
                .andExpect(jsonPath("$.arrivals", hasSize(0)));
    }

    @Test
    void rejectsInvalidParameters() throws Exception {
        mvc.perform(get("/api/transit/stops").param("keyword", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mvc.perform(get("/api/transit/routes").param("keyword", "302").param("cityCode", "광주"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/transit/routes/bad%20id"))
                .andExpect(status().isBadRequest());
    }
}
