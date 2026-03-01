package com.stationalarm.favorite.controller;

import com.stationalarm.favorite.dto.FavoriteCreateRequest;
import com.stationalarm.favorite.dto.FavoriteResponse;
import com.stationalarm.favorite.service.FavoriteService;
import com.stationalarm.global.common.SuccessResponse;
import com.stationalarm.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 노선 추가
     */
    @PostMapping("/{folderId}/routes")
    public ResponseEntity<SuccessResponse<Void>> addRoute(
            @PathVariable Long folderId,
            @Valid @RequestBody FavoriteCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        favoriteService.createFavorite(
                userDetails.getId(),
                folderId,
                request
        );

        return SuccessResponse.of(
                HttpStatus.CREATED,
                "노선 추가 완료"
        );
    }

    /**
     * 즐겨찾기 노선 목록 조회
     */
    @GetMapping("/{folderId}/routes")
    public ResponseEntity<SuccessResponse<List<FavoriteResponse>>> getRoutes(
            @PathVariable Long folderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FavoriteResponse> response =
                favoriteService.getListFavorites(
                        userDetails.getId(),
                        folderId
                );

        return SuccessResponse.of(
                HttpStatus.OK,
                "노선 목록 조회 성공",
                response
        );
    }

    /**
     * 즐겨찾기 노선 삭제
     * @param folderId
     * @param favoriteId
     * @param userDetails
     * @return
     */
    @DeleteMapping("/{folderId}/routes/{favoriteId}")
    public ResponseEntity<SuccessResponse<Void>> deleteFavorite(
            @PathVariable Long folderId,
            @PathVariable Long favoriteId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        favoriteService.deleteFavorite(
                userDetails.getId(),
                folderId,
                favoriteId
        );

        return SuccessResponse.of(
                HttpStatus.OK,
                "노선 삭제 완료"
        );
    }
}
