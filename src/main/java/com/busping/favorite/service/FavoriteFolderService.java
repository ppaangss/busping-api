package com.busping.favorite.service;

import com.busping.favorite.domain.FavoriteFolder;
import com.busping.favorite.domain.FavoriteFolderRepository;
import com.busping.favorite.dto.FolderCreateRequest;
import com.busping.favorite.dto.FolderResponse;
import com.busping.favorite.dto.FolderUpdateRequest;
import com.busping.global.exception.custom.BusinessException;
import com.busping.global.exception.errorcode.CommonErrorCode;
import com.busping.user.domain.User;
import com.busping.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteFolderService {

    private final FavoriteFolderRepository favoriteFolderRepository;
    private final UserRepository userRepository;

    /**
     * 폴더 생성
     */
    @Transactional
    public FolderResponse createFolder(Long userId, FolderCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        FavoriteFolder folder = FavoriteFolder.create(user,request.getName());

        favoriteFolderRepository.save(folder);

        return FolderResponse.from(folder);
    }

    /**
     * 폴더 리스트 조회
     */
    @Transactional(readOnly = true)
    public List<FolderResponse> getFolders(Long userId) {

        // 폴더를 만든 순서 (자동 id 순서 별로 오름차순 정렬)
        return favoriteFolderRepository.findAllByUser_IdOrderByIdAsc(userId)
                .stream()
                .map(FolderResponse::from)
                .toList();
    }

    /**
     * 폴더명 변경
     */
    @Transactional
    public void updateFolder(Long userId, Long folderId, FolderUpdateRequest request) {

        FavoriteFolder folder = favoriteFolderRepository
                .findByIdAndUser_Id(folderId, userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        folder.updateName(request.getName());
    }

    /**
     * 폴더 삭제
     */
    @Transactional
    public void deleteFolder(Long userId, Long folderId) {

        FavoriteFolder folder = favoriteFolderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        favoriteFolderRepository.delete(folder);
    }
}
