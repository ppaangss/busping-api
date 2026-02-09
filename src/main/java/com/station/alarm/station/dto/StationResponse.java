package com.station.alarm.station.dto;

import com.station.alarm.station.domain.Station;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StationResponse {

    private String stationId;
    private String stationName;
    private double latitude;
    private double longitude;

    public static StationResponse from(Station station) {
        return new StationResponse(
                station.getNodeId(),
                station.getName(),
                station.getLatitude(),
                station.getLongitude()
        );
    }
}

