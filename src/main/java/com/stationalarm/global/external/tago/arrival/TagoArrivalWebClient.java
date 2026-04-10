package com.stationalarm.global.external.tago.arrival;

import com.stationalarm.arrival.domain.Arrival;
import com.stationalarm.global.external.tago.TagoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 알람 배치 병렬 호출 전용 WebClient 기반 TAGO 도착정보 클라이언트
 *
 * 기존 TagoArrivalClient(RestTemplate)는 REST API 단건 조회용으로 유지하고,
 * 배치에서 다수 정류장을 동시에 호출할 때는 이 클래스를 사용한다.
 *
 * RestTemplate: 호출 → 응답 올 때까지 스레드 블로킹
 * WebClient:    호출 → 스레드 반환 → 응답 오면 콜백 실행 (논블로킹)
 */
@Slf4j
@Component
public class TagoArrivalWebClient {

    private final WebClient webClient;
    private final TagoProperties props;
    private final TagoArrivalParser parser;
    private final AtomicLong callCount = new AtomicLong(0);

    public TagoArrivalWebClient(WebClient.Builder builder, TagoProperties props, TagoArrivalParser parser) {
        // WebClient.Builder는 Spring이 자동 주입해주는 빌더
        // baseUrl을 미리 설정해두면 이후 호출 시 path만 추가하면 됨
        this.webClient = builder.baseUrl(props.getArrival().getBaseUrl()).build();
        this.props = props;
        this.parser = parser;
    }

    /**
     * TAGO 도착정보를 비동기로 조회한다.
     *
     * @return Mono<List<Arrival>>
     *         Mono: 지금 당장 값이 없고 미래에 하나의 결과가 올 것임을 나타내는 비동기 컨테이너
     *         구독(subscribe) 또는 block() 호출 시 실제 HTTP 요청이 발생한다.
     */
    public Mono<List<Arrival>> fetchRealtimeArrivals(String cityCode, String nodeId) {
        long callNumber = callCount.incrementAndGet();
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getSttnAcctoArvlPrearngeInfoList")
                        .queryParam("serviceKey", props.getApi().getKey())
                        .queryParam("cityCode", cityCode)
                        .queryParam("nodeId", nodeId)
                        .queryParam("numOfRows", props.getApi().getDefaultNumOfRows())
                        .queryParam("_type", props.getApi().getType())
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(parser::parseArrival)
                .elapsed()
                .doOnNext(t -> log.info("[TAGO] #{} {}ms (cityCode={}, nodeId={})",
                        callNumber, t.getT1(), cityCode, nodeId))
                .map(Tuple2::getT2)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
    }
}
