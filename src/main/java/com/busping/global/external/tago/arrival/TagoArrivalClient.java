package com.busping.global.external.tago.arrival;

import com.busping.arrival.domain.Arrival;
import com.busping.global.exception.custom.ExternalApiException;
import com.busping.global.exception.errorcode.TagoErrorCode;
import com.busping.global.external.tago.TagoProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RestTemplate 기반 TAGO 도착정보 클라이언트 (REST API 단건 조회용)
 *
 * 사용자가 앱에서 직접 도착정보를 조회할 때 사용한다.
 * 호출 시 스레드가 응답을 기다리는 동안 블로킹된다.
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
    public List<Arrival> fetchRealtimeArrivals(String cityCode, String nodeId) {

        // 템플릿 + 변수 분리 호출 - http_client_requests의 uri 태그에 전개 전 템플릿이 기록된다
        // (완성 URL로 넘기면 정류장마다 시계열이 쪼개져 increase() 집계가 새고, serviceKey가 라벨에 노출됨)
        String uriTemplate = props.getArrival().getBaseUrl()
                + "/getSttnAcctoArvlPrearngeInfoList"
                + "?serviceKey={serviceKey}&cityCode={cityCode}&nodeId={nodeId}&numOfRows={numOfRows}&_type={type}";

        Map<String, Object> uriVariables = Map.of(
                "serviceKey", props.getApi().getKey(),
                "cityCode", cityCode,
                "nodeId", nodeId,
                "numOfRows", props.getApi().getDefaultNumOfRows(),
                "type", props.getApi().getType());

        try {
            long callNumber = callCount.incrementAndGet();
            long start = System.currentTimeMillis();
            ResponseEntity<String> response =
                    restTemplate.getForEntity(uriTemplate, String.class, uriVariables);
            long elapsed = System.currentTimeMillis() - start;
            log.info("[TAGO] #{} {}ms (cityCode={}, nodeId={})", callNumber, elapsed, cityCode, nodeId);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ExternalApiException(TagoErrorCode.SERVER_ERROR);
            }

            return parser.parseArrival(response.getBody());

        } catch (HttpClientErrorException e) {
            throw new ExternalApiException(TagoErrorCode.BAD_REQUEST);

        } catch (HttpServerErrorException e) {
            throw new ExternalApiException(TagoErrorCode.SERVER_ERROR);

        } catch (RestClientException e) {
            throw new ExternalApiException(TagoErrorCode.COMMUNICATION_ERROR);
        }
    }
}
