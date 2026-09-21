package com.busping.device;

import com.busping.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceRegisterIntegrationTest extends IntegrationTestSupport {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("디바이스 등록은 무인증으로 201과 UUID를 반환한다 - Flyway 스키마 위에서 실제 저장까지")
    void registerReturnsUuid() throws Exception {
        mockMvc.perform(post("/api/devices"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.deviceId").isNotEmpty());
    }
}
