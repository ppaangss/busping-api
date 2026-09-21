package com.busping.favorite;

import com.busping.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 즐겨찾기 폴더·노선 CRUD와 소유권 격리를 API 왕복으로 검증한다.
 */
class FavoriteCrudIntegrationTest extends IntegrationTestSupport {

    private static final double LAT = 37.5665;
    private static final double LNG = 126.9780;

    @Test
    @DisplayName("폴더는 생성-조회-이름변경-삭제 생명주기를 돈다")
    void folderLifecycle() throws Exception {
        String deviceId = registerDevice();
        String folderId = createFolder(deviceId, "출근길");

        mockMvc.perform(get("/api/favorites/folders").header(DEVICE_HEADER, deviceId))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("출근길"));

        mockMvc.perform(patch("/api/favorites/folders/" + folderId)
                        .header(DEVICE_HEADER, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"퇴근길\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/favorites/folders").header(DEVICE_HEADER, deviceId))
                .andExpect(jsonPath("$.data[0].name").value("퇴근길"));

        mockMvc.perform(delete("/api/favorites/folders/" + folderId).header(DEVICE_HEADER, deviceId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/favorites/folders").header(DEVICE_HEADER, deviceId))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("다른 디바이스의 폴더는 이름을 바꿀 수 없다 - 404")
    void foreignFolderIsInvisible() throws Exception {
        String owner = registerDevice();
        String folderId = createFolder(owner, "주인폴더");

        String intruder = registerDevice();
        mockMvc.perform(patch("/api/favorites/folders/" + folderId)
                        .header(DEVICE_HEADER, intruder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"탈취\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("같은 폴더에 같은 정류장·노선을 다시 추가하면 409다")
    void duplicateRouteConflicts() throws Exception {
        String deviceId = registerDevice();
        String folderId = createFolder(deviceId, "출근길");
        addRoute(deviceId, folderId, "CRUD1", "ROUTE1", LAT, LNG);

        mockMvc.perform(post("/api/favorites/" + folderId + "/routes")
                        .header(DEVICE_HEADER, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stationId":"CRUD1","stationName":"시청앞","regionCode":"23",
                                 "latitude":37.5665,"longitude":126.9780,
                                 "routeId":"ROUTE1","routeName":"77"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("노선을 추가하고 목록에서 확인하고 삭제한다")
    void routeAddListDelete() throws Exception {
        String deviceId = registerDevice();
        String folderId = createFolder(deviceId, "출근길");
        addRoute(deviceId, folderId, "CRUD2", "ROUTE1", LAT, LNG);

        String listBody = mockMvc.perform(get("/api/favorites/" + folderId + "/routes")
                        .header(DEVICE_HEADER, deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        int favoriteId = com.jayway.jsonpath.JsonPath.read(listBody, "$.data[0].favoriteId");

        mockMvc.perform(delete("/api/favorites/" + folderId + "/routes/" + favoriteId)
                        .header(DEVICE_HEADER, deviceId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/favorites/" + folderId + "/routes").header(DEVICE_HEADER, deviceId))
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
