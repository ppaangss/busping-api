package com.busping.device;

import com.busping.favorite.domain.FavoriteFolderRepository;
import com.busping.favorite.domain.FavoriteRepository;
import com.busping.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceAuthIntegrationTest extends IntegrationTestSupport {

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
        mockMvc.perform(get("/api/favorites/folders").header(DEVICE_HEADER, "not-a-uuid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("형식은 맞지만 등록되지 않은 UUID는 401이다")
    void unregisteredUuidIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/favorites/folders").header(DEVICE_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("등록된 디바이스는 보호 API에 접근할 수 있다")
    void registeredDeviceIsAuthorized() throws Exception {
        String deviceId = registerDevice();

        mockMvc.perform(get("/api/favorites/folders").header(DEVICE_HEADER, deviceId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("디바이스를 삭제하면 폴더·즐겨찾기가 함께 지워지고 이후 접근은 401이다")
    void deleteCascadesAndInvalidatesDevice() throws Exception {
        // given - 디바이스에 폴더 1개, 노선 1개
        String deviceId = registerDevice();
        String folderId = createFolder(deviceId, "출근길");
        addRoute(deviceId, folderId, "ST1", "FAKE-ROUTE", 37.5665, 126.9780);
        assertThat(favoriteFolderRepository.findAllByDevice_IdOrderByIdAsc(UUID.fromString(deviceId))).hasSize(1);
        assertThat(favoriteRepository.findAllByFolder_Device_Id(UUID.fromString(deviceId))).hasSize(1);

        // when - 디바이스 삭제
        mockMvc.perform(delete("/api/devices/me").header(DEVICE_HEADER, deviceId))
                .andExpect(status().isOk());

        // then - DB 레벨 cascade로 전부 삭제 + 재접근 401
        assertThat(favoriteFolderRepository.findAllByDevice_IdOrderByIdAsc(UUID.fromString(deviceId))).isEmpty();
        assertThat(favoriteRepository.findAllByFolder_Device_Id(UUID.fromString(deviceId))).isEmpty();
        mockMvc.perform(get("/api/favorites/folders").header(DEVICE_HEADER, deviceId))
                .andExpect(status().isUnauthorized());
    }
}
