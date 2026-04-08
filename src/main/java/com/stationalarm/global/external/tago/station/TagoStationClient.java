package com.stationalarm.global.external.tago.station;

import com.stationalarm.global.exception.custom.ExternalApiException;
import com.stationalarm.global.exception.custom.RetryableExternalApiException;
import com.stationalarm.global.exception.errorcode.TagoErrorCode;
import com.stationalarm.global.external.tago.TagoProperties;
import com.stationalarm.station.domain.BusStation;
import java.util.List;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class TagoStationClient {

    private final TagoProperties props;
    private final TagoStationParser parser;
    private final RestTemplate restTemplate;

    /**
     * TAGO 근처 정류장 API를 호출해 좌표 기준 버스 정류장 목록을 조회한다.
     */
    @Retryable(
            value = RetryableExternalApiException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    public List<BusStation> fetchNearbyStations(double lat, double lng) {
        log.info("[TAGO] 근접 정류장 조회 시작");

        String url = UriComponentsBuilder
                .fromUriString(props.getStation().getBaseUrl())
                .path("/getCrdntPrxmtSttnList")
                .queryParam("serviceKey", props.getApi().getKey())
                .queryParam("gpsLati", lat)
                .queryParam("gpsLong", lng)
                .queryParam("numOfRows", props.getApi().getDefaultNumOfRows())
                .queryParam("_type", props.getApi().getType())
                .build(true)
                .toUriString();

        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();

            if (status.is4xxClientError()) {
                log.info("[TAGO] 요청 오류 status={}", status.value());
                throw new ExternalApiException(TagoErrorCode.BAD_REQUEST);
            }

            if (status.is5xxServerError()) {
                log.warn("[TAGO] 서버 오류 status={}, retry 예정", status.value());
                throw new RetryableExternalApiException(TagoErrorCode.SERVER_ERROR);
            }

            throw e;
        } catch (RestClientException e) {
            log.warn("[TAGO] 통신 실패, retry 예정 lat={}, lng={}", lat, lng, e);
            throw new RetryableExternalApiException(TagoErrorCode.COMMUNICATION_ERROR);
        }

        HttpStatusCode status = response.getStatusCode();

        if (status.is2xxSuccessful()) {
            if (response.getBody() == null || status.value() == 204) {
                log.debug("[TAGO] 정류장 데이터 없음 lat={}, lng={}", lat, lng);
                return List.of();
            }

            try {
                List<BusStation> stations = parser.parseStations(response.getBody());
                log.debug("[TAGO] 정류장 조회 성공 count={}", stations.size());
                return stations;
            } catch (Exception e) {
                log.error("[TAGO] 정류장 파싱 실패", e);
                throw new ExternalApiException(TagoErrorCode.STATION_PARSE_ERROR);
            }
        }

        log.error("[TAGO] 예상하지 못한 응답 status={}", status.value());
        throw new ExternalApiException(TagoErrorCode.COMMUNICATION_ERROR);
    }

    /**
     * 재시도 횟수를 모두 소진했을 때 최종 예외를 변환해 던진다.
     */
    @Recover
    public List<BusStation> recover(
            RetryableExternalApiException e,
            double lat,
            double lng
    ) {
        log.error("[TAGO] 최종 실패 lat={}, lng={}", lat, lng, e);
        throw new ExternalApiException(TagoErrorCode.RETRY_EXHAUSTED);
    }
}
