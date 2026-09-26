package com.busping.arrival.service;

import com.busping.arrival.domain.Arrival;
import com.busping.arrival.dto.ArrivalItem;
import com.busping.arrival.dto.RouteArrivalResponse;
import com.busping.arrival.dto.StationArrivalResponse;
import com.busping.global.external.tago.arrival.TagoArrivalPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArrivalService {

    private final TagoArrivalPort tagoArrivalClient;

    public StationArrivalResponse getGroupedArrivalsResponse(String cityCode, String stationId) {
        Map<String, List<Arrival>> grouped = getGroupedArrivals(cityCode, stationId);

        List<RouteArrivalResponse> routes = grouped.entrySet().stream()
                .map(entry -> {
                    String routeId = entry.getKey();
                    List<Arrival> list = entry.getValue();
                    String busNumber = list.get(0).getBusNumber();
                    List<ArrivalItem> items = list.stream()
                            .map(arrival -> new ArrivalItem(
                                    arrival.getRemainingMinutes(),
                                    arrival.getRemainingStops(),
                                    arrival.getRouteType(),
                                    arrival.getVehicleType()
                            ))
                            .toList();
                    return new RouteArrivalResponse(routeId, busNumber, items);
                })
                .toList();

        return new StationArrivalResponse(routes);
    }

    public Map<String, List<Arrival>> getGroupedArrivals(String cityCode, String stationId) {
        List<Arrival> arrivals = tagoArrivalClient.fetchRealtimeArrivals(cityCode, stationId);
        return groupByRoute(arrivals);
    }

    private Map<String, List<Arrival>> groupByRoute(List<Arrival> arrivals) {
        if (arrivals == null || arrivals.isEmpty()) {
            return Map.of();
        }

        Map<String, List<Arrival>> grouped = arrivals.stream()
                .collect(Collectors.groupingBy(
                        Arrival::getRouteId,
                        TreeMap::new,
                        Collectors.toList()
                ));

        grouped.values().forEach(list ->
                list.sort(Comparator.comparingInt(Arrival::getRemainingMinutes))
        );

        return grouped;
    }
}
