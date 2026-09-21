package com.busping.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 통합 테스트 공통 베이스.
 * - Testcontainers로 실 MySQL·Redis를 띄운다 (로컬 개발용 컨테이너와 별개, 테스트 후 자동 폐기)
 * - static 싱글톤 컨테이너 - 모든 테스트 클래스가 공유해서 기동 비용을 1회만 낸다
 * - dev 프로파일 - TAGO·FCM이 fake라 외부 호출 없이 전체 플로우가 돈다
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public abstract class IntegrationTestSupport {

    protected static final String DEVICE_HEADER = "X-Device-Id";

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7")
            .withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    // ===== 공용 API 헬퍼 - 데이터 준비도 실제 API 호출로 한다 =====

    protected String registerDevice() throws Exception {
        String body = mockMvc.perform(post("/api/devices"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.deviceId");
    }

    protected void updateFcmToken(String deviceId, String token) throws Exception {
        mockMvc.perform(patch("/api/devices/me/fcm-token")
                        .header(DEVICE_HEADER, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fcmToken\":\"" + token + "\"}"))
                .andExpect(status().isOk());
    }

    protected String createFolder(String deviceId, String name) throws Exception {
        String body = mockMvc.perform(post("/api/favorites/folders")
                        .header(DEVICE_HEADER, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return String.valueOf((int) JsonPath.read(body, "$.data.id"));
    }

    protected void addRoute(String deviceId, String folderId, String stationId, String routeId,
                            double latitude, double longitude) throws Exception {
        mockMvc.perform(post("/api/favorites/" + folderId + "/routes")
                        .header(DEVICE_HEADER, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stationId":"%s","stationName":"시청앞","regionCode":"23",
                                 "latitude":%f,"longitude":%f,
                                 "routeId":"%s","routeName":"77"}
                                """.formatted(stationId, latitude, longitude, routeId)))
                .andExpect(status().isCreated());
    }

    protected void reportLocation(String deviceId, double latitude, double longitude) throws Exception {
        mockMvc.perform(post("/api/devices/me/location")
                        .header(DEVICE_HEADER, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":%f,\"longitude\":%f}".formatted(latitude, longitude)))
                .andExpect(status().isOk());
    }
}
