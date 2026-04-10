package com.stationalarm.arrival.service;

import com.stationalarm.arrival.domain.Arrival;
import com.stationalarm.arrival.dto.ArrivalItem;
import com.stationalarm.arrival.dto.RouteArrivalResponse;
import com.stationalarm.arrival.dto.StationArrivalResponse;
import com.stationalarm.global.external.tago.arrival.TagoArrivalPort;
import com.stationalarm.global.external.tago.arrival.TagoArrivalWebClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArrivalService {

    private final TagoArrivalPort tagoArrivalClient;         // RestTemplate 기반 (REST API 단건 조회용)
    private final TagoArrivalWebClient tagoArrivalWebClient; // WebClient 기반 (알람 배치 병렬 조회용)

    public StationArrivalResponse getGroupedArrivalsResponse(String cityCode, String stationId) {
        Map<String, List<Arrival>> grouped = getGroupedArrivals(cityCode, stationId);

        List<RouteArrivalResponse> routes = grouped.entrySet().stream()
                .map(entry -> {
                    String routeId = entry.getKey();
                    List<Arrival> list = entry.getValue();
                    String busNumber = list.get(0).getBusNumber();
                    List<ArrivalItem> items = list.stream()
                            .map(a -> new ArrivalItem(
                                    a.getRemainingMinutes(),
                                    a.getRemainingStops(),
                                    a.getRouteType(),
                                    a.getVehicleType()
                            ))
                            .toList();
                    return new RouteArrivalResponse(routeId, busNumber, items);
                })
                .toList();

        return new StationArrivalResponse(routes);
    }

    // 알람 배치 경로: WebClient로 TAGO API 호출
    public Mono<Map<String, List<Arrival>>> getGroupedArrivalsMono(String cityCode, String stationId) {
        return tagoArrivalWebClient.fetchRealtimeArrivals(cityCode, stationId)
                .map(arrivals -> {
                    if (arrivals == null || arrivals.isEmpty()) return Map.<String, List<Arrival>>of();
                    // routeId 기준 그룹핑 (TreeMap: 문자열 오름차순 정렬)
                    Map<String, List<Arrival>> grouped = arrivals.stream()
                            .collect(Collectors.groupingBy(Arrival::getRouteId, TreeMap::new, Collectors.toList()));
                    // 각 노선 내부는 도착시간 오름차순 정렬
                    grouped.values().forEach(list ->
                            list.sort(Comparator.comparingInt(Arrival::getRemainingMinutes)));
                    return grouped;
                });
    }

    public Map<String, List<Arrival>> getGroupedArrivals(String cityCode, String stationId) {
        List<Arrival> arrivals = tagoArrivalClient.fetchRealtimeArrivals(cityCode, stationId);

        if (arrivals == null || arrivals.isEmpty()) {
            return Map.of();
        }

        // routeId 문자열 기준 정렬
        Map<String, List<Arrival>> grouped = arrivals.stream()
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
