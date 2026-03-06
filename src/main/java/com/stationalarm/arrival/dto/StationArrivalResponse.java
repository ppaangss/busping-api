package com.stationalarm.arrival.dto;

import java.util.List;

public record StationArrivalResponse(
        List<RouteArrivalResponse> routes
) {}