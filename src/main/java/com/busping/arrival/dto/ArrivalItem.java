package com.busping.arrival.dto;

public record ArrivalItem(
        int remainingMinutes,
        int remainingStops,
        String routeType,
        String vehicleType
) {}
