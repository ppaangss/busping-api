package com.busping.favorite.service;

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
import com.busping.global.util.DistanceUtils;
import com.busping.user.domain.User;
import com.busping.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteArrivalService {

    private static final int RADIUS_METERS = 500;

    private final FavoriteRepository favoriteRepository;
    private final FavoriteFolderRepository favoriteFolderRepository;
    private final UserRepository userRepository;
    private final ArrivalService arrivalService;

    public FolderArrivalResponse getArrivalsByFolder(Long userId, Long folderId) {
        FavoriteFolder folder = favoriteFolderRepository
                .findByIdAndUser_Id(folderId, userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        List<Favorite> favorites = favoriteRepository.findAllByFolder_Id(folder.getId());
        if (favorites.isEmpty()) {
            return new FolderArrivalResponse(folder.getId(), folder.getName(), List.of());
        }

        return new FolderArrivalResponse(
                folder.getId(),
                folder.getName(),
                buildStationGroups(favorites)
        );
    }

    public FolderArrivalResponse getNearbyArrivalsByFolder(Long userId, Long folderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        FavoriteFolder folder = favoriteFolderRepository
                .findByIdAndUser_Id(folderId, userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        List<Favorite> favorites = favoriteRepository.findAllByFolder_Id(folder.getId());
        if (favorites.isEmpty()) {
            return new FolderArrivalResponse(folder.getId(), folder.getName(), List.of());
        }

        if (user.getLatitude() == null || user.getLongitude() == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        List<Favorite> nearbyFavorites = favorites.stream()
                .filter(f -> DistanceUtils.calculateDistance(
                        user.getLatitude(), user.getLongitude(),
                        f.getLatitude(), f.getLongitude()
                ) <= RADIUS_METERS)
                .toList();

        if (nearbyFavorites.isEmpty()) {
            return new FolderArrivalResponse(folder.getId(), folder.getName(), List.of());
        }

        return new FolderArrivalResponse(
                folder.getId(),
                folder.getName(),
                buildStationGroups(nearbyFavorites)
        );
    }

    private List<FolderArrivalResponse.StationGroup> buildStationGroups(List<Favorite> favorites) {
        Map<String, List<Favorite>> grouped = favorites.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getCityCode() + "_" + f.getStationId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<FolderArrivalResponse.StationGroup> result = new ArrayList<>();

        for (List<Favorite> group : grouped.values()) {
            Favorite sample = group.get(0);
            StationArrivalResponse stationResponse = arrivalService.getGroupedArrivalsResponse(
                    sample.getCityCode(), sample.getStationId());

            Map<String, RouteArrivalResponse> routeMap = stationResponse.routes().stream()
                    .collect(Collectors.toMap(RouteArrivalResponse::routeId, route -> route));

            List<FolderArrivalResponse.RouteArrival> routes = new ArrayList<>();

            for (Favorite favorite : group) {
                RouteArrivalResponse routeArrival = routeMap.get(favorite.getRouteId());
                List<FolderArrivalResponse.ArrivalInfo> arrivalInfos = routeArrival == null
                        ? List.of()
                        : routeArrival.arrivals().stream()
                                .map(arrival -> new FolderArrivalResponse.ArrivalInfo(
                                        arrival.remainingMinutes(),
                                        arrival.remainingStops()
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
}
