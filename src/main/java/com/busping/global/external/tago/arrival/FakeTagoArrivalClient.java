package com.busping.global.external.tago.arrival;

import com.busping.arrival.domain.Arrival;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TAGO 실 호출 대체 클라이언트.
 * - dev: 지연 0ms로 가짜 도착 데이터 반환 (실 API 키 불필요)
 * - benchmark: fake-latency-ms를 실측 평균(1385)으로 올려 부하 측정용으로 사용
 */
@Slf4j
@Profile({"dev", "benchmark"})
@Primary
@Component
public class FakeTagoArrivalClient implements TagoArrivalPort {

    @Value("${tago.fake-latency-ms:0}")
    private long fakeLatencyMs;

    @Override
    public List<Arrival> fetchRealtimeArrivals(String cityCode, String nodeId) {
        simulateLatency();

        log.info("[TAGO Fake] {}ms (cityCode={}, nodeId={})", fakeLatencyMs, cityCode, nodeId);

        // 어떤 정류장을 조회하든 FAKE-ROUTE 노선 버스 2대를 돌려준다
        // (dev에서 알람 플로우를 보려면 즐겨찾기 routeId를 FAKE-ROUTE로 등록)
        return List.of(
                fakeArrival(3, 2),
                fakeArrival(12, 8)
        );
    }

    private Arrival fakeArrival(int remainingMinutes, int remainingStops) {
        return Arrival.builder()
                .routeId("FAKE-ROUTE")
                .busNumber("77")
                .remainingMinutes(remainingMinutes)
                .remainingStops(remainingStops)
                .routeType("일반버스")
                .vehicleType("일반차량")
                .build();
    }

    private void simulateLatency() {
        if (fakeLatencyMs <= 0) {
            return;
        }
        try {
            Thread.sleep(fakeLatencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
