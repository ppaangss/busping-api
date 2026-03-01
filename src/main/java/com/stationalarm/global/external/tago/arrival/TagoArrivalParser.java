package com.stationalarm.global.external.tago.arrival;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stationalarm.arrival.domain.Arrival;
import com.stationalarm.global.exception.custom.ExternalApiException;
import com.stationalarm.global.exception.errorcode.TagoErrorCode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
@Slf4j
@Component
public class TagoArrivalParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * TAGO 버스 도착정보 API의 Raw JSON 응답을 파싱하여
     * 내부 DTO Arrival 리스트로 변환한다.
     *
     * @param rawJson TAGO 도착정보 API의 원본 JSON 문자열
     * @return 도착 예정 버스 목록
     * @throws ExternalApiException resultCode 오류 또는 JSON 구조 파싱 실패 시 발생
     */
    public List<Arrival> parseArrival(String rawJson) {

        try {
            JsonNode root = objectMapper.readTree(rawJson);

            JsonNode header = root.path("response").path("header");
            String resultCode = header.path("resultCode").asText();

            if (!"00".equals(resultCode)) {
                log.error("[TAGO] API 비즈니스 오류 resultCode={}", resultCode);
                throw new ExternalApiException(TagoErrorCode.BAD_REQUEST);
            }

            JsonNode body = root.path("response").path("body");
            JsonNode itemsNode = body.path("items").path("item");

            if (itemsNode.isMissingNode() || itemsNode.isNull()) {
                return List.of();
            }

            List<Arrival> arrivals = new ArrayList<>();

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    arrivals.add(parseItem(item));
                }
            } else if (itemsNode.isObject()) {
                arrivals.add(parseItem(itemsNode));
            }

            // 남은 시간 기준 정렬
            arrivals.sort(Comparator.comparing(Arrival::getRemainingMinutes));

            return arrivals;

        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TAGO] 도착정보 파싱 실패", e);
            throw new ExternalApiException(TagoErrorCode.ARRIVAL_PARSE_ERROR);
        }
    }

    /**
     * 단일 도착정보(JsonNode)를 Arrival 객체로 변환
     */
    private Arrival parseItem(JsonNode item) {

        int arrTimeSec = item.path("arrtime").asInt();
        int remainingMinutes = arrTimeSec > 0 ? arrTimeSec / 60 : 0;

        return Arrival.builder()
                .routeId(item.path("routeid").asText())
                .busNumber(item.path("routeno").asText())
                .remainingMinutes(remainingMinutes)
                .remainingStops(item.path("arrprevstationcnt").asInt())
                .vehicleType(item.path("vehicletp").asText())
                .routeType(item.path("routetp").asText())
                .build();
    }
}
