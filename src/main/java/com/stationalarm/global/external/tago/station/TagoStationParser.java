package com.stationalarm.global.external.tago.station;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stationalarm.station.domain.Station;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * String Json 형식의 외부 API를 객체로 변환해주는 클래스
 * */

@Component
@RequiredArgsConstructor
public class TagoStationParser {

    // Spring이 관리하는 ObjectMapper 사용
    // 전역 Jackson 설정 재사용
    // 테스트 시 Mock 가능
    private final ObjectMapper objectMapper;

    /**
     * TAGO 정류소 API RAW JSON → Station 목록 변환
     */
    public List<Station> parseStations(String rawJson) {

        List<Station> result = new ArrayList<>();

        try {

            JsonNode root = objectMapper.readTree(rawJson);

            // response → body → items → item[]
            JsonNode itemsNode = root
                    .path("response")
                    .path("body")
                    .path("items")
                    .path("item");

            // item이 배열이 아닐 수도 있음 (1건 응답)
            if (!itemsNode.isArray()) {
                return result;
            }

            for (JsonNode item : itemsNode) {

                //
                // asTest(null): 값 없으면 null
                // asDouble() , asInt(0): 값 없으면 기본값
                String nodeId = item.path("nodeid").asText(null);
                String name = item.path("nodenm").asText(null);
                double lat = item.path("gpslati").asDouble();
                double lng = item.path("gpslong").asDouble();
                String cityCode = item.path("citycode").asText(null);

                // 핵심 값 없으면 스킵
                if (nodeId == null || name == null) {
                    continue;
                }

                // 도메인 객체 생성
                result.add(
                        new Station(nodeId, name, lat, lng, cityCode)
                );
            }

        } catch (Exception e) {
            // 지금 단계에서는 로그만
            System.err.println("정류소 JSON 파싱 실패");
            e.printStackTrace();
        }

        return result;
    }
}
