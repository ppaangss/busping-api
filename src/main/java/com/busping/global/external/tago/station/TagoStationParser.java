package com.busping.global.external.tago.station;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.busping.station.domain.BusStation;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TagoStationParser {

    private final ObjectMapper objectMapper;

    /**
     * TAGO 정류장 API RAW JSON을 BusStation 목록으로 변환한다.
     */
    public List<BusStation> parseStations(String rawJson) {
        List<BusStation> result = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode itemsNode = root
                    .path("response")
                    .path("body")
                    .path("items")
                    .path("item");

            if (!itemsNode.isArray()) {
                return result;
            }

            for (JsonNode item : itemsNode) {
                String nodeId = item.path("nodeid").asText(null);
                String name = item.path("nodenm").asText(null);
                double lat = item.path("gpslati").asDouble();
                double lng = item.path("gpslong").asDouble();
                String cityCode = item.path("citycode").asText(null);

                if (nodeId == null || name == null) {
                    continue;
                }

                // result.add(new BusStation(nodeId, name, lat, lng, cityCode));
            }
        } catch (Exception e) {
            System.err.println("정류장 JSON 파싱 실패");
            e.printStackTrace();
        }

        return result;
    }
}
