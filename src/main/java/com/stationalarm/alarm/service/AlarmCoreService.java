package com.stationalarm.alarm.service;

import com.stationalarm.alarm.AlarmCooldownManager;
import com.stationalarm.alarm.domain.AlarmCandidate;
import com.stationalarm.arrival.domain.Arrival;
import com.stationalarm.arrival.domain.StationKey;
import com.stationalarm.arrival.service.ArrivalService;
import com.stationalarm.favorite.domain.Favorite;
import com.stationalarm.favorite.domain.FavoriteRepository;
import com.stationalarm.global.util.DistanceUtils;
import com.stationalarm.user.domain.User;
import com.stationalarm.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    // 상수
    // 실제 활성화 시간은 5분차이
    // 테스트를 위해 길게 잡음
    private static final int ACTIVE_MINUTES = 100;
    private static final int RADIUS_METERS = 500;
    
    // 의존성
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final ArrivalService arrivalService;
    private final AlarmCooldownManager cooldownManager;

    public void runCycle(){

        log.info("[ALARM] runCycle start");
        // 1. 활성 유저 ID 추출

        // 서버 시간 문제 생길 수 있음.
        LocalDateTime activeSince =
                LocalDateTime.now().minusMinutes(ACTIVE_MINUTES);

        List<User> activeUsers =
                userRepository.findByLastLocationUpdatedAtAfter(activeSince);

        log.info("[ALARM] 활성 유저 수 = {}", activeUsers.size());

        if (activeUsers.isEmpty()) {
            log.info("[ALARM] 처리할 활성 유저 없음. 사이클 종료.");
            return;
        }

        // 2. 활성 유저의 즐겨찾기 조회
        
        // 활성 유저들의 Id 리스트 추출
        List<Long> userIds = activeUsers.stream()
                .map(User::getId)
                .toList();
        
        // 활성 유저들의 즐겨찾기 리스트 조회
        List<Favorite> favorites =
                favoriteRepository.findByUserIds(userIds);

        log.info("[ALARM] 조회된 즐겨찾기 수 = {}", favorites.size());

        // userId 기준 그룹핑
        Map<Long, List<Favorite>> favoritesByUser =
                favorites.stream()
                        .collect(Collectors.groupingBy(
                                f -> f.getFolder().getUser().getId()
                        ));

        // 3. 거리 반경에 맞는 즐겨찾기 목록 리스트 추출

        // 유저별 500m 이내 즐겨찾기 리스트
        Map<Long, List<Favorite>> nearbyFavoritesByUser = new HashMap<>();
        
        for (User user : activeUsers) {

            Long userId = user.getId();

            List<Favorite> userFavorites =
                    favoritesByUser.getOrDefault(userId, Collections.emptyList());

            if (userFavorites.isEmpty()) {
                continue;
            }

            List<Favorite> nearbyFavorites = new ArrayList<>();

            for (Favorite favorite : userFavorites) {

                double distance = DistanceUtils.calculateDistance(
                        user.getLatitude(),
                        user.getLongitude(),
                        favorite.getLatitude(),
                        favorite.getLongitude()
                );

                if (distance <= RADIUS_METERS) {
                    nearbyFavorites.add(favorite);
                }
            }

            if (!nearbyFavorites.isEmpty()) {
                nearbyFavoritesByUser.put(userId, nearbyFavorites);
            }
        }

        log.info("[ALARM] 500m 이내 즐겨찾기 유저 수 = {}", nearbyFavoritesByUser.size());

        // 4. 정류장 기준으로 집합 생성
        Set<StationKey> stationKeys = new HashSet<>();

        for (List<Favorite> userFavorites : nearbyFavoritesByUser.values()) {
            for (Favorite f : userFavorites) {
                stationKeys.add(new StationKey(
                        f.getCityCode(),
                        f.getStationId()
                ));
            }
        }

        // 5. 정류장 기준으로 도착 정보 조회
        
        // String: routeId, List<Arrival>: 도착정보 리스트
        Map<StationKey, Map<String, List<Arrival>>> arrivalsByStation = new HashMap<>();

        for (StationKey key : stationKeys) {

            try {

                Map<String, List<Arrival>> grouped =
                        arrivalService.getGroupedArrivals(
                                key.cityCode(),
                                key.stationId()
                        );

                arrivalsByStation.put(key, grouped);

            } catch (Exception e) {
                // 예외가 발생할 경우 runCycle() 전체를 종료하는것이 아닌 해당 조회 실패 하나만 격리하기.
                log.error("[ALARM] arrival 조회 실패 stationId={}",
                        key.stationId(), e);

            }
        }

        // 6. 유저와 도착 정보 매칭

        // 알람 후보 객체 리스트
        List<AlarmCandidate> candidates = new ArrayList<>();

        // 알람 후보 객체 중복 제거 집합
        Set<String> candidateKeys = new HashSet<>();

        for (Map.Entry<Long, List<Favorite>> entry : nearbyFavoritesByUser.entrySet()) {

            Long userId = entry.getKey();
            List<Favorite> userFavorites = entry.getValue();

            for (Favorite favorite : userFavorites) {

                // candidate dedup key 생성
                String candidateKey = userId + ":" +
                        favorite.getCityCode() + ":" +
                        favorite.getStationId() + ":" +
                        favorite.getRouteId();

                // 이미 생성된 candidate이면 skip
                if (!candidateKeys.add(candidateKey)) {
                    continue;
                }

                // 유저의 Favorite 중 StationKey 추출
                StationKey key = new StationKey(
                        favorite.getCityCode(),
                        favorite.getStationId()
                );

                // StationKey 가지고 도착 정보 조회
                Map<String, List<Arrival>> stationArrivals =
                        arrivalsByStation.get(key);

                if (stationArrivals == null) continue;

                // 도착 정보 리스트 중 즐겨찾기의 route인거 찾기
                List<Arrival> routeArrivals =
                        stationArrivals.get(favorite.getRouteId());

                if (routeArrivals == null || routeArrivals.isEmpty()) continue;

                // route 내부는 이미 도착시간 오름차순 정렬됨
                Arrival firstArrival = routeArrivals.get(0);

                log.info("[ALARM] 알림 후보 userId={}, routeId={}, 남은시간={}분",
                        userId,
                        favorite.getRouteId(),
                        firstArrival.getRemainingMinutes()
                );

                AlarmCandidate candidate = new AlarmCandidate(
                        userId,
                        favorite.getCityCode(),
                        favorite.getStationId(),
                        favorite.getStationName(),
                        favorite.getRouteId(),
                        firstArrival.getBusNumber(),
                        firstArrival.getRemainingMinutes(),
                        firstArrival.getRemainingStops()
                );

                candidates.add(candidate);
            }
        }

        log.info("[ALARM] 생성된 알림 후보 수 = {}", candidates.size());

        // 7. 알람 후보 쿨타임 락에 등록 후 알람 전송
        for (AlarmCandidate candidate : candidates) {

            // 쿨타임 락에 등록된 알람인지 검사
            boolean acquired = cooldownManager.tryAcquireCooldown(
                    candidate.userId(),
                    candidate.cityCode(),
                    candidate.stationId(),
                    candidate.routeId()
            );

            // 쿨타임 락에 등록된 후보자인 경우 알람 안보내기
            if (!acquired) {
                log.info("[ALARM] cooldown skip userId={} routeId={} stationId={}",
                        candidate.userId(),
                        candidate.routeId(),
                        candidate.stationId());
                continue;
            }

            // 실제 알람 발송 대신 로그
            log.info("[ALARM SEND] userId={} station={} route={} bus={} remaining={}min stops={}",
                    candidate.userId(),
                    candidate.stationName(),
                    candidate.routeId(),
                    candidate.busNumber(),
                    candidate.remainingMinutes(),
                    candidate.remainingStops()
            );

            // sendAlarm(candidate);
        }

        log.info("[ALARM] runCycle end");
    }

    /**
     * 활성 유저 조회 (5분 기준)
     *
     * 유저별 500m 내 stationId 수집 (중복 제거)
     *
     * stationId별 도착정보 조회 (Redis 캐시 → 없으면 TAGO)
     *
     * 유저별 즐겨찾기 로딩
     *
     * 유저×즐겨찾기 매칭하여 “알림 후보” 생성
     *
     * 조건 검사 + 쿨타임 검사(10분 Redis)
     *
     * FCM 발송
     *
     * 알림 기록 저장
     */




}
