package com.station.alarm.favorite.service;

import com.station.alarm.favorite.domain.Favorite;
import com.station.alarm.favorite.domain.FavoriteRepository;
import com.station.alarm.favorite.dto.FavoriteCreateRequest;
import com.station.alarm.global.exception.custom.BusinessException;
import com.station.alarm.global.exception.errorcode.CommonErrorCode;
import com.station.alarm.user.domain.User;
import com.station.alarm.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    /**
     * 노선 기반 즐겨찾기 등록
     */
    public void addFavorite(Long userId, FavoriteCreateRequest request) {

        if (request == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
                );

        boolean exists = favoriteRepository.existsByUserAndStationIdAndRouteId(
                user,
                request.getStationId(),
                request.getRouteId()
        );

        if (exists) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE);
        }

        Favorite favorite = new Favorite(
                user,
                request.getStationId(),
                request.getStationName(),
                request.getRouteId(),
                request.getBusNumber(),
                request.getRegionCode()
        );

        favoriteRepository.save(favorite);
    }

//    /**
//     * 즐겨찾기 목록 조회
//     */
//    @Transactional(readOnly = true)
//    public List<FavoriteResponse> getFavorites(Long userId) {
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() ->
//                        new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
//                );
//
//        return favoriteRepository.findAllByUser(user)
//                .stream()
//                .map(FavoriteResponse::from)
//                .toList();
//    }
//
//    /**
//     * 즐겨찾기 삭제
//     */
//    public void deleteFavorite(Long userId, String stationId, String routeId) {
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() ->
//                        new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
//                );
//
//        Favorite favorite = favoriteRepository
//                .findByUserAndStationIdAndRouteId(user, stationId, routeId)
//                .orElseThrow(() ->
//                        new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
//                );
//
//        favoriteRepository.delete(favorite);
//    }
}