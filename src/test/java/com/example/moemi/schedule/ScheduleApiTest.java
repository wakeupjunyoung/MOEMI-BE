package com.example.moemi.schedule;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import com.example.moemi.common.web.DeviceId;
import com.example.moemi.schedule.travel.TravelPreferenceRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfig.class)
class ScheduleApiTest {

    private static final String DEVICE = "device-a";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private TravelPreferenceRepository travelPreferenceRepository;

    @AfterEach
    void cleanUp() {
        scheduleRepository.deleteAll();
        travelPreferenceRepository.deleteAll();
    }

    @Test
    void createReadUpdateAndDelete() throws Exception {
        Number id = readId(create(DEVICE, """
                {"title":"강남 미팅","date":"2026-10-30","startTime":"14:00","endTime":"15:00","place":"강남",
                 "memo":"자료 챙기기","prepMinutes":30,"travelMinutes":40}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("강남 미팅"))
                .andExpect(jsonPath("$.repeatType").value("NONE"))
                .andExpect(jsonPath("$.conflicts", hasSize(0))));

        mvc.perform(put("/api/schedules/" + id).header(DeviceId.HEADER, DEVICE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"강남 미팅(변경)\",\"date\":\"2026-10-30\",\"startTime\":\"15:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("강남 미팅(변경)"))
                .andExpect(jsonPath("$.place").value(nullValue()));

        mvc.perform(get("/api/schedules/" + id).header(DeviceId.HEADER, "device-b"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCHEDULE_NOT_FOUND"));

        mvc.perform(delete("/api/schedules/" + id).header(DeviceId.HEADER, DEVICE)).andExpect(status().isNoContent());
        mvc.perform(get("/api/schedules/" + id).header(DeviceId.HEADER, DEVICE)).andExpect(status().isNotFound());
    }

    @Test
    void conflictingScheduleNeedsConfirmationThenIsSavedWithAWarning() throws Exception {
        create(DEVICE, "{\"title\":\"미팅\",\"date\":\"2026-10-30\",\"startTime\":\"14:00\",\"endTime\":\"15:00\"}");
        String overlapping = "{\"title\":\"통화\",\"date\":\"2026-10-30\",\"startTime\":\"14:30\"%s}";

        mvc.perform(post("/api/schedules/check").header(DeviceId.HEADER, DEVICE).contentType(MediaType.APPLICATION_JSON)
                        .content(overlapping.formatted("")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("OVERLAP"));

        create(DEVICE, overlapping.formatted(""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("미팅")));

        create(DEVICE, overlapping.formatted(",\"overlapConfirmed\":true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflicts[0].type").value("OVERLAP"))
                .andExpect(jsonPath("$.conflicts[0].earlierTitle").value("미팅"));
    }

    @Test
    void timelineShowsPreparationTravelAndScheduleBlocks() throws Exception {
        create(DEVICE, """
                {"title":"미팅","date":"2026-10-30","startTime":"14:00","endTime":"15:00","place":"강남",
                 "prepMinutes":30,"travelMinutes":40,"travelMode":"BUS"}
                """);
        create(DEVICE, "{\"title\":\"아침 운동\",\"date\":\"2026-10-30\",\"startTime\":\"07:00\",\"endTime\":\"08:00\"}");

        mvc.perform(get("/api/schedules/timeline").header(DeviceId.HEADER, DEVICE).param("date", "2026-10-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules[*].title", contains("아침 운동", "미팅")))
                .andExpect(jsonPath("$.schedules[1].departAt").value("2026-10-30T13:20:00"))
                .andExpect(jsonPath("$.schedules[1].prepStartAt").value("2026-10-30T12:50:00"))
                .andExpect(jsonPath("$.blocks[*].type", contains("SCHEDULE", "PREP", "TRAVEL", "SCHEDULE")))
                .andExpect(jsonPath("$.blocks[2].label").value("버스 · 강남(으)로 이동"))
                .andExpect(jsonPath("$.schedules[1].travelMode").value("BUS"))
                .andExpect(jsonPath("$.conflicts", hasSize(0)));
    }

    @Test
    void weeklyScheduleAppearsOnEachWeekInTheCalendar() throws Exception {
        create(DEVICE, """
                {"title":"수영","date":"2026-10-29","startTime":"07:00","repeatType":"WEEKLY","repeatUntil":"2026-11-12"}
                """);

        mvc.perform(get("/api/schedules").header(DeviceId.HEADER, DEVICE)
                        .param("from", "2026-10-26").param("to", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].date", contains("2026-10-29", "2026-11-05", "2026-11-12")));
    }

    @Test
    void rejectsInvalidSchedules() throws Exception {
        create(DEVICE, "{\"title\":\" \",\"date\":\"2026-10-30\",\"startTime\":\"14:00\"}")
                .andExpect(status().isBadRequest());
        create(DEVICE, "{\"title\":\"미팅\",\"date\":\"2026-10-30\",\"startTime\":\"14:00\",\"endTime\":\"13:00\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME"));
        create(DEVICE, "{\"title\":\"미팅\",\"date\":\"2026-10-30\",\"startTime\":\"14:00\",\"reminderOffsetsMinutes\":[-5]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REMINDER"));
    }

    @Test
    void parseSuggestsACandidateWithoutSaving() throws Exception {
        mvc.perform(post("/api/schedules/parse").header(DeviceId.HEADER, DEVICE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"내일 오후 2시에 강남에서 미팅\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("미팅"))
                .andExpect(jsonPath("$.date").value("2026-10-30"))
                .andExpect(jsonPath("$.startTime").value("14:00:00"))
                .andExpect(jsonPath("$.place").value("강남"))
                .andExpect(jsonPath("$.suggestedPrepMinutes").value(30))
                .andExpect(jsonPath("$.travel.status").value("NO_DESTINATION"))
                .andExpect(jsonPath("$.missingFields", hasSize(0)));

        mvc.perform(get("/api/schedules").header(DeviceId.HEADER, DEVICE)
                        .param("from", "2026-10-30").param("to", "2026-10-30"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void nearbyPlaceIsWalkedWithoutAnyKey() throws Exception {
        travelTime("{\"originLat\":35.1650,\"originLng\":126.9090,\"destLat\":35.1677,\"destLng\":126.9090,\"mode\":\"BUS\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.mode").value("WALK"))
                .andExpect(jsonPath("$.forcedWalk").value(true));
    }

    @Test
    void farBusWithoutOdsayKeyIsUnavailableButGivesWalkingTime() throws Exception {
        travelTime("{\"originLat\":35.1650,\"originLng\":126.9090,\"destLat\":35.2100,\"destLng\":126.9090}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.requestedMode").value("BUS"))
                .andExpect(jsonPath("$.walkMinutes").isNumber());
    }

    @Test
    void travelSettingsCanBeChanged() throws Exception {
        mvc.perform(get("/api/schedules/travel-settings").header(DeviceId.HEADER, DEVICE))
                .andExpect(jsonPath("$.defaultMode").value("BUS"))
                .andExpect(jsonPath("$.roundUpMinutes").value(0))
                .andExpect(jsonPath("$.alwaysWalkMinutes").value(10));

        mvc.perform(put("/api/schedules/travel-settings").header(DeviceId.HEADER, DEVICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaultMode\":\"WALK\",\"bufferMinutes\":5,\"roundUpMinutes\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultMode").value("WALK"))
                .andExpect(jsonPath("$.bufferMinutes").value(5));

        mvc.perform(put("/api/schedules/travel-settings").header(DeviceId.HEADER, DEVICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaultMode\":\"WALK\",\"roundUpMinutes\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRAVEL_SETTING"));
    }

    private ResultActions travelTime(String body) throws Exception {
        return mvc.perform(post("/api/schedules/travel-time").header(DeviceId.HEADER, DEVICE)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions create(String device, String body) throws Exception {
        return mvc.perform(post("/api/schedules").header(DeviceId.HEADER, device)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static Number readId(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
