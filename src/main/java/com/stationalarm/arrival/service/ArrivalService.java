package com.stationalarm.arrival.service;

import com.stationalarm.arrival.domain.Arrival;
import com.stationalarm.arrival.dto.ArrivalItem;
import com.stationalarm.arrival.dto.RouteArrivalResponse;
import com.stationalarm.arrival.dto.StationArrivalResponse;
import com.stationalarm.global.external.tago.arrival.TagoArrivalClient;
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
    private final TagoArrivalClient tagoArrivalClient;


    public StationArrivalResponse getGroupedArrivalsResponse(
            String cityCode,
            String stationId
    ) {

        Map<String, List<Arrival>> grouped =
                getGroupedArrivals(cityCode, stationId);

        List<RouteArrivalResponse> routes =
                grouped.entrySet().stream()
                        .map(entry -> {

                            String routeId = entry.getKey();
                            List<Arrival> list = entry.getValue();

                            String busNumber = list.get(0).getBusNumber();

                            List<ArrivalItem> items =
                                    list.stream()
                                            .map(a -> new ArrivalItem(
                                                    a.getRemainingMinutes(),
                                                    a.getRemainingStops(),
                                                    a.getRouteType(),
                                                    a.getVehicleType()
                                            ))
                                            .toList();

                            return new RouteArrivalResponse(
                                    routeId,
                                    busNumber,
                                    items
                            );
                        })
                        .toList();

        return new StationArrivalResponse(routes);
    }

    /**
     * 정류장 기준 도착정보를 조회하여
     *
     * 1. routeId 기준으로 그룹핑하고
     * 2. 각 route 내부에서는 remainingMinutes 기준 오름차순 정렬하며
     * 3. routeId 자체도 문자열 기준 오름차순 정렬하여 반환한다.
     *
     * @param cityCode 도시 코드
     * @param stationId 정류장 ID
     * @return routeId 기준으로 정렬된 Map (route 내부는 도착시간 오름차순 정렬)
     */
    public Map<String, List<Arrival>> getGroupedArrivals(
            String cityCode,
            String stationId
    ) {

        List<Arrival> arrivals =
                tagoArrivalClient.fetchRealtimeArrivals(cityCode, stationId);

        if (arrivals == null || arrivals.isEmpty()) {
            return Map.of();
        }

        // routeId 문자열 기준 정렬
        Map<String, List<Arrival>> grouped =
                arrivals.stream()
                        .collect(Collectors.groupingBy(
                                Arrival::getRouteId,
                                TreeMap::new,
                                Collectors.toList()
                        ));

        // 각 route 내부 도착시간 기준 오름차순 정렬
        grouped.values().forEach(list ->
                list.sort(Comparator.comparingInt(Arrival::getRemainingMinutes))
        );

        return grouped;
    }
}
