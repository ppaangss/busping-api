package com.busping.arrival.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized // 캐시 역직렬화용 - Jackson이 빌더 경유로 복원 (no-args 생성자 없이)
public class Arrival {
    private String routeId;
    private String busNumber;
    private int remainingMinutes;
    private int remainingStops;
    private String routeType;
    private String vehicleType;
}
