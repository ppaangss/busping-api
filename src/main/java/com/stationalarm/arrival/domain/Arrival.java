package com.stationalarm.arrival.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized // Redis에서 JSON → Arrival 객체로 역직렬화할 때 Jackson이 @Builder를 인식하게 해줌
             // 없으면 캐시에서 꺼낼 때 역직렬화 실패
public class Arrival {
    private String routeId;
    private String busNumber;
    private int remainingMinutes;
    private int remainingStops;
    private String routeType;
    private String vehicleType;
}
