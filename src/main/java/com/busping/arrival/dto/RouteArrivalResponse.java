package com.busping.arrival.dto;

import java.util.List;

public record RouteArrivalResponse(
        String routeId,
        String busNumber,
        List<ArrivalItem> arrivals
) {}