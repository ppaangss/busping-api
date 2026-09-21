package com.busping.station;

import com.busping.station.domain.BusStation;
import com.busping.station.domain.BusStationRepository;
import com.busping.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 주변 정류장 검색 - 바운딩박스 + 반경 1km 필터를 실 DB 데이터로 검증한다.
 */
class StationSearchIntegrationTest extends IntegrationTestSupport {

    private static final double BASE_LAT = 35.1595; // 다른 테스트와 겹치지 않는 지역(광주)
    private static final double BASE_LNG = 126.8526;

    @Autowired
    BusStationRepository busStationRepository;

    @BeforeEach
    void setUpStations() {
        busStationRepository.deleteAll();
        busStationRepository.save(station("NEAR1", "가까운정류장", BASE_LAT + 0.002, BASE_LNG)); // 약 222m
        busStationRepository.save(station("FAR1", "먼정류장", BASE_LAT + 0.05, BASE_LNG));      // 약 5.5km
    }

    @Test
    @DisplayName("반경 1km 안 정류장만 반환된다")
    void onlyNearbyStationsReturned() throws Exception {
        String deviceId = registerDevice();

        mockMvc.perform(get("/api/stations/nearby")
                        .param("latitude", String.valueOf(BASE_LAT))
                        .param("longitude", String.valueOf(BASE_LNG))
                        .header(DEVICE_HEADER, deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].stationId").value("NEAR1"));
    }

    private BusStation station(String nodeId, String name, double latitude, double longitude) {
        return new BusStation(null, nodeId, name, latitude, longitude,
                null, "24", "광주광역시", null, null, null);
    }
}
