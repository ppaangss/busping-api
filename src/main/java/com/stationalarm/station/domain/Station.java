package com.stationalarm.station.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Station {

    private final String nodeId;     // 정류소 ID (핵심)
    private final String name;       // 정류소명
    private final double latitude;   // 위도
    private final double longitude;  // 경도
    private final String cityCode;      // 도시코드

}
