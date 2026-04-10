package com.stationalarm.global.external.tago.arrival;

import com.stationalarm.arrival.domain.Arrival;
import com.stationalarm.global.exception.custom.ExternalApiException;
import com.stationalarm.global.exception.custom.RetryableExternalApiException;
import com.stationalarm.global.exception.errorcode.TagoErrorCode;
import com.stationalarm.global.external.tago.TagoProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RestTemplate 기반 TAGO 도착정보 클라이언트 (REST API 단건 조회용)
 *
 * 사용자가 앱에서 직접 도착정보를 조회할 때 사용한다.
 * 호출 시 스레드가 응답을 기다리는 동안 블로킹된다.
 *
 * 알람 배치의 병렬 호출에는 TagoArrivalWebClient(WebClient 방식)를 사용한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TagoArrivalClient implements TagoArrivalPort {

    private final RestTemplate restTemplate;
    private final TagoProperties props;
    private final TagoArrivalParser parser;
    private final AtomicLong callCount = new AtomicLong(0);

    /**
     * @param cityCode 도시코드
     * @param nodeId 정류장 정보
     * @return
     */
    @Retryable(
            value = RetryableExternalApiException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public List<Arrival> fetchRealtimeArrivals(String cityCode, String nodeId) {

        String url = UriComponentsBuilder
                .fromHttpUrl(props.getArrival().getBaseUrl())
                .path("/getSttnAcctoArvlPrearngeInfoList")
                .queryParam("serviceKey", props.getApi().getKey())
                .queryParam("cityCode", cityCode)
                .queryParam("nodeId", nodeId)
                .queryParam("numOfRows", props.getApi().getDefaultNumOfRows())
                .queryParam("_type", props.getApi().getType())
                .build(true)
                .toUriString();

        try {
            long callNumber = callCount.incrementAndGet();
            long start = System.currentTimeMillis();
            ResponseEntity<String> response =
                    restTemplate.getForEntity(url, String.class);
            long elapsed = System.currentTimeMillis() - start;
            log.info("[TAGO] #{} {}ms (cityCode={}, nodeId={})", callNumber, elapsed, cityCode, nodeId);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RetryableExternalApiException(TagoErrorCode.SERVER_ERROR);
            }

            return parser.parseArrival(response.getBody());

        } catch (HttpClientErrorException e) {
            throw new ExternalApiException(TagoErrorCode.BAD_REQUEST);

        } catch (HttpServerErrorException e) {
            throw new RetryableExternalApiException(TagoErrorCode.SERVER_ERROR);

        } catch (RestClientException e) {
            throw new RetryableExternalApiException(TagoErrorCode.COMMUNICATION_ERROR);
        }
    }

    @Recover
    public TagoArrivalResponse recover(RetryableExternalApiException e,
                                       String cityCode,
                                       String nodeId) {

        log.error("[TAGO] 재시도 후 최종 실패 cityCode={}, nodeId={}", cityCode, nodeId);
        throw new ExternalApiException(TagoErrorCode.RETRY_EXHAUSTED);
    }
}
