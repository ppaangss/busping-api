package com.busping.favorite.service;

import com.busping.device.domain.Device;
import com.busping.favorite.domain.FavoriteFolder;
import com.busping.favorite.domain.FavoriteFolderRepository;
import com.busping.favorite.dto.FolderCreateRequest;
import com.busping.favorite.dto.FolderResponse;
import com.busping.favorite.dto.FolderUpdateRequest;
import com.busping.global.exception.custom.BusinessException;
import com.busping.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteFolderService {

    private final FavoriteFolderRepository favoriteFolderRepository;

    /**
     * 폴더 생성 - 인터셉터가 인증한 Device를 소유자로 연결
     */
    @Transactional
    public FolderResponse createFolder(Device device, FolderCreateRequest request) {

        FavoriteFolder folder = FavoriteFolder.create(device, request.getName());

        favoriteFolderRepository.save(folder);

        return FolderResponse.from(folder);
    }

    /**
     * 폴더 리스트 조회
     */
    @Transactional(readOnly = true)
    public List<FolderResponse> getFolders(UUID deviceId) {

        // 폴더를 만든 순서 (자동 id 순서 별로 오름차순 정렬)
        return favoriteFolderRepository.findAllByDevice_IdOrderByIdAsc(deviceId)
                .stream()
                .map(FolderResponse::from)
                .toList();
    }

    /**
     * 폴더명 변경
     */
    @Transactional
    public void updateFolder(UUID deviceId, Long folderId, FolderUpdateRequest request) {

        FavoriteFolder folder = favoriteFolderRepository
                .findByIdAndDevice_Id(folderId, deviceId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        folder.updateName(request.getName());
    }

    /**
     * 폴더 삭제
     */
    @Transactional
    public void deleteFolder(UUID deviceId, Long folderId) {

        FavoriteFolder folder = favoriteFolderRepository
                .findByIdAndDevice_Id(folderId, deviceId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        favoriteFolderRepository.delete(folder);
    }
}
