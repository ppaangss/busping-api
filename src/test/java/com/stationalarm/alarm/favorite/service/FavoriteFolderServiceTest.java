package com.stationalarm.alarm.favorite.service;

import com.stationalarm.favorite.domain.FavoriteFolder;
import com.stationalarm.favorite.domain.FavoriteFolderRepository;
import com.stationalarm.favorite.dto.FolderCreateRequest;
import com.stationalarm.favorite.dto.FolderResponse;
import com.stationalarm.favorite.dto.FolderUpdateRequest;
import com.stationalarm.global.exception.custom.BusinessException;
import com.stationalarm.global.exception.errorcode.CommonErrorCode;
import com.stationalarm.user.domain.User;
import com.stationalarm.user.domain.UserRepository;
import com.stationalarm.favorite.service.FavoriteFolderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class FavoriteFolderServiceTest {

    @Mock
    FavoriteFolderRepository favoriteFolderRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    FavoriteFolderService favoriteFolderService;

    @Nested
    @DisplayName("폴더 생성")
    class CreateFolder {

        @Test
        @DisplayName("정상적으로 폴더 생성")
        void given_validUser_when_createFolder_then_success() {
            // given
            Long userId = 1L;
            User user = mock(User.class);

            FolderCreateRequest request = new FolderCreateRequest("출근");

            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user));

            FavoriteFolder folder = FavoriteFolder.create(user, "출근");
            ReflectionTestUtils.setField(folder, "id", 1L);

            given(favoriteFolderRepository.save(any(FavoriteFolder.class)))
                    .willReturn(folder);

            // when
            FolderResponse response = favoriteFolderService.createFolder(userId, request);

            // then
            assertThat(response).isNotNull();
            then(userRepository).should().findById(userId);
            then(favoriteFolderRepository).should().save(any(FavoriteFolder.class));
        }

        @Test
        @DisplayName("유저가 없으면 예외 발생")
        void given_invalidUser_when_createFolder_then_throwException() {
            // given
            Long userId = 1L;
            FolderCreateRequest request = new FolderCreateRequest("출근");

            given(userRepository.findById(userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    favoriteFolderService.createFolder(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("폴더 목록 조회")
    class GetFolders {

        @Test
        @DisplayName("정상적으로 폴더 리스트 반환")
        void given_userFoldersExist_when_getFolders_then_success() {
            // given
            Long userId = 1L;

            FavoriteFolder folder1 = mock(FavoriteFolder.class);
            FavoriteFolder folder2 = mock(FavoriteFolder.class);

            given(favoriteFolderRepository.findAllByUser_IdOrderByIdAsc(userId))
                    .willReturn(List.of(folder1, folder2));

            // when
            List<FolderResponse> result = favoriteFolderService.getFolders(userId);

            // then
            assertThat(result).hasSize(2);
            then(favoriteFolderRepository).should()
                    .findAllByUser_IdOrderByIdAsc(userId);
        }

        @Test
        @DisplayName("폴더가 없으면 빈 리스트 반환")
        void given_noFolders_when_getFolders_then_returnEmptyList() {
            // given
            Long userId = 1L;

            given(favoriteFolderRepository.findAllByUser_IdOrderByIdAsc(userId))
                    .willReturn(List.of());

            // when
            List<FolderResponse> result = favoriteFolderService.getFolders(userId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("폴더명 수정")
    class UpdateFolder {

        @Test
        @DisplayName("정상적으로 폴더명 수정")
        void given_validFolder_when_updateFolder_then_success() {
            // given
            Long userId = 1L;
            Long folderId = 10L;

            FavoriteFolder folder = mock(FavoriteFolder.class);

            given(favoriteFolderRepository.findByIdAndUser_Id(folderId, userId))
                    .willReturn(Optional.of(folder));

            FolderUpdateRequest request = new FolderUpdateRequest("퇴근");

            // when
            favoriteFolderService.updateFolder(userId, folderId, request);

            // then
            then(folder).should().updateName("퇴근");
        }

        @Test
        @DisplayName("폴더가 없으면 예외 발생")
        void given_invalidFolder_when_updateFolder_then_throwException() {
            // given
            Long userId = 1L;
            Long folderId = 10L;

            given(favoriteFolderRepository.findByIdAndUser_Id(folderId, userId))
                    .willReturn(Optional.empty());

            FolderUpdateRequest request = new FolderUpdateRequest("퇴근");

            // when & then
            assertThatThrownBy(() ->
                    favoriteFolderService.updateFolder(userId, folderId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("폴더 삭제")
    class DeleteFolder {

        @Test
        @DisplayName("정상적으로 폴더 삭제")
        void given_validFolder_when_deleteFolder_then_success() {
            // given
            Long userId = 1L;
            Long folderId = 10L;

            FavoriteFolder folder = mock(FavoriteFolder.class);

            given(favoriteFolderRepository.findByIdAndUserId(folderId, userId))
                    .willReturn(Optional.of(folder));

            // when
            favoriteFolderService.deleteFolder(userId, folderId);

            // then
            then(favoriteFolderRepository).should().delete(folder);
        }

        @Test
        @DisplayName("폴더가 없으면 예외 발생")
        void given_invalidFolder_when_deleteFolder_then_throwException() {
            // given
            Long userId = 1L;
            Long folderId = 10L;

            given(favoriteFolderRepository.findByIdAndUserId(folderId, userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    favoriteFolderService.deleteFolder(userId, folderId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}