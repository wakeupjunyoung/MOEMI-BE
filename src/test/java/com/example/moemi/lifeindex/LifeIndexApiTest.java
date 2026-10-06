package com.example.moemi.lifeindex;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.moemi.common.config.ClockConfig;
import com.example.moemi.lifeindex.kma.IndexForecast;
import com.example.moemi.lifeindex.kma.IndexForecast.HourlyValue;
import com.example.moemi.lifeindex.kma.KmaLifeIndexClient;

@SpringBootTest
@AutoConfigureMockMvc
class LifeIndexApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private KmaLifeIndexClient kmaClient;

    @Test
    void listsRegions() throws Exception {
        mvc.perform(get("/api/life-index/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(Region.values().length)))
                .andExpect(jsonPath("$[0].code").value("1233000000"))
                .andExpect(jsonPath("$[0].name").value("광주 광산구"));
    }

    @Test
    void returnsIndicesPerDate() throws Exception {
        LocalDate today = LocalDate.now(ClockConfig.KST);
        LocalDateTime issuedAt = today.atStartOfDay();
        IndexForecast uv = new IndexForecast(LifeIndexType.UV, issuedAt, issuedAt.plusHours(36),
                List.of(new HourlyValue(today.atTime(12, 0), 8)));
        given(kmaClient.fetch(eq(LifeIndexType.UV), any(), any())).willReturn(Optional.of(uv));
        given(kmaClient.fetch(eq(LifeIndexType.AIR_STAGNATION), any(), any())).willReturn(Optional.empty());

        mvc.perform(get("/api/life-index")
                        .param("regionCode", "1233000000")
                        .param("from", today.toString())
                        .param("to", today.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionName").value("광주 광산구"))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.source").value(LifeIndexService.SOURCE))
                .andExpect(jsonPath("$.days", hasSize(2)))
                .andExpect(jsonPath("$.days[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.days[0].overallLevel").value("CAUTION"))
                .andExpect(jsonPath("$.days[0].indices[0].type").value("UV"))
                .andExpect(jsonPath("$.days[0].indices[0].name").value("자외선지수"))
                .andExpect(jsonPath("$.days[0].indices[0].value").value(8))
                .andExpect(jsonPath("$.days[0].indices[0].grade").value("매우높음"))
                .andExpect(jsonPath("$.days[0].indices[0].issuedAt").value(issuedAt.toString() + ":00"))
                .andExpect(jsonPath("$.days[1].availability").value("NO_DATA"));
    }

    @Test
    void asksForRegionFirst() throws Exception {
        mvc.perform(get("/api/life-index"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REGION_REQUIRED"));

        mvc.perform(get("/api/life-index").param("regionCode", "1100000000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_REGION"));
    }

    @Test
    void unknownPathUsesTheSameErrorBody() throws Exception {
        mvc.perform(get("/api/life-index/unknown/path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
