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
    private final TagoArrivalPort tagoArrivalClient;
    private final TagoArrivalWebClient tagoArrivalWebClient;


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
    /**
     * 알람 배치 전용 비동기 도착정보 조회 메서드
     * WebClient를 사용해 Mono(비동기 단일 값)로 반환하며,
     * AlarmCoreService에서 Flux.flatMap()으로 병렬 호출될 때 사용된다.
     * getGroupedArrivals()와 동일한 그룹핑/정렬 로직이지만 반환 타입이 다르다.
     *
     * @return Mono: 미래에 도착할 결과를 감싼 비동기 컨테이너
     */
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
