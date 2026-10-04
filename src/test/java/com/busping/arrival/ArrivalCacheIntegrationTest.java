package com.busping.arrival;

import com.busping.global.external.fcm.FakeFcmService;
import com.busping.global.external.tago.arrival.FakeTagoArrivalClient;
import com.busping.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 정류장 단위 캐시 - 실 Redis TTL로 "첫 요청만 원 호출, 나머지는 캐시 한 장" 검증.
 * TTL을 1초로 줄여 만료 후 재호출까지 테스트한다.
 */
@TestPropertySource(properties = "arrival-cache.ttl-seconds=1")
class ArrivalCacheIntegrationTest extends IntegrationTestSupport {

    private static final double STATION_LAT = 37.5665;
    private static final double STATION_LNG = 126.9780;

    @MockitoSpyBean
    FakeTagoArrivalClient tagoClient;

    @MockitoSpyBean
    FakeFcmService fcmService;

    @Test
    @DisplayName("같은 정류장을 여러 번 조회해도 TTL 안에서는 원 호출이 1번이다")
    void sameStationFetchesOriginOnce() throws Exception {
        String deviceId = registerDevice();
        String station = uniqueStation();

        String first = lookup(deviceId, station);
        String second = lookup(deviceId, station);
        String third = lookup(deviceId, station);

        verify(tagoClient, times(1)).fetchRealtimeArrivals(eq("23"), eq(station));
        assertThat(second).isEqualTo(first);
        assertThat(third).isEqualTo(first);
    }

    @Test
    @DisplayName("다른 정류장은 캐시를 공유하지 않는다 - 키는 정류장 단위")
    void differentStationsAreSeparateEntries() throws Exception {
        String deviceId = registerDevice();
        String stationA = uniqueStation();
        String stationB = uniqueStation();

        lookup(deviceId, stationA);
        lookup(deviceId, stationB);

        verify(tagoClient, times(1)).fetchRealtimeArrivals(eq("23"), eq(stationA));
        verify(tagoClient, times(1)).fetchRealtimeArrivals(eq("23"), eq(stationB));
    }

    @Test
    @DisplayName("TTL이 지나면 다시 원 호출로 간다 - 낡음의 상한이 지켜진다")
    void expiredEntryRefetches() throws Exception {
        String deviceId = registerDevice();
        String station = uniqueStation();

        lookup(deviceId, station);
        Thread.sleep(1300); // TTL 1초 만료 대기
        lookup(deviceId, station);

        verify(tagoClient, times(2)).fetchRealtimeArrivals(eq("23"), eq(station));
    }

    @Test
    @DisplayName("조회가 만든 캐시를 알람 평가가 재사용한다 - 두 경로는 같은 검문소를 지난다")
    void alarmPathReusesLookupCache() throws Exception {
        String deviceId = registerDevice();
        updateFcmToken(deviceId, "token-cache-test");
        String station = uniqueStation();
        String folderId = createFolder(deviceId, "출근");
        addRoute(deviceId, folderId, station, "FAKE-ROUTE", STATION_LAT, STATION_LNG);

        lookup(deviceId, station);                                // 조회가 캐시를 만든다
        reportLocation(deviceId, STATION_LAT, STATION_LNG);       // 알람 평가는 같은 정류장을 쓴다

        // 발송은 완료되는데(캐시 데이터로 평가), 원 호출은 조회 때 1번뿐
        verify(fcmService, timeout(3000)).send(eq("token-cache-test"), any(), any());
        verify(tagoClient, times(1)).fetchRealtimeArrivals(eq("23"), eq(station));
    }

    private String lookup(String deviceId, String station) throws Exception {
        return mockMvc.perform(get("/api/stations/" + station + "/realtime")
                        .param("cityCode", "23")
                        .header(DEVICE_HEADER, deviceId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String uniqueStation() {
        // Redis 컨테이너가 테스트 클래스 간 공유되므로 키 충돌을 막는다
        return "STA-" + UUID.randomUUID();
    }
}
