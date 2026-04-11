package com.busping.global.external.tago.arrival;

import com.busping.arrival.domain.Arrival;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TagoArrivalResponse {

    private String stationId;
    private String stationName;
    private List<Arrival> arrivals;

}
