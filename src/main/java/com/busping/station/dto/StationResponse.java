package com.busping.station.dto;

import com.busping.station.domain.BusStation;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StationResponse {

    private String stationId;
    private String stationName;
    private double latitude;
    private double longitude;
    private String cityCode;

    /**
     * 버스 정류장 엔티티를 클라이언트 응답 DTO로 변환한다.
     */
    public static StationResponse from(BusStation station) {
        return new StationResponse(
                station.getNodeId(),
                station.getName(),
                station.getLatitude(),
                station.getLongitude(),
                station.getCityCode()
        );
    }
}
