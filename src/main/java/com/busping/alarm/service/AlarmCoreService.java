package com.busping.alarm.service;

import com.busping.alarm.AlarmCooldownManager;
import com.busping.alarm.domain.AlarmCandidate;
import com.busping.arrival.domain.Arrival;
import com.busping.arrival.domain.StationKey;
import com.busping.arrival.service.ArrivalService;
import com.busping.favorite.domain.Favorite;
import com.busping.favorite.domain.FavoriteRepository;
import com.busping.global.external.fcm.FcmService;
import com.busping.global.util.DistanceUtils;
import com.busping.user.domain.User;
import com.busping.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 스케줄러가 30초마다 호출하는 한 번의 배치 사이클
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmCoreService {

    // 실제 활성화 시간은 5분, 테스트를 위해 길게 잡음
    private static final int ACTIVE_MINUTES = 100;
    private static final int RADIUS_METERS = 500;

    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final ArrivalService arrivalService;
    private final AlarmCooldownManager cooldownManager;
    private final FcmService fcmService;

    /**
     * 배치 사이클 진입점 — 각 단계를 순서대로 호출하는 오케스트레이터
     */
    public void runCycle() {

        Instant start = Instant.now();
        log.info("[ALARM] runCycle start");

        // 활성 유저 조회
        List<User> activeUsers = findActiveUsers();
        if (activeUsers.isEmpty()) {
            log.info("[ALARM] 처리할 활성 유저 없음. 사이클 종료.");
            return;
        }

        // 활성유저의 즐겨찾기 조회
        Map<Long, List<Favorite>> nearbyFavoritesByUser = findNearbyFavorites(activeUsers);
        if (nearbyFavoritesByUser.isEmpty()) {
            log.info("[ALARM] 500m 이내 즐겨찾기 없음. 사이클 종료.");
            return;
        }

        //
        Map<StationKey, Map<String, List<Arrival>>> arrivalsByStation = fetchArrivals(nearbyFavoritesByUser);

        List<AlarmCandidate> candidates = buildCandidates(nearbyFavoritesByUser, arrivalsByStation);

        sendAlarms(candidates);

        long elapsed = Duration.between(start, Instant.now()).toMillis();
        log.info("[ALARM] runCycle end - 소요시간 = {}ms", elapsed);
    }

    /**
     * 최근 ACTIVE_MINUTES 이내에 위치를 업데이트한 활성 유저 조회
     */
    private List<User> findActiveUsers() {
        LocalDateTime activeSince = LocalDateTime.now().minusMinutes(ACTIVE_MINUTES);
        List<User> activeUsers = userRepository.findByLastLocationUpdatedAtAfter(activeSince);
        log.info("[ALARM] 활성 유저 수 = {}", activeUsers.size());
        return activeUsers;
    }

    /**
     * 활성 유저의 즐겨찾기를 조회하고, 현재 위치 기준 RADIUS_METERS 이내인 것만 필터링
     *
     * @return userId → 500m 이내 즐겨찾기 리스트
     */
    private Map<Long, List<Favorite>> findNearbyFavorites(List<User> activeUsers) {

        List<Long> userIds = activeUsers.stream()
                .map(User::getId)
                .toList();

        List<Favorite> favorites = favoriteRepository.findByUserIds(userIds);
        log.info("[ALARM] 조회된 즐겨찾기 수 = {}", favorites.size());

        // userId 기준 그룹핑
        Map<Long, List<Favorite>> favoritesByUser = favorites.stream()
                .collect(Collectors.groupingBy(f -> f.getFolder().getUser().getId()));

        // 유저별로 500m 이내 즐겨찾기만 추출
        Map<Long, List<Favorite>> nearbyFavoritesByUser = new HashMap<>();

        for (User user : activeUsers) {
            List<Favorite> userFavorites = favoritesByUser.getOrDefault(user.getId(), Collections.emptyList());
            if (userFavorites.isEmpty()) continue;

            List<Favorite> nearbyFavorites = userFavorites.stream()
                    .filter(f -> DistanceUtils.calculateDistance(
                            user.getLatitude(), user.getLongitude(),
                            f.getLatitude(), f.getLongitude()
                    ) <= RADIUS_METERS)
                    .toList();

            if (!nearbyFavorites.isEmpty()) {
                nearbyFavoritesByUser.put(user.getId(), nearbyFavorites);
            }
        }

        log.info("[ALARM] 500m 이내 즐겨찾기 유저 수 = {}", nearbyFavoritesByUser.size());
        return nearbyFavoritesByUser;
    }

    /**
     * 근처 즐겨찾기에서 unique 정류장 키를 추출하고, WebClient로 병렬 도착정보 조회
     *
     * @return StationKey → (routeId → 도착정보 리스트)
     */
    private Map<StationKey, Map<String, List<Arrival>>> fetchArrivals(Map<Long, List<Favorite>> nearbyFavoritesByUser) {

        // 중복 제거된 unique 정류장 집합
        Set<StationKey> stationKeys = new HashSet<>();
        for (List<Favorite> userFavorites : nearbyFavoritesByUser.values()) {
            for (Favorite f : userFavorites) {
                stationKeys.add(new StationKey(f.getCityCode(), f.getStationId()));
            }
        }

        log.info("[ALARM] unique 정류장 수 = {}", stationKeys.size());

        // Flux.flatMap: 각 정류장에 대해 Mono를 동시에 구독 → 모든 API 요청이 동시에 출발
        // .block(): 모든 결과가 모일 때까지 대기 (배치 사이클이므로 허용)
        return Flux.fromIterable(stationKeys)
                .flatMap(key ->
                        arrivalService.getGroupedArrivalsMono(key.cityCode(), key.stationId())
                                .map(grouped -> Map.entry(key, grouped))
                                .onErrorResume(e -> {
                                    log.error("[ALARM] arrival 조회 실패 stationId={}", key.stationId(), e);
                                    return Mono.empty();
                                })
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .block();
    }

    /**
     * 유저별 즐겨찾기와 도착정보를 매칭하여 알람 후보 리스트 생성
     * 동일한 (userId, cityCode, stationId, routeId) 조합은 중복 제거
     *
     * @return 알람을 보내야 할 후보 리스트
     */
    private List<AlarmCandidate> buildCandidates(
            Map<Long, List<Favorite>> nearbyFavoritesByUser,
            Map<StationKey, Map<String, List<Arrival>>> arrivalsByStation
    ) {
        List<AlarmCandidate> candidates = new ArrayList<>();
        Set<String> candidateKeys = new HashSet<>();

        for (Map.Entry<Long, List<Favorite>> entry : nearbyFavoritesByUser.entrySet()) {
            Long userId = entry.getKey();

            for (Favorite favorite : entry.getValue()) {

                // 중복 후보 제거
                String candidateKey = userId + ":" + favorite.getCityCode() + ":" +
                        favorite.getStationId() + ":" + favorite.getRouteId();
                if (!candidateKeys.add(candidateKey)) continue;

                StationKey key = new StationKey(favorite.getCityCode(), favorite.getStationId());
                Map<String, List<Arrival>> stationArrivals = arrivalsByStation.get(key);
                if (stationArrivals == null) continue;

                List<Arrival> routeArrivals = stationArrivals.get(favorite.getRouteId());
                if (routeArrivals == null || routeArrivals.isEmpty()) continue;

                // route 내부는 이미 도착시간 오름차순 정렬됨
                Arrival firstArrival = routeArrivals.get(0);

                log.info("[ALARM] 알림 후보 userId={}, routeId={}, 남은시간={}분",
                        userId, favorite.getRouteId(), firstArrival.getRemainingMinutes());

                candidates.add(new AlarmCandidate(
                        userId,
                        favorite.getCityCode(),
                        favorite.getStationId(),
                        favorite.getStationName(),
                        favorite.getRouteId(),
                        firstArrival.getBusNumber(),
                        firstArrival.getRemainingMinutes(),
                        firstArrival.getRemainingStops()
                ));
            }
        }

        log.info("[ALARM] 생성된 알림 후보 수 = {}", candidates.size());
        return candidates;
    }

    /**
     * 알람 후보별로 쿨타임을 확인하고, 쿨타임이 지난 경우에만 알람 발송
     */
    private void sendAlarms(List<AlarmCandidate> candidates) {
        for (AlarmCandidate candidate : candidates) {

            // TODO: 테스트 완료 후 쿨다운 복구
            // boolean acquired = cooldownManager.tryAcquireCooldown(...);
            // if (!acquired) { continue; }

            userRepository.findById(candidate.userId()).ifPresentOrElse(user -> {
                String fcmToken = user.getFcmToken();
                if (fcmToken == null) {
                    log.info("[ALARM] FCM 토큰 없음 skip userId={}", candidate.userId());
                    return;
                }
                try {
                    String title = candidate.busNumber() + " (" + candidate.stationName() + ")";
                    String body = candidate.remainingMinutes() + "분 후 도착 (" + candidate.remainingStops() + "정거장 전)";
                    fcmService.send(fcmToken, title, body);
                    log.info("[ALARM SEND] userId={} station={} route={} bus={} remaining={}min",
                            candidate.userId(), candidate.stationName(), candidate.routeId(),
                            candidate.busNumber(), candidate.remainingMinutes());
                } catch (Exception e) {
                    log.error("[ALARM] FCM 발송 실패 userId={}", candidate.userId(), e);
                }
            }, () -> log.warn("[ALARM] 유저 없음 userId={}", candidate.userId()));
        }
    }
}
