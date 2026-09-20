package com.busping.alarm.service;

import com.busping.alarm.AlarmCooldownManager;
import com.busping.arrival.domain.Arrival;
import com.busping.arrival.domain.StationKey;
import com.busping.arrival.service.ArrivalService;
import com.busping.device.domain.Device;
import com.busping.favorite.domain.Favorite;
import com.busping.favorite.domain.FavoriteRepository;
import com.busping.global.external.fcm.FcmPort;
import com.busping.global.util.DistanceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 위치 이벤트 1건에 대한 인라인 알람 평가.
 * 알람 on 확인 → 즐겨찾기 조회 → 500m 필터 → 쿨다운 → 도착정보 조회 → FCM 발송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmEvaluationService {

    private static final int RADIUS_METERS = 500;

    private final FavoriteRepository favoriteRepository;
    private final ArrivalService arrivalService;
    private final AlarmCooldownManager cooldownManager;
    private final FcmPort fcmService;

    @Transactional
    public void evaluate(Device device, double latitude, double longitude) {

        // 1. 알람 꺼진 디바이스는 평가하지 않는다
        if (!device.isAlarmEnabled()) {
            return;
        }

        // 2. 디바이스의 전체 즐겨찾기 중 요청 좌표 기준 500m 이내만 추린다
        List<Favorite> favorites = favoriteRepository.findAllByFolder_Device_Id(device.getId());

        List<Favorite> nearbyFavorites = favorites.stream()
                .filter(f -> DistanceUtils.calculateDistance(
                        latitude, longitude,
                        f.getLatitude(), f.getLongitude()
                ) <= RADIUS_METERS)
                .toList();

        if (nearbyFavorites.isEmpty()) {
            return;
        }

        // 3. (정류장, 노선) 조합별로 평가 - 같은 요청 안 중복 제거
        Set<String> evaluatedKeys = new HashSet<>();
        Map<StationKey, Map<String, List<Arrival>>> arrivalsByStation = new HashMap<>();

        for (Favorite favorite : nearbyFavorites) {

            String candidateKey = favorite.getCityCode() + ":" + favorite.getStationId() + ":" + favorite.getRouteId();
            if (!evaluatedKeys.add(candidateKey)) {
                continue;
            }

            // 4. 쿨다운 - TTL 안이면 도착정보 조회 없이 스킵 (TAGO 호출 절약)
            if (!cooldownManager.tryAcquireCooldown(
                    device.getId(), favorite.getCityCode(), favorite.getStationId(), favorite.getRouteId())) {
                continue;
            }

            // 5. 도착정보 조회 - 같은 정류장은 요청 안에서 1회만 조회 (동기 경로)
            StationKey stationKey = new StationKey(favorite.getCityCode(), favorite.getStationId());
            Map<String, List<Arrival>> stationArrivals =
                    arrivalsByStation.computeIfAbsent(stationKey, this::fetchArrivalsSafely);

            List<Arrival> routeArrivals = stationArrivals.get(favorite.getRouteId());
            if (routeArrivals == null || routeArrivals.isEmpty()) {
                continue;
            }

            // 6. 가장 가까운 도착(오름차순 정렬 첫 번째)으로 발송
            sendAlarm(device, favorite, routeArrivals.get(0));
        }
    }

    /** TAGO 조회 실패가 위치 이벤트 전체를 실패시키지 않도록 정류장 단위로 격리 */
    private Map<String, List<Arrival>> fetchArrivalsSafely(StationKey key) {
        try {
            return arrivalService.getGroupedArrivals(key.cityCode(), key.stationId());
        } catch (Exception e) {
            log.error("[ALARM] 도착정보 조회 실패 stationId={} - {}", key.stationId(), e.getMessage());
            return Map.of();
        }
    }

    private void sendAlarm(Device device, Favorite favorite, Arrival arrival) {

        if (device.getFcmToken() == null) {
            log.info("[ALARM] FCM 토큰 없음 - 발송 스킵");
            return;
        }

        String title = arrival.getBusNumber() + " (" + favorite.getStationName() + ")";
        String body = arrival.getRemainingMinutes() + "분 후 도착 (" + arrival.getRemainingStops() + "정거장 전)";

        fcmService.send(device.getFcmToken(), title, body);

        log.info("[ALARM SEND] station={} route={} bus={} remaining={}min",
                favorite.getStationName(), favorite.getRouteId(),
                arrival.getBusNumber(), arrival.getRemainingMinutes());
    }
}
