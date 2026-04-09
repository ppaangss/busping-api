package com.stationalarm.global.external.tago.arrival;

import com.stationalarm.arrival.domain.Arrival;

import java.util.List;

public interface TagoArrivalPort {
    List<Arrival> fetchRealtimeArrivals(String cityCode, String nodeId);
}
