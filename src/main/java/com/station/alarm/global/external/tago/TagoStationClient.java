package com.station.alarm.global.external.tago;

import com.station.alarm.global.exception.custom.ExternalApiException;
import com.station.alarm.global.exception.custom.RetryableExternalApiException;
import com.station.alarm.global.exception.errorcode.TagoErrorCode;
import com.station.alarm.station.domain.Station;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagoStationClient {

    private final TagoProperties props;
    private final TagoStationParser parser;

    private final RestTemplate restTemplate;
    
    /**
     * Tago 정류장 API 호출
     * 좌표 기반 호출
     * 성공/실패에 따른 제어
     * 도메인으로 변환
     * 재시도 정책
     */

    @Retryable(
            value = RetryableExternalApiException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    public List<Station> fetchNearbyStations(double lat, double lng) {

        log.info("[TAGO] 근접 정류장 조회 시작 lat={}, lng={}", lat, lng);

        String url = UriComponentsBuilder
                .fromUriString(props.getStation().getBaseUrl())
                .path("/getCrdntPrxmtSttnList")
                .queryParam("serviceKey", props.getApi().getKey())
                .queryParam("gpsLati", lat)
                .queryParam("gpsLong", lng)
                .queryParam("numOfRows", props.getApi().getDefaultNumOfRows())
                .queryParam("_type", props.getApi().getType())
                .build(true)   // 이미 인코딩된 값은 유지
                .toUriString();

        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );
        }
        catch (org.springframework.web.client.HttpStatusCodeException e) {
            // 4xx, 5xx 에러는 여기서 잡힙니다.
            HttpStatusCode status = e.getStatusCode();

            // 4xx -> 재시도 x
            if (status.is4xxClientError()) {
                log.info("[TAGO] 요청 오류 status={}", status.value());
                // 4xx는 재시도하지 않는 일반 예외를 던집니다.
                throw new ExternalApiException(TagoErrorCode.BAD_REQUEST);
            }

            // 5xx -> 재시도 o
            // 5xx 응답인 경우 retry 시도 허용
            if (status.is5xxServerError()) {
                log.warn("[TAGO] 서버 오류 status={}, retry 예정", status.value());
                // 5xx는 재시도를 유발하는 예외를 던집니다.
                throw new RetryableExternalApiException(TagoErrorCode.SERVER_ERROR);
            }

            throw e;
        }
        catch (RestClientException e) {
            // timeout, 연결 실패 등
            // timeout 의 경우 retry 시도 허용

            // 통신 실패 + 재시도 예정 (WARN)
            log.warn("[TAGO] 통신 실패, retry 예정 lat={}, lng={}", lat, lng, e);

            throw new RetryableExternalApiException(TagoErrorCode.COMMUNICATION_ERROR);
        }

        // 정상
        HttpStatusCode status = response.getStatusCode();

        if (status.is2xxSuccessful()) {

            if (response.getBody() == null || status.value() == 204) {

                // 정상 + 데이터 없음 (DEBUG)
                log.debug("[TAGO] 정류장 데이터 없음 lat={}, lng={}", lat, lng);

                return List.of();
            }

            try {

                List<Station> stations = parser.parseStations(response.getBody());

                // 정상 처리 완료 (DEBUG)
                log.debug("[TAGO] 정류장 조회 성공 count={}", stations.size());

                return stations;
            } catch (Exception e) {
                // 파싱 오류 (ERROR)
                log.error("[TAGO] 정류장 파싱 실패", e);

                throw new ExternalApiException(TagoErrorCode.PARSE_ERROR);
            }
        }

        log.error("[TAGO] 알 수 없는 응답 status={}", status.value());

        // fallback
        throw new ExternalApiException(TagoErrorCode.COMMUNICATION_ERROR);
    }

    /**
     * 최종 API 요청 실패 시 처리 메서드
    * */
    @Recover
    public List<Station> recover(RetryableExternalApiException e,
                                 double lat, double lng) {

        log.error("[TAGO] 최종 실패 lat={}, lng={}", lat, lng, e);

        throw new ExternalApiException(TagoErrorCode.RETRY_EXHAUSTED);
    }
}
