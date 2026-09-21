package com.busping.alarm;

import com.busping.global.external.fcm.FakeFcmService;
import com.busping.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C-2 알람 시나리오 - "접근 1회 = 쿨다운 창 안에서 정확히 1회 발송"을 실 Redis TTL로 검증한다.
 * 쿨다운을 2초로 줄여 창 만료(재알림)까지 테스트한다.
 */
@TestPropertySource(properties = "alarm.cooldown-seconds=2")
class AlarmScenarioIntegrationTest extends IntegrationTestSupport {

    private static final double STATION_LAT = 37.5665;
    private static final double STATION_LNG = 126.9780;

    @MockitoSpyBean
    FakeFcmService fcmService;

    @Test
    @DisplayName("접근하면 1회 발송되고, 쿨다운 안 재접근은 무시되며, 창이 만료되면 재발송된다")
    void oneAlarmPerCooldownWindow() throws Exception {
        String deviceId = registerDevice();
        updateFcmToken(deviceId, "scenario-token");
        String folderId = createFolder(deviceId, "출근길");
        addRoute(deviceId, folderId, "SC1", "FAKE-ROUTE", STATION_LAT, STATION_LNG);

        // 접근 1: 500m 안 - 발송 1회
        reportLocation(deviceId, STATION_LAT + 0.002, STATION_LNG); // 약 222m
        verify(fcmService, times(1)).send(eq("scenario-token"), any(), any());

        // 접근 2: 쿨다운(2초) 안 - 여전히 1회
        reportLocation(deviceId, STATION_LAT + 0.001, STATION_LNG);
        verify(fcmService, times(1)).send(eq("scenario-token"), any(), any());

        // 접근 3: 쿨다운 만료 후 - 재알림으로 2회
        Thread.sleep(2500);
        reportLocation(deviceId, STATION_LAT + 0.001, STATION_LNG);
        verify(fcmService, times(2)).send(eq("scenario-token"), any(), any());
    }

    @Test
    @DisplayName("알람을 끈 디바이스는 접근해도 발송되지 않는다")
    void alarmOffDeviceGetsNothing() throws Exception {
        String deviceId = registerDevice();
        updateFcmToken(deviceId, "off-token");
        String folderId = createFolder(deviceId, "출근길");
        addRoute(deviceId, folderId, "SC2", "FAKE-ROUTE", STATION_LAT, STATION_LNG);

        mockMvc.perform(patch("/api/devices/me/alarm")
                        .header(DEVICE_HEADER, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alarmEnabled\":false}"))
                .andExpect(status().isOk());

        reportLocation(deviceId, STATION_LAT, STATION_LNG);

        verify(fcmService, times(0)).send(eq("off-token"), any(), any());
    }
}
