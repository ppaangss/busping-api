package com.busping.favorite.controller;

import com.busping.device.domain.Device;
import com.busping.favorite.dto.FolderCreateRequest;
import com.busping.favorite.dto.FolderResponse;
import com.busping.favorite.dto.FolderUpdateRequest;
import com.busping.favorite.service.FavoriteFolderService;
import com.busping.global.common.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            Device device
    ) {
        FolderResponse response = folderService.createFolder(device, request);

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
            Device device
    ) {
        List<FolderResponse> response = folderService.getFolders(device.getId());

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
            Device device
    ) {
        folderService.updateFolder(device.getId(), folderId, request);

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
            Device device
    ) {
        folderService.deleteFolder(device.getId(), folderId);

        return SuccessResponse.of(
                HttpStatus.OK,
                "폴더 삭제 완료"
        );
    }
}
