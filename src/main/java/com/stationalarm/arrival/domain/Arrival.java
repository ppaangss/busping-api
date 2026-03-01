package com.stationalarm.arrival.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Arrival {
    private String routeId;
    private String busNumber;
    private int remainingMinutes;
    private int remainingStops;
    private String routeType;
    private String vehicleType;
}
