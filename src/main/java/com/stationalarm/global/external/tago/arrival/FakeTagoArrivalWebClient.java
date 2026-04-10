package com.stationalarm.global.external.tago.arrival;

import com.stationalarm.arrival.domain.Arrival;
import com.stationalarm.global.external.tago.TagoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * 벤치마크용 가짜 WebClient 클라이언트
 *
 * benchmark 프로파일 활성화 시 TagoArrivalWebClient 대신 주입됨 (@Primary)
 * 실제 TAGO API를 호출하지 않고 실측 평균 응답시간만큼 지연 후 빈 응답 반환
 * → 네트워크/서버 변수 없이 순수하게 순차 vs 병렬 구조 차이만 측정 가능
 */
@Slf4j
@Profile("benchmark")
@Primary
@Component
public class FakeTagoArrivalWebClient extends TagoArrivalWebClient {

    private static final long FAKE_LATENCY_MS = 1_385;  // 실측 중앙값 (ms)

    public FakeTagoArrivalWebClient(WebClient.Builder builder, TagoProperties props, TagoArrivalParser parser) {
        super(builder, props, parser);
    }

    @Override
    public Mono<List<Arrival>> fetchRealtimeArrivals(String cityCode, String nodeId) {
        return Mono.delay(Duration.ofMillis(FAKE_LATENCY_MS))
                .thenReturn(List.<Arrival>of())
                .elapsed()
                .doOnNext(t -> log.info("[TAGO Fake] {}ms", t.getT1()))
                .map(tuple -> tuple.getT2());
    }
}
