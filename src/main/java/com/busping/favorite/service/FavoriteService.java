package com.busping.favorite.service;

import com.busping.favorite.domain.Favorite;
import com.busping.favorite.domain.FavoriteFolder;
import com.busping.favorite.domain.FavoriteFolderRepository;
import com.busping.favorite.domain.FavoriteRepository;
import com.busping.favorite.dto.FavoriteCreateRequest;
import com.busping.favorite.dto.FavoriteResponse;
import com.busping.global.exception.custom.BusinessException;
import com.busping.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final FavoriteFolderRepository favoriteFolderRepository;

    /**
     *
     * @param userId 사용자 ID (JWT에서 추출)
     * @param folderId 폴더 ID
     * @param request 즐겨찾기(노선) 추가 요청 DTO
     */
    @Transactional
    public void createFavorite(Long userId, Long folderId, FavoriteCreateRequest request) {

        // 1. request 검증
        if (request == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        // 2. 폴더 소유권 검증
        // folderId와 userId 모두 만족해야 리소스를 반환함.
        // 만약 하나라도 일치하지 않을 경우 Optional.empty() 반환
        FavoriteFolder folder = favoriteFolderRepository
                .findByIdAndUser_Id(folderId, userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        // 3. 중복 체크 (같은 폴더 안에서 동일 정류장 + 노선)
        if (favoriteRepository.existsByFolder_IdAndStationIdAndRouteId(
                folderId,
                request.getStationId(),
                request.getRouteId()
        )) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE);
        }

        // 4. 리소스 생성
        Favorite favorite = Favorite.create(
                folder,
                request.getStationId(),
                request.getStationName(),
                request.getRegionCode(),
                request.getLatitude(),
                request.getLongitude(),
                request.getRouteId(),
                request.getRouteName()
        );

        // 5. 저장
        favoriteRepository.save(favorite);
    }

    /**
     * 노선 목록 조회
     * @param userId 사용자 Id
     * @param folderId 폴더 Id
     * @return 즐겨찾기 노선 리스트 반환
     */
    @Transactional(readOnly = true)
    public List<FavoriteResponse> getListFavorites(Long userId, Long folderId) {

        // 1. 폴더 소유권 검증
        FavoriteFolder folder = favoriteFolderRepository
                .findByIdAndUser_Id(folderId, userId)
                .orElseThrow(() ->
                        new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
                );

        // 2. 폴더 내 노선 조회 (생성 순서 기준)
        List<Favorite> favorites =
                favoriteRepository.findAllByFolder_IdOrderByIdAsc(folder.getId());

        // 3. DTO 변환
        return favorites.stream()
                .map(FavoriteResponse::from)
                .toList();
    }

    /**
     * 노선 삭제
     * @param userId     사용자 ID
     * @param folderId   폴더 ID
     * @param favoriteId 삭제할 노선 ID
     */
    @Transactional
    public void deleteFavorite(Long userId, Long folderId, Long favoriteId) {

        // 폴더 소유권 검증
        FavoriteFolder folder = favoriteFolderRepository
                .findByIdAndUser_Id(folderId, userId)
                .orElseThrow(() ->
                        new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
                );

        // 해당 폴더에 속한 노선인지 검증
        Favorite favorite = favoriteRepository
                .findByIdAndFolder_Id(favoriteId, folder.getId())
                .orElseThrow(() ->
                        new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
                );

        // 삭제
        favoriteRepository.delete(favorite);
    }
}