package com.example.moemi.hazard;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.moemi.hazard.its.ItsEvent;
import com.example.moemi.hazard.its.ItsEventClient;

@SpringBootTest
@AutoConfigureMockMvc
class HazardApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ItsEventClient itsClient;

    @Test
    void returnsHazardsFilteredByType() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        given(itsClient.fetch(any())).willReturn(List.of(
                new ItsEvent("국도", "공사", "도로포장", start, null, new GeoPoint(35.191, 126.82), "1",
                        "국도22호선", "22", "화순방향", "전면통제", null, "정비공사"),
                new ItsEvent("국도", "교통사고", "충돌사고", start, null, new GeoPoint(35.192, 126.82), "2",
                        null, "1", "목포방향", null, "1 차로", "추돌 사고")));

        mvc.perform(get("/api/hazards")
                        .param("lat", "35.19").param("lng", "126.82")
                        .param("radius", "500").param("type", "CONSTRUCTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.source").value(HazardService.SOURCE))
                .andExpect(jsonPath("$.radiusM").value(500))
                .andExpect(jsonPath("$.hazards", hasSize(1)))
                .andExpect(jsonPath("$.hazards[0].type").value("CONSTRUCTION"))
                .andExpect(jsonPath("$.hazards[0].state").value("ACTIVE"))
                .andExpect(jsonPath("$.hazards[0].title").value("국도22호선 도로포장"))
                .andExpect(jsonPath("$.hazards[0].description").value("정비공사"))
                .andExpect(jsonPath("$.hazards[0].blockType").value("전면통제"))
                .andExpect(jsonPath("$.hazards[0].lat").value(35.191));
    }

    @Test
    void rejectsOutOfRangeParameters() throws Exception {
        mvc.perform(get("/api/hazards").param("lat", "95").param("lng", "126.82"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mvc.perform(get("/api/hazards").param("lat", "35.19").param("lng", "126.82").param("type", "FLOOD"))
                .andExpect(status().isBadRequest());
    }
}
