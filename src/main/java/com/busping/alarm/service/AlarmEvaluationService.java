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
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

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

    /** 손절선 TTL - 접수 후 이 시간이 지난 이벤트는 배달해도 가치가 없다 (가치 곡선의 0점, 02/수치.md) */
    private static final Duration EVENT_TTL = Duration.ofSeconds(30);

    private final FavoriteRepository favoriteRepository;
    private final ArrivalService arrivalService;
    private final AlarmCooldownManager cooldownManager;
    private final FcmPort fcmService;
    private final MeterRegistry meterRegistry;

    /**
     * 비동기 진입점 - 호출은 즉시 반환되고, 평가는 alarmExecutor 워커에서 실행된다.
     *
     * 호출자 계약: receivedAt은 반드시 "제출하는 순간"의 Instant.now()여야 한다.
     * (이 메서드 안에서 찍으면 이미 워커 실행 중이라 큐 대기 시간이 빠져 TTL이 무의미해짐)
     */
    @Async("alarmExecutor")
    public void evaluateAsync(Device device, double latitude, double longitude, Instant receivedAt) {

        // 손절선 - 큐에서 늙어버린 이벤트는 평가 없이 버린다 (유저는 이미 직접 앱을 열었다)
        if (Duration.between(receivedAt, Instant.now()).compareTo(EVENT_TTL) > 0) {
            meterRegistry.counter("alarm.stale.skipped").increment();
            return;
        }

        try {
            evaluate(device, latitude, longitude);
        } catch (Exception e) {
            // @Async 메서드의 예외는 호출자에게 돌아가지 않는다 - 여기서 삼키지 않으면 증발
            log.error("[ALARM] 비동기 평가 실패 - {}", e.getMessage(), e);
        }
    }

    // 외부 호출(TAGO·FCM)이 낀 플로우 - 트랜잭션으로 묶지 않아 커넥션 점유를 쿼리 순간으로 제한
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
