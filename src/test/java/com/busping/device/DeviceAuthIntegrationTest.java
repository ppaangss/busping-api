package com.busping.device;

import com.busping.favorite.domain.FavoriteFolderRepository;
import com.busping.favorite.domain.FavoriteRepository;
import com.busping.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceAuthIntegrationTest extends IntegrationTestSupport {

    private static final String HEADER = "X-Device-Id";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    FavoriteFolderRepository favoriteFolderRepository;
    @Autowired
    FavoriteRepository favoriteRepository;

    @Test
    @DisplayName("헤더 없이 보호 API에 접근하면 401이다")
    void missingHeaderIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/favorites/folders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("등록되지 않은 디바이스입니다."));
    }

    @Test
    @DisplayName("UUID 형식이 아닌 헤더는 401이다")
    void malformedUuidIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/favorites/folders").header(HEADER, "not-a-uuid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("형식은 맞지만 등록되지 않은 UUID는 401이다")
    void unregisteredUuidIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/favorites/folders").header(HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("등록된 디바이스는 보호 API에 접근할 수 있다")
    void registeredDeviceIsAuthorized() throws Exception {
        String deviceId = registerDevice();

        mockMvc.perform(get("/api/favorites/folders").header(HEADER, deviceId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("디바이스를 삭제하면 폴더·즐겨찾기가 함께 지워지고 이후 접근은 401이다")
    void deleteCascadesAndInvalidatesDevice() throws Exception {
        // given - 디바이스에 폴더 1개, 노선 1개
        String deviceId = registerDevice();
        String folderId = createFolder(deviceId);
        addRoute(deviceId, folderId);
        assertThat(favoriteFolderRepository.findAllByDevice_IdOrderByIdAsc(UUID.fromString(deviceId))).hasSize(1);
        assertThat(favoriteRepository.findAllByFolder_Device_Id(UUID.fromString(deviceId))).hasSize(1);

        // when - 디바이스 삭제
        mockMvc.perform(delete("/api/devices/me").header(HEADER, deviceId))
                .andExpect(status().isOk());

        // then - DB 레벨 cascade로 전부 삭제 + 재접근 401
        assertThat(favoriteFolderRepository.findAllByDevice_IdOrderByIdAsc(UUID.fromString(deviceId))).isEmpty();
        assertThat(favoriteRepository.findAllByFolder_Device_Id(UUID.fromString(deviceId))).isEmpty();
        mockMvc.perform(get("/api/favorites/folders").header(HEADER, deviceId))
                .andExpect(status().isUnauthorized());
    }

    // ===== API 헬퍼 =====

    private String registerDevice() throws Exception {
        String body = mockMvc.perform(post("/api/devices"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.deviceId");
    }

    private String createFolder(String deviceId) throws Exception {
        String body = mockMvc.perform(post("/api/favorites/folders")
                        .header(HEADER, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"출근길\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return String.valueOf((int) JsonPath.read(body, "$.data.id"));
    }

    private void addRoute(String deviceId, String folderId) throws Exception {
        mockMvc.perform(post("/api/favorites/" + folderId + "/routes")
                        .header(HEADER, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stationId":"ST1","stationName":"시청앞","regionCode":"23",
                                 "latitude":37.5665,"longitude":126.9780,
                                 "routeId":"FAKE-ROUTE","routeName":"77"}
                                """))
                .andExpect(status().isCreated());
    }
}
