package com.busping.arrival.service;

import com.busping.arrival.domain.Arrival;
import com.busping.arrival.dto.ArrivalItem;
import com.busping.arrival.dto.RouteArrivalResponse;
import com.busping.arrival.dto.StationArrivalResponse;
import com.busping.global.external.tago.arrival.TagoArrivalPort;
import com.busping.global.external.tago.arrival.TagoArrivalWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArrivalService {

    static final String CACHE_NAME = "arrivals";

    private final TagoArrivalPort tagoArrivalClient;
    private final TagoArrivalWebClient tagoArrivalWebClient;
    private final CacheManager cacheManager;

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

    public Mono<Map<String, List<Arrival>>> getGroupedArrivalsMono(String cityCode, String stationId) {
        String cacheKey = buildCacheKey(cityCode, stationId);
        Cache cache = cacheManager.getCache(CACHE_NAME);
        Map<String, List<Arrival>> cached = getCached(cache, cacheKey, cityCode, stationId, "ASYNC");

        if (cached != null) {
            return Mono.just(cached);
        }

        log.info("[ARRIVAL][REDIS MISS][ASYNC] key={}, cityCode={}, stationId={} - TAGO 호출",
                cacheKey, cityCode, stationId);

        return tagoArrivalWebClient.fetchRealtimeArrivals(cityCode, stationId)
                .map(this::groupByRoute)
                .doOnNext(grouped -> putCache(cache, cacheKey, grouped, "ASYNC"));
    }

    public Map<String, List<Arrival>> getGroupedArrivals(String cityCode, String stationId) {
        String cacheKey = buildCacheKey(cityCode, stationId);
        Cache cache = cacheManager.getCache(CACHE_NAME);
        Map<String, List<Arrival>> cached = getCached(cache, cacheKey, cityCode, stationId, "SYNC");

        if (cached != null) {
            return cached;
        }

        log.info("[ARRIVAL][REDIS MISS][SYNC] key={}, cityCode={}, stationId={} - TAGO 호출",
                cacheKey, cityCode, stationId);

        List<Arrival> arrivals = tagoArrivalClient.fetchRealtimeArrivals(cityCode, stationId);
        Map<String, List<Arrival>> grouped = groupByRoute(arrivals);
        putCache(cache, cacheKey, grouped, "SYNC");

        return grouped;
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

    @SuppressWarnings("unchecked")
    private Map<String, List<Arrival>> getCached(Cache cache,
                                                 String cacheKey,
                                                 String cityCode,
                                                 String stationId,
                                                 String path) {
        if (cache == null) {
            log.warn("[ARRIVAL][REDIS SKIP][{}] cacheName={} 없음", path, CACHE_NAME);
            return null;
        }

        Map<String, List<Arrival>> cached = cache.get(cacheKey, Map.class);
        if (cached != null) {
            log.info("[ARRIVAL][REDIS HIT][{}] key={}, cityCode={}, stationId={}, routeCount={}",
                    path, cacheKey, cityCode, stationId, cached.size());
        }

        return cached;
    }

    private void putCache(Cache cache, String cacheKey, Map<String, List<Arrival>> grouped, String path) {
        if (cache == null) {
            return;
        }

        cache.put(cacheKey, grouped);
        log.info("[ARRIVAL][REDIS PUT][{}] key={}, routeCount={}",
                path, cacheKey, grouped.size());
    }

    private String buildCacheKey(String cityCode, String stationId) {
        return cityCode + ":" + stationId;
    }
}
