package com.stationalarm.favorite.service;

import com.stationalarm.arrival.domain.Arrival;
import com.stationalarm.favorite.domain.Favorite;
import com.stationalarm.favorite.domain.FavoriteFolder;
import com.stationalarm.favorite.dto.FolderArrivalResponse;
import com.stationalarm.favorite.domain.FavoriteFolderRepository;
import com.stationalarm.favorite.domain.FavoriteRepository;
import com.stationalarm.global.exception.custom.BusinessException;
import com.stationalarm.global.exception.errorcode.CommonErrorCode;
import com.stationalarm.global.external.tago.arrival.TagoArrivalClient;
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

    private final FavoriteRepository favoriteRepository;
    private final FavoriteFolderRepository favoriteFolderRepository;
    private final TagoArrivalClient tagoArrivalClient;

    /**
     * 폴더 기준 즐겨찾기 도착정보 조회
     *
     * 처리 흐름:
     * 1. 폴더 소유권 검증
     * 2. 폴더 내 Favorite 조회
     * 3. 정류장 기준 그룹핑 후 도착정보 조회 (공통 메서드 사용)
     * 4. 응답 DTO로 조립
     */
    public FolderArrivalResponse getArrivalsByFolder(Long userId, Long folderId) {

        // 1️⃣ 폴더 소유권 검증
        FavoriteFolder folder = favoriteFolderRepository
                .findByIdAndUser_Id(folderId, userId)
                .orElseThrow(() ->
                        new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
                );

        // 2️⃣ 폴더 내 즐겨찾기 조회
        List<Favorite> favorites =
                favoriteRepository.findAllByFolder_Id(folder.getId());

        // 즐겨찾기 없는 경우 빈 응답 반환
        if (favorites.isEmpty()) {
            return new FolderArrivalResponse(
                    folder.getId(),
                    folder.getName(),
                    List.of()
            );
        }

        // 3️⃣ 정류장 단위 도착정보 조회 (공통 로직)
        Map<String, StationArrivalGroup> stationMap =
                getArrivalsGroupedByStation(favorites);

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

                routes.add(
                        new FolderArrivalResponse.RouteArrival(
                                routeData.getFavorite().getId(),
                                routeData.getFavorite().getRouteId(),
                                routeData.getFavorite().getRouteName(),
                                arrivalInfos
                        )
                );
            }

            stationGroups.add(
                    new FolderArrivalResponse.StationGroup(
                            stationGroup.getSample().getStationId(),
                            stationGroup.getSample().getStationName(),
                            routes
                    )
            );
        }

        return new FolderArrivalResponse(
                folder.getId(),
                folder.getName(),
                stationGroups
        );
    }


    /**
     * 공통 로직:
     * Favorite 목록을 받아
     * 정류장 기준으로 그룹핑 후
     * 정류장당 1회만 API 호출하여
     * routeId 기준으로 도착정보 필터링한다.
     *
     * 반환 구조:
     *   key = regionCode_stationId
     *   value = StationArrivalGroup
     */
    private Map<String, StationArrivalGroup> getArrivalsGroupedByStation(List<Favorite> favorites) {

        if (favorites.isEmpty()) {
            return Map.of();
        }

        // 1️⃣ 정류장 기준 그룹핑
        Map<String, List<Favorite>> grouped =
                favorites.stream()
                        .collect(Collectors.groupingBy(
                                f -> f.getCityCode() + "_" + f.getStationId()
                        ));

        Map<String, StationArrivalGroup> result = new LinkedHashMap<>();

        // 2️⃣ 정류장별 1회 호출
        for (Map.Entry<String, List<Favorite>> entry : grouped.entrySet()) {

            List<Favorite> group = entry.getValue();
            Favorite sample = group.get(0);

            // 외부 API 호출 (정류장당 1회)
            List<Arrival> arrivals =
                    tagoArrivalClient.fetchRealtimeArrivals(
                            sample.getCityCode(),
                            sample.getStationId()
                    );

            List<RouteArrivalData> routeDataList = new ArrayList<>();

            // routeId 기준 필터링
            for (Favorite favorite : group) {

                List<Arrival> filtered =
                        arrivals.stream()
                                .filter(a -> a.getRouteId().equals(favorite.getRouteId()))
                                .toList();

                routeDataList.add(
                        new RouteArrivalData(favorite, filtered)
                );
            }

            result.put(
                    entry.getKey(),
                    new StationArrivalGroup(sample, routeDataList)
            );
        }

        return result;
    }


    /* ============================================================
       ==========  내부 전용 DTO (Service 내부에서만 사용)  ==========
       ============================================================ */

    /**
     * 정류장 단위 그룹 데이터
     * sample: 해당 정류장의 대표 Favorite (stationId, stationName 사용용)
     * routes: 해당 정류장에 속한 노선별 도착정보
     */
    @Getter
    @AllArgsConstructor
    private static class StationArrivalGroup {
        private Favorite sample;
        private List<RouteArrivalData> routes;
    }

    /**
     * 노선 단위 도착정보 데이터
     * favorite: 원본 Favorite 엔티티
     * arrivals: 해당 노선의 도착정보 리스트
     */
    @Getter
    @AllArgsConstructor
    private static class RouteArrivalData {
        private Favorite favorite;
        private List<Arrival> arrivals;
    }
}