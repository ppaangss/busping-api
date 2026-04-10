package com.stationalarm.arrival.service;

import com.stationalarm.arrival.domain.Arrival;
import com.stationalarm.arrival.dto.ArrivalItem;
import com.stationalarm.arrival.dto.RouteArrivalResponse;
import com.stationalarm.arrival.dto.StationArrivalResponse;
import com.stationalarm.global.external.tago.arrival.TagoArrivalPort;
import com.stationalarm.global.external.tago.arrival.TagoArrivalWebClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
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
    private final CacheManager cacheManager;                 // 배치 경로 수동 캐시 처리용

    static final String CACHE_NAME = "arrivals"; // Redis 키 prefix: "arrivals::cityCode:stationId"

    // ── REST API 경로 ──────────────────────────────────────────────────────────
    // 컨트롤러에서 외부 호출하므로 Spring 프록시가 정상 동작한다.
    // 프록시 동작 순서:
    //   1. Redis에서 "arrivals::cityCode:stationId" 키 조회
    //   2. 히트 → 메서드 실행 없이 캐시 값 즉시 반환
    //   3. 미스 → 메서드 실행 → 결과를 Redis에 저장 후 반환
    @Cacheable(value = CACHE_NAME, key = "#cityCode + ':' + #stationId")
    public StationArrivalResponse getGroupedArrivalsResponse(String cityCode, String stationId) {
        // 캐시 미스일 때만 실행됨
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

    // ── 알람 배치 경로 ─────────────────────────────────────────────────────────
    // @Cacheable은 Mono 반환 타입과 동작하지 않으므로 CacheManager로 직접 처리한다.
    // (Spring이 Mono를 구독하기 전에 결과를 알 수 없어 캐싱 시점을 잡지 못함)
    public Mono<Map<String, List<Arrival>>> getGroupedArrivalsMono(String cityCode, String stationId) {
        String cacheKey = cityCode + ":" + stationId;
        Cache cache = cacheManager.getCache(CACHE_NAME); // "arrivals" 캐시 가져오기

        // 수동으로 Redis 조회
        if (cache != null) {
            Map<String, List<Arrival>> cached = cache.get(cacheKey, Map.class);
            if (cached != null) {
                return Mono.just(cached); // 캐시 히트 → WebClient 호출 없이 즉시 반환
            }
        }

        // 캐시 미스 → WebClient로 TAGO API 호출
        return tagoArrivalWebClient.fetchRealtimeArrivals(cityCode, stationId)
                .map(arrivals -> {
                    if (arrivals == null || arrivals.isEmpty()) return new TreeMap<String, List<Arrival>>();
                    // routeId 기준 그룹핑 (TreeMap: 문자열 오름차순 정렬)
                    Map<String, List<Arrival>> grouped = arrivals.stream()
                            .collect(Collectors.groupingBy(Arrival::getRouteId, TreeMap::new, Collectors.toList()));
                    // 각 노선 내부는 도착시간 오름차순 정렬
                    grouped.values().forEach(list ->
                            list.sort(Comparator.comparingInt(Arrival::getRemainingMinutes)));
                    return grouped;
                })
                .doOnNext(grouped -> {
                    if (cache != null) cache.put(cacheKey, grouped); // 결과를 Redis에 저장
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
