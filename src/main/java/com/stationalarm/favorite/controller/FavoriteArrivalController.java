package com.stationalarm.favorite.controller;

import com.stationalarm.favorite.dto.FolderArrivalResponse;
import com.stationalarm.favorite.service.FavoriteArrivalService;
import com.stationalarm.global.common.SuccessResponse;
import com.stationalarm.global.security.CustomUserDetails;
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
}
