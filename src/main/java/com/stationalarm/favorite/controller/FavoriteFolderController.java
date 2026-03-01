package com.stationalarm.favorite.controller;

import com.stationalarm.favorite.dto.FolderCreateRequest;
import com.stationalarm.favorite.dto.FolderResponse;
import com.stationalarm.favorite.dto.FolderUpdateRequest;
import com.stationalarm.favorite.service.FavoriteFolderService;
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
@RequestMapping("/api/favorites/folders")
public class FavoriteFolderController {

    private final FavoriteFolderService folderService;

    /**
     * 폴더 생성
     */
    @PostMapping
    public ResponseEntity<SuccessResponse<FolderResponse>> createFolder(
            @Valid @RequestBody FolderCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();

        FolderResponse response = folderService.createFolder(userId, request);

        return SuccessResponse.of(
                HttpStatus.CREATED,
                "폴더 생성 완료",
                response
        );
    }

    /**
     * 폴더 목록 조회
     */
    @GetMapping
    public ResponseEntity<SuccessResponse<List<FolderResponse>>> getFolders(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();

        List<FolderResponse> response = folderService.getFolders(userId);

        return SuccessResponse.of(
                HttpStatus.OK,
                "폴더 목록 조회 성공",
                response
        );
    }

    /**
     * 폴더 수정 (이름 변경)
     */
    @PatchMapping("/{folderId}")
    public ResponseEntity<SuccessResponse<Void>> updateFolder(
            @PathVariable Long folderId,
            @Valid @RequestBody FolderUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();

        folderService.updateFolder(userId, folderId, request);

        return SuccessResponse.of(
                HttpStatus.OK,
                "폴더 수정 완료"
        );
    }

    /**
     * 폴더 삭제
     */
    @DeleteMapping("/{folderId}")
    public ResponseEntity<SuccessResponse<Void>> deleteFolder(
            @PathVariable Long folderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();

        folderService.deleteFolder(userId, folderId);

        return SuccessResponse.of(
                HttpStatus.OK,
                "폴더 삭제 완료"
        );
    }



}