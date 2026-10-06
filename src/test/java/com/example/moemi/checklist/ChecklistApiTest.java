package com.example.moemi.checklist;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
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
import com.example.moemi.lifeindex.DailyLifeIndex;
import com.example.moemi.lifeindex.DayLookup;
import com.example.moemi.lifeindex.IndexValue;
import com.example.moemi.lifeindex.LifeIndexService;
import com.example.moemi.lifeindex.LifeIndexStatus;
import com.example.moemi.lifeindex.LifeIndexType;

@SpringBootTest
@AutoConfigureMockMvc
class ChecklistApiTest {

    private static final String DEVICE = "device-a";
    private static final LocalDate DATE = LocalDate.of(2026, 10, 6);
    private static final String REGION = "1233000000";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ChecklistRepository checklistRepository;

    @MockitoBean
    private LifeIndexService lifeIndexService;

    @AfterEach
    void cleanUp() {
        checklistRepository.deleteAll();
    }

    @Test
    void openCreatesChecklistWithRecommendationsOnceAndReturnsItAgain() throws Exception {
        givenHighUv();

        Number id = readId(open(DEVICE, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationStatus").value("OK"))
                .andExpect(jsonPath("$.items[*].name", contains("선크림", "모자", "선글라스")))
                .andExpect(jsonPath("$.items[0].source").value("RECOMMENDED"))
                .andExpect(jsonPath("$.items[0].reason").value("자외선 높음"))
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.remainingCount").value(3)));

        open(DEVICE, null).andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void scheduleChecklistsAreSeparateAndListedByDate() throws Exception {
        givenHighUv();
        open(DEVICE, null);
        open(DEVICE, 7L).andExpect(jsonPath("$.scheduleId").value(7));
        open("device-b", null);

        mvc.perform(get("/api/checklists").header(DeviceId.HEADER, DEVICE).param("date", DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].scheduleId").value(7));
    }

    @Test
    void itemsCanBeAddedCheckedRenamedAndRemoved() throws Exception {
        givenHighUv();
        Number id = readId(open(DEVICE, null));
        String items = "/api/checklists/" + id + "/items";

        String added = mvc.perform(post(items).header(DeviceId.HEADER, DEVICE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"교통카드\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[3].name").value("교통카드"))
                .andExpect(jsonPath("$.items[3].source").value("CUSTOM"))
                .andReturn().getResponse().getContentAsString();
        Number cardId = JsonPath.read(added, "$.items[3].id");
        Number sunscreenId = JsonPath.read(added, "$.items[0].id");

        mvc.perform(patch(items + "/" + cardId).header(DeviceId.HEADER, DEVICE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"checked\":true}"))
                .andExpect(jsonPath("$.checkedCount").value(1))
                .andExpect(jsonPath("$.remainingCount").value(3));

        mvc.perform(patch(items + "/" + sunscreenId).header(DeviceId.HEADER, DEVICE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"선스틱\"}"))
                .andExpect(jsonPath("$.items[0].name").value("선스틱"))
                .andExpect(jsonPath("$.items[0].source").value("CUSTOM"));

        mvc.perform(delete(items + "/" + cardId).header(DeviceId.HEADER, DEVICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3));

        // The renamed item was the user's choice, so a refresh does not bring "선크림" back.
        mvc.perform(post("/api/checklists/" + id + "/recommendations").header(DeviceId.HEADER, DEVICE))
                .andExpect(jsonPath("$.items[*].name", contains("선스틱", "모자", "선글라스")));
    }

    @Test
    void failedIndexLookupStillCreatesAnEmptyChecklist() throws Exception {
        given(lifeIndexService.findDay(any(), any())).willReturn(new DayLookup(LifeIndexStatus.FAILED, null));

        open(DEVICE, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationStatus").value("FAILED"))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void otherDevicesCannotSeeTheChecklist() throws Exception {
        givenHighUv();
        Number id = readId(open(DEVICE, null));

        mvc.perform(get("/api/checklists/" + id).header(DeviceId.HEADER, "device-b"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHECKLIST_NOT_FOUND"));
    }

    @Test
    void rejectsMissingDeviceRegionAndBlankName() throws Exception {
        mvc.perform(post("/api/checklists").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-06\",\"regionCode\":\"" + REGION + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DEVICE_ID_REQUIRED"));

        mvc.perform(post("/api/checklists").header(DeviceId.HEADER, DEVICE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-06\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REGION_REQUIRED"));

        givenHighUv();
        Number id = readId(open(DEVICE, null));
        mvc.perform(post("/api/checklists/" + id + "/items").header(DeviceId.HEADER, DEVICE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private void givenHighUv() {
        DailyLifeIndex day = new DailyLifeIndex(DATE, List.of(IndexValue.of(LifeIndexType.UV, 7, DATE.atTime(6, 0))));
        given(lifeIndexService.findDay(any(), any())).willReturn(new DayLookup(LifeIndexStatus.OK, day));
    }

    private ResultActions open(String device, Long scheduleId) throws Exception {
        String body = "{\"date\":\"" + DATE + "\",\"regionCode\":\"" + REGION + "\""
                + (scheduleId == null ? "" : ",\"scheduleId\":" + scheduleId) + "}";
        return mvc.perform(post("/api/checklists").header(DeviceId.HEADER, device)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static Number readId(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
