package com.busping.favorite.controller;

import com.busping.device.domain.Device;
import com.busping.favorite.dto.FavoriteCreateRequest;
import com.busping.favorite.dto.FavoriteResponse;
import com.busping.favorite.service.FavoriteService;
import com.busping.global.common.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            Device device
    ) {
        favoriteService.createFavorite(
                device.getId(),
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
            Device device
    ) {
        List<FavoriteResponse> response =
                favoriteService.getListFavorites(
                        device.getId(),
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
     */
    @DeleteMapping("/{folderId}/routes/{favoriteId}")
    public ResponseEntity<SuccessResponse<Void>> deleteFavorite(
            @PathVariable Long folderId,
            @PathVariable Long favoriteId,
            Device device
    ) {
        favoriteService.deleteFavorite(
                device.getId(),
                folderId,
                favoriteId
        );

        return SuccessResponse.of(
                HttpStatus.OK,
                "노선 삭제 완료"
        );
    }
}
