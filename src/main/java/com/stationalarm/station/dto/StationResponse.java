package com.stationalarm.station.dto;

import com.stationalarm.station.domain.Station;
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

    public static StationResponse from(Station station) {
        return new StationResponse(
                station.getNodeId(),
                station.getName(),
                station.getLatitude(),
                station.getLongitude(),
                station.getCityCode()
        );
    }
}

