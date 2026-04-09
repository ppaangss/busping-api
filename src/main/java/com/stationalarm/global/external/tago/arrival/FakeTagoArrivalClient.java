package com.stationalarm.global.external.tago.arrival;

import com.stationalarm.arrival.domain.Arrival;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

// 외부 API 호출 대용 테스트용 클래스
@Slf4j
@Profile("benchmark")
@Primary
@Component
public class FakeTagoArrivalClient implements TagoArrivalPort {

    private static final long FAKE_LATENCY_MS = 1_385;  // 실측 평균 응답시간

    @Override
    public List<Arrival> fetchRealtimeArrivals(String cityCode, String nodeId) {
        //log.info("[FAKE] 요청 시작 stationId={} thread={}", nodeId, Thread.currentThread().getName());
        try {
            Thread.sleep(FAKE_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        //log.info("[FAKE] 요청 완료 stationId={} thread={}", nodeId, Thread.currentThread().getName());
        return List.of();
    }
}
