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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites/arrival")
public class FavoriteArrivalController {

    private final FavoriteArrivalService favoriteArrivalService;

    /**
     * 폴더 내 즐겨찾기 도착정보 조회 (거리 필터 없음)
     * - 폴더에 등록된 모든 즐겨찾기의 실시간 도착정보를 반환한다.
     * - ArrivalService를 경유해 TAGO 도착정보를 조회한다.
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
     * 폴더 내 즐겨찾기 도착정보 조회 (요청 좌표 기준 500m 이내만)
     * - 위치는 저장하지 않으므로 쿼리 파라미터로 현재 좌표를 받는다.
     * - ArrivalService를 경유해 TAGO 도착정보를 조회한다.
     * - 좌표 파라미터가 없으면 400 에러를 반환한다.
     */
    @GetMapping("/{folderId}/nearby")
    public ResponseEntity<SuccessResponse<FolderArrivalResponse>> getNearbyFolderRealtime(
            @PathVariable Long folderId,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        FolderArrivalResponse response =
                favoriteArrivalService.getNearbyArrivalsByFolder(
                        userDetails.getId(),
                        folderId,
                        latitude,
                        longitude
                );

        return SuccessResponse.of(
                HttpStatus.OK,
                "폴더 근처 실시간 도착정보 조회 성공",
                response
        );
    }
}
