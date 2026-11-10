package com.example.moemi.common.config;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void apiDocsShowDeviceIdAsHeaderOnlyWhereNeeded() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("MOEMI API"))
                .andExpect(jsonPath("$.paths['/api/checklists'].get.parameters[*].name", hasItem("X-Device-Id")))
                .andExpect(jsonPath("$.paths['/api/checklists'].get.parameters[*].in", hasItem("header")))
                .andExpect(jsonPath("$.paths['/api/checklists'].get.parameters[*].name", not(hasItem("deviceId"))))
                .andExpect(jsonPath("$.paths['/api/life-index/regions'].get.parameters").doesNotExist());
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
