package com.busping.global.external.tago.arrival;

import com.busping.arrival.domain.Arrival;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TAGO 실 호출 대체 클라이언트 - dev 전용. 즉시 가짜 도착 데이터 반환 (실 API 키·네트워크 불필요).
 * 성능 측정용 지연 재현은 mock TAGO(WireMock, loadtest 프로파일)가 담당한다.
 */
@Slf4j
@Profile("dev")
@Component // primary는 캐시 데코레이터(ArrivalCacheConfig)가 가져가고, 이 빈은 그 안의 원 출처로 조립된다
public class FakeTagoArrivalClient implements TagoArrivalPort {

    @Override
    public List<Arrival> fetchRealtimeArrivals(String cityCode, String nodeId) {
        log.info("[TAGO Fake] (cityCode={}, nodeId={})", cityCode, nodeId);

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
}
