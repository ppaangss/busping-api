package com.busping.favorite.service;

import com.busping.arrival.domain.Arrival;
import com.busping.arrival.dto.RouteArrivalResponse;
import com.busping.arrival.dto.StationArrivalResponse;
import com.busping.arrival.service.ArrivalService;
import com.busping.favorite.domain.Favorite;
import com.busping.favorite.domain.FavoriteFolder;
import com.busping.favorite.domain.FavoriteFolderRepository;
import com.busping.favorite.domain.FavoriteRepository;
import com.busping.favorite.dto.FolderArrivalResponse;
import com.busping.global.exception.custom.BusinessException;
import com.busping.global.exception.errorcode.CommonErrorCode;
import com.busping.global.external.tago.arrival.TagoArrivalClient;
import com.busping.global.util.DistanceUtils;
import com.busping.user.domain.User;
import com.busping.user.domain.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteArrivalService {

    private static final int RADIUS_METERS = 500;

    private final FavoriteRepository favoriteRepository;
    private final FavoriteFolderRepository favoriteFolderRepository;
    private final TagoArrivalClient tagoArrivalClient;
    private final UserRepository userRepository;
    private final ArrivalService arrivalService;

    /**
     * 폴더 기준 즐겨찾기 도착정보 조회 (거리 필터 없음)
     *
     * 처리 흐름:
     * 1. 폴더 소유권 검증
     * 2. 폴더 내 Favorite 조회
     * 3. 정류장 기준 그룹핑 후 도착정보 조회
     * 4. 응답 DTO로 조립
     */
    public FolderArrivalResponse getArrivalsByFolder(Long userId, Long folderId) {

        // 1️⃣ 폴더 소유권 검증
        FavoriteFolder folder = favoriteFolderRepository
                .findByIdAndUser_Id(folderId, userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        // 2️⃣ 폴더 내 즐겨찾기 조회
        List<Favorite> favorites = favoriteRepository.findAllByFolder_Id(folder.getId());

        if (favorites.isEmpty()) {
            return new FolderArrivalResponse(folder.getId(), folder.getName(), List.of());
        }

        // 3️⃣ 정류장 단위 도착정보 조회
        Map<String, StationArrivalGroup> stationMap = getArrivalsGroupedByStation(favorites);

        // 4️⃣ 응답 DTO 조립
        List<FolderArrivalResponse.StationGroup> stationGroups = new ArrayList<>();

        for (StationArrivalGroup stationGroup : stationMap.values()) {
            List<FolderArrivalResponse.RouteArrival> routes = new ArrayList<>();

            for (RouteArrivalData routeData : stationGroup.getRoutes()) {
                List<FolderArrivalResponse.ArrivalInfo> arrivalInfos =
                        routeData.getArrivals().stream()
                                .map(a -> new FolderArrivalResponse.ArrivalInfo(
                                        a.getRemainingMinutes(),
                                        a.getRemainingStops()
                                ))
                                .toList();

                routes.add(new FolderArrivalResponse.RouteArrival(
                        routeData.getFavorite().getId(),
                        routeData.getFavorite().getRouteId(),
                        routeData.getFavorite().getRouteName(),
                        arrivalInfos
                ));
            }

            stationGroups.add(new FolderArrivalResponse.StationGroup(
                    stationGroup.getSample().getStationId(),
                    stationGroup.getSample().getStationName(),
                    routes
            ));
        }

        return new FolderArrivalResponse(folder.getId(), folder.getName(), stationGroups);
    }

    /**
     * 폴더 기준 즐겨찾기 도착정보 조회 (유저 위치 기준 500m 이내만)
     *
     * 처리 흐름:
     * 1. 유저 조회 (위치 정보 필요)
     * 2. 폴더 소유권 검증
     * 3. 폴더 내 즐겨찾기 조회
     * 4. 500m 이내 필터링
     * 5. 정류장 기준 그룹핑 후 도착정보 조회 (ArrivalService 캐시 활용)
     * 6. 응답 DTO 조립
     */
    public FolderArrivalResponse getNearbyArrivalsByFolder(Long userId, Long folderId) {

        // 1. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        // 2. 폴더 소유권 검증
        FavoriteFolder folder = favoriteFolderRepository
                .findByIdAndUser_Id(folderId, userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        // 3. 폴더 내 즐겨찾기 조회
        List<Favorite> favorites = favoriteRepository.findAllByFolder_Id(folder.getId());

        if (favorites.isEmpty()) {
            return new FolderArrivalResponse(folder.getId(), folder.getName(), List.of());
        }

        // 4. 유저 위치 미설정 시 에러
        if (user.getLatitude() == null || user.getLongitude() == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        // 5. 500m 이내 즐겨찾기만 필터링
        List<Favorite> nearbyFavorites = favorites.stream()
                .filter(f -> DistanceUtils.calculateDistance(
                        user.getLatitude(), user.getLongitude(),
                        f.getLatitude(), f.getLongitude()
                ) <= RADIUS_METERS)
                .toList();

        if (nearbyFavorites.isEmpty()) {
            return new FolderArrivalResponse(folder.getId(), folder.getName(), List.of());
        }

        // 6. 정류장 기준 도착정보 조회 후 응답 DTO 조립
        return new FolderArrivalResponse(
                folder.getId(),
                folder.getName(),
                buildNearbyStationGroups(nearbyFavorites)
        );
    }

    /**
     * 기존 방식: TagoArrivalClient 직접 호출 (캐시 미적용)
     */
    private Map<String, StationArrivalGroup> getArrivalsGroupedByStation(List<Favorite> favorites) {

        Map<String, List<Favorite>> grouped = favorites.stream()
                .collect(Collectors.groupingBy(f -> f.getCityCode() + "_" + f.getStationId()));

        Map<String, StationArrivalGroup> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<Favorite>> entry : grouped.entrySet()) {
            List<Favorite> group = entry.getValue();
            Favorite sample = group.get(0);

            List<Arrival> arrivals = tagoArrivalClient.fetchRealtimeArrivals(
                    sample.getCityCode(), sample.getStationId());

            List<RouteArrivalData> routeDataList = new ArrayList<>();

            for (Favorite favorite : group) {
                List<Arrival> filtered = arrivals.stream()
                        .filter(a -> a.getRouteId().equals(favorite.getRouteId()))
                        .toList();
                routeDataList.add(new RouteArrivalData(favorite, filtered));
            }

            result.put(entry.getKey(), new StationArrivalGroup(sample, routeDataList));
        }

        return result;
    }

    /**
     * 근처 전용: ArrivalService 경유 (Redis 캐시 적용)
     */
    private List<FolderArrivalResponse.StationGroup> buildNearbyStationGroups(List<Favorite> favorites) {

        Map<String, List<Favorite>> grouped = favorites.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getCityCode() + "_" + f.getStationId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<FolderArrivalResponse.StationGroup> result = new ArrayList<>();

        for (List<Favorite> group : grouped.values()) {
            Favorite sample = group.get(0);

            // 캐시 히트 시 Redis에서 즉시 반환, 정류장당 1회 호출
            StationArrivalResponse stationResponse = arrivalService.getGroupedArrivalsResponse(
                    sample.getCityCode(), sample.getStationId());

            // routeId → RouteArrivalResponse 맵
            Map<String, RouteArrivalResponse> routeMap = stationResponse.routes().stream()
                    .collect(Collectors.toMap(RouteArrivalResponse::routeId, r -> r));

            List<FolderArrivalResponse.RouteArrival> routes = new ArrayList<>();

            for (Favorite favorite : group) {
                RouteArrivalResponse routeArrival = routeMap.get(favorite.getRouteId());

                List<FolderArrivalResponse.ArrivalInfo> arrivalInfos = routeArrival == null
                        ? List.of()
                        : routeArrival.arrivals().stream()
                                .map(a -> new FolderArrivalResponse.ArrivalInfo(
                                        a.remainingMinutes(),
                                        a.remainingStops()
                                ))
                                .toList();

                routes.add(new FolderArrivalResponse.RouteArrival(
                        favorite.getId(),
                        favorite.getRouteId(),
                        favorite.getRouteName(),
                        arrivalInfos
                ));
            }

            result.add(new FolderArrivalResponse.StationGroup(
                    sample.getStationId(),
                    sample.getStationName(),
                    routes
            ));
        }

        return result;
    }

    @Getter
    @AllArgsConstructor
    private static class StationArrivalGroup {
        private Favorite sample;
        private List<RouteArrivalData> routes;
    }

    @Getter
    @AllArgsConstructor
    private static class RouteArrivalData {
        private Favorite favorite;
        private List<Arrival> arrivals;
    }
}
