package com.example.moemi.reminder;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import com.example.moemi.checklist.ChecklistRepository;
import com.example.moemi.common.web.DeviceId;
import com.example.moemi.lifeindex.DayLookup;
import com.example.moemi.lifeindex.LifeIndexService;
import com.example.moemi.lifeindex.LifeIndexStatus;
import com.example.moemi.schedule.FixedClockConfig;
import com.example.moemi.schedule.ScheduleRepository;

/** "Now" is Thursday 2026-10-29 10:00 (see {@link FixedClockConfig}). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfig.class)
class ReminderApiTest {

    private static final String DEVICE = "device-r";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ReminderSettingsRepository settingsRepository;

    @Autowired
    private ChecklistRepository checklistRepository;

    @MockitoBean
    private LifeIndexService lifeIndexService;

    @AfterEach
    void cleanUp() {
        checklistRepository.deleteAll();
        scheduleRepository.deleteAll();
        settingsRepository.deleteAll();
    }

    @Test
    void defaultIsOneHourBeforeDepartureAndPastRemindersAreSkipped() throws Exception {
        createSchedule("""
                {"title":"미팅","date":"2026-10-29","startTime":"14:00","place":"강남","prepMinutes":30,
                 "travelMinutes":40,"travelMode":"BUS"}
                """);
        // Starts in 30 minutes without travel: its one-hour-before reminder (09:30) has already passed.
        createSchedule("{\"title\":\"통화\",\"date\":\"2026-10-29\",\"startTime\":\"10:30\"}");

        upcoming()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reminders", hasSize(1)))
                // Leaves at 13:20 (40 minutes' travel), so one hour before departure is 12:20.
                .andExpect(jsonPath("$.reminders[0].fireAt").value("2026-10-29T12:20:00"))
                .andExpect(jsonPath("$.reminders[0].anchor").value("DEPARTURE"))
                .andExpect(jsonPath("$.reminders[0].minutesBefore").value(60))
                .andExpect(jsonPath("$.reminders[0].departAt").value("2026-10-29T13:20:00"))
                .andExpect(jsonPath("$.reminders[0].notificationTitle").value("14:00 미팅 · 출발 1시간 전"))
                .andExpect(jsonPath("$.reminders[0].notificationBody").value("장소: 강남 · 13:20 출발 (버스)"))
                .andExpect(jsonPath("$.reminders[0].target").value("SCHEDULE"));
    }

    @Test
    void startAnchorCountsFromTheStart() throws Exception {
        createSchedule("{\"title\":\"미팅\",\"date\":\"2026-10-29\",\"startTime\":\"14:00\",\"travelMinutes\":40,"
                + "\"reminderAnchor\":\"START\",\"reminderOffsetsMinutes\":[30]}");

        upcoming()
                .andExpect(jsonPath("$.reminders[0].fireAt").value("2026-10-29T13:30:00"))
                .andExpect(jsonPath("$.reminders[0].notificationTitle").value("14:00 미팅 · 시작 30분 전"));
    }

    @Test
    void scheduleSettingsOverrideTheDefaultAndRepeatsGetOneReminderEach() throws Exception {
        createSchedule("""
                {"title":"수영","date":"2026-10-30","startTime":"07:00","repeatType":"WEEKLY",
                 "reminderOffsetsMinutes":[10, 60, 10]}
                """);
        Number off = readId(createSchedule("{\"title\":\"쉬는 날\",\"date\":\"2026-10-30\",\"startTime\":\"09:00\"}"));
        mvc.perform(put("/api/reminders/schedules/" + off).header(DeviceId.HEADER, DEVICE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(jsonPath("$.effectiveEnabled").value(false))
                .andExpect(jsonPath("$.effectiveOffsetsMinutes", contains(60)));

        upcoming()
                .andExpect(jsonPath("$.reminders[*].id", contains(
                        "s%s-20261030-D60".formatted(scheduleIdOf("수영")),
                        "s%s-20261030-D10".formatted(scheduleIdOf("수영")),
                        "s%s-20261106-D60".formatted(scheduleIdOf("수영")),
                        "s%s-20261106-D10".formatted(scheduleIdOf("수영")))));
    }

    @Test
    void deletingAScheduleRemovesItsReminders() throws Exception {
        Number id = readId(createSchedule("{\"title\":\"미팅\",\"date\":\"2026-10-30\",\"startTime\":\"14:00\"}"));
        upcoming().andExpect(jsonPath("$.reminders", hasSize(1)));

        mvc.perform(delete("/api/schedules/" + id).header(DeviceId.HEADER, DEVICE)).andExpect(status().isNoContent());

        upcoming().andExpect(jsonPath("$.reminders", hasSize(0)));
    }

    @Test
    void defaultSettingsCanBeChangedOrTurnedOff() throws Exception {
        mvc.perform(get("/api/reminders/settings").header(DeviceId.HEADER, DEVICE))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.anchor").value("DEPARTURE"))
                .andExpect(jsonPath("$.defaultOffsetsMinutes", contains(60)));
        createSchedule("{\"title\":\"미팅\",\"date\":\"2026-10-30\",\"startTime\":\"14:00\"}");

        putSettings("{\"enabled\":true,\"defaultOffsetsMinutes\":[30, 1440]}")
                .andExpect(jsonPath("$.defaultOffsetsMinutes", contains(1440, 30)));
        upcoming().andExpect(jsonPath("$.reminders[*].minutesBefore", contains(1440, 30)));

        putSettings("{\"enabled\":false,\"defaultOffsetsMinutes\":[30]}");
        upcoming().andExpect(jsonPath("$.reminders", hasSize(0)));
    }

    @Test
    void reminderLinksToTheChecklistWhileItemsRemain() throws Exception {
        given(lifeIndexService.findDay(any(), any())).willReturn(new DayLookup(LifeIndexStatus.FAILED, null));
        Number scheduleId = readId(createSchedule("{\"title\":\"소풍\",\"date\":\"2026-10-30\",\"startTime\":\"11:00\"}"));
        String checklist = mvc.perform(post("/api/checklists").header(DeviceId.HEADER, DEVICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-30\",\"scheduleId\":" + scheduleId + ",\"regionCode\":\"1233000000\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Number checklistId = JsonPath.read(checklist, "$.id");
        mvc.perform(post("/api/checklists/" + checklistId + "/items").header(DeviceId.HEADER, DEVICE)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"돗자리\"}"));

        upcoming()
                .andExpect(jsonPath("$.reminders[0].checklist.checklistId").value(checklistId))
                .andExpect(jsonPath("$.reminders[0].checklist.remainingCount").value(1))
                .andExpect(jsonPath("$.reminders[0].target").value("CHECKLIST"))
                .andExpect(jsonPath("$.reminders[0].notificationBody").value("준비물 1개 남음"));

        mvc.perform(get("/api/schedules/timeline").header(DeviceId.HEADER, DEVICE).param("date", "2026-10-30"))
                .andExpect(jsonPath("$.schedules[0].checklist.checklistId").value(checklistId));
    }

    @Test
    void rejectsInvalidSettings() throws Exception {
        putSettings("{\"enabled\":true,\"defaultOffsetsMinutes\":[]}").andExpect(status().isBadRequest());
        putSettings("{\"enabled\":true,\"defaultOffsetsMinutes\":[1,2,3,4,5,6]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REMINDER"));
        mvc.perform(get("/api/reminders/upcoming").header(DeviceId.HEADER, DEVICE).param("days", "30"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions createSchedule(String body) throws Exception {
        return mvc.perform(post("/api/schedules").header(DeviceId.HEADER, DEVICE)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private ResultActions putSettings(String body) throws Exception {
        return mvc.perform(put("/api/reminders/settings").header(DeviceId.HEADER, DEVICE)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions upcoming() throws Exception {
        return mvc.perform(get("/api/reminders/upcoming").header(DeviceId.HEADER, DEVICE).param("days", "8"));
    }

    private Long scheduleIdOf(String title) {
        return scheduleRepository.findAll().stream().filter(s -> s.getTitle().equals(title)).findFirst().orElseThrow().getId();
    }

    private static Number readId(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
