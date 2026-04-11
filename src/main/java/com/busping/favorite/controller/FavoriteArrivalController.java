package com.busping.favorite.controller;

import com.busping.favorite.dto.FolderArrivalResponse;
import com.busping.favorite.service.FavoriteArrivalService;
import com.busping.global.common.SuccessResponse;
import com.busping.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites/arrival")
public class FavoriteArrivalController {

    private final FavoriteArrivalService favoriteArrivalService;

    /**
     * 폴더 내 즐겨찾기 도착정보 조회 (거리 필터 없음)
     * - 폴더에 등록된 모든 즐겨찾기의 실시간 도착정보를 반환한다.
     * - TAGO API를 직접 호출하며 Redis 캐시를 사용하지 않는다.
     */
    @GetMapping("/{folderId}/realtime")
    public ResponseEntity<SuccessResponse<FolderArrivalResponse>> getFolderRealtime(
            @PathVariable Long folderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        FolderArrivalResponse response =
                favoriteArrivalService.getArrivalsByFolder(
                        userDetails.getId(),
                        folderId
                );

        return SuccessResponse.of(
                HttpStatus.OK,
                "폴더 실시간 도착정보 조회 성공",
                response
        );
    }

    
    /**
     * 폴더 내 즐겨찾기 도착정보 조회 (유저 위치 기준 500m 이내만)
     * - 유저의 현재 위치에서 500m 이내인 즐겨찾기만 필터링하여 도착정보를 반환한다.
     * - ArrivalService를 경유하므로 Redis 캐시가 적용된다.
     * - 유저 위치(위경도)가 설정되지 않은 경우 400 에러를 반환한다.
     */
    @GetMapping("/{folderId}/nearby")
    public ResponseEntity<SuccessResponse<FolderArrivalResponse>> getNearbyFolderRealtime(
            @PathVariable Long folderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        FolderArrivalResponse response =
                favoriteArrivalService.getNearbyArrivalsByFolder(
                        userDetails.getId(),
                        folderId
                );

        return SuccessResponse.of(
                HttpStatus.OK,
                "폴더 근처 실시간 도착정보 조회 성공",
                response
        );
    }
}
