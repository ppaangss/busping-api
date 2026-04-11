package com.busping.global.external.tago.arrival;

import com.busping.arrival.domain.Arrival;

import java.util.List;

public interface TagoArrivalPort {
    List<Arrival> fetchRealtimeArrivals(String cityCode, String nodeId);
}
