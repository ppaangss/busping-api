package com.busping.alarm.favorite.service;

import com.busping.favorite.domain.Favorite;
import com.busping.favorite.domain.FavoriteFolder;
import com.busping.favorite.domain.FavoriteFolderRepository;
import com.busping.favorite.domain.FavoriteRepository;
import com.busping.favorite.dto.FavoriteCreateRequest;
import com.busping.favorite.dto.FavoriteResponse;
import com.busping.global.exception.custom.BusinessException;
import com.busping.global.exception.errorcode.CommonErrorCode;
import com.busping.user.domain.User;
import com.busping.favorite.service.FavoriteService;
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
class FavoriteServiceTest {

    @Mock
    FavoriteRepository favoriteRepository;

    @Mock
    FavoriteFolderRepository favoriteFolderRepository;

    @InjectMocks
    FavoriteService favoriteService;

    /* =========================
        실제 객체 생성 메서드들
        dto, entity
     ========================= */

    /**
     * dto 생성 임시 메서드
     * @return dto 반환
     */
    private FavoriteCreateRequest createRequest() {
        return FavoriteCreateRequest.builder()
                .stationId("123")
                .stationName("세종대후문")
                .regionCode("SEOUL")
                .latitude(37.5)
                .longitude(127.0)
                .routeId("3216")
                .routeName("강남역")
                .build();
    }

    /**
     * 유저 생성 임시 메서드
     * @param userId 사용자 Id
     * @return 유저 객체
     */
    private User createUser(Long userId) {
        User user = User.create("test@test.com", "encodedPassword");

        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    /**
     * 폴더 생성 임시 메서드
     * @param folderId 폴더 Id
     * @param userId 사용자 Id
     * @return 폴더 객체
     */
    private FavoriteFolder createFolder(Long folderId, Long userId) {

        User user = createUser(userId);

        FavoriteFolder folder = FavoriteFolder.create(user, "출근");

        ReflectionTestUtils.setField(folder, "id", folderId);

        return folder;
    }

    /**
     * 즐겨찾기 생성 임시 메서드
     * @param folder 폴더 Id
     * @param id 사용자 Id
     * @return 즐겨찾기 객체
     */
    private Favorite createFavorite(FavoriteFolder folder, Long id) {
        Favorite favorite = Favorite.create(
                folder,
                "123",
                "세종대후문",
                "SEOUL",
                37.5,
                127.0,
                "3216",
                "강남역"
        );
        ReflectionTestUtils.setField(favorite, "id", id);
        return favorite;
    }

    /* =========================
        Create Favorite
     ========================= */

    @Nested
    @DisplayName("즐겨찾기 생성")
    class CreateFavorite {

        @Test
        @DisplayName("정상 요청이면 즐겨찾기가 저장된다")
        void given_validRequest_when_createFavorite_then_success() {

            Long userId = 1L;
            Long folderId = 10L;

            FavoriteFolder folder = createFolder(folderId, userId);

            given(favoriteFolderRepository.findByIdAndUser_Id(folderId, userId))
                    .willReturn(Optional.of(folder));

            given(favoriteRepository.existsByFolder_IdAndStationIdAndRouteId(
                    folderId, "123", "3216"))
                    .willReturn(false);

            favoriteService.createFavorite(userId, folderId, createRequest());

            then(favoriteRepository).should().save(any(Favorite.class));
        }

        @Test
        @DisplayName("요청값이 null이면 INVALID_PARAMETER 예외가 발생한다")
        void given_nullRequest_when_createFavorite_then_throwException() {

            assertThatThrownBy(() ->
                    favoriteService.createFavorite(1L, 10L, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.INVALID_PARAMETER);
        }

        @Test
        @DisplayName("폴더가 존재하지 않으면 RESOURCE_NOT_FOUND 예외가 발생한다")
        void given_folderNotFound_when_createFavorite_then_throwException() {

            given(favoriteFolderRepository.findByIdAndUser_Id(10L, 1L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    favoriteService.createFavorite(1L, 10L, createRequest()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("같은 폴더에 동일 정류장과 노선이 이미 존재하면 DUPLICATE_RESOURCE 예외가 발생한다")
        void given_duplicateFavorite_when_createFavorite_then_throwException() {

            Long userId = 1L;
            Long folderId = 10L;

            FavoriteFolder folder = createFolder(folderId, userId);

            given(favoriteFolderRepository.findByIdAndUser_Id(folderId, userId))
                    .willReturn(Optional.of(folder));

            given(favoriteRepository.existsByFolder_IdAndStationIdAndRouteId(
                    folderId, "123", "3216"))
                    .willReturn(true);

            assertThatThrownBy(() ->
                    favoriteService.createFavorite(userId, folderId, createRequest()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.DUPLICATE_RESOURCE);
        }
    }

    /* =========================
        Get Favorites
     ========================= */

    @Nested
    @DisplayName("즐겨찾기 목록 조회")
    class GetFavorites {

        @Test
        @DisplayName("폴더가 존재하면 생성 순서대로 즐겨찾기 목록을 반환한다")
        void given_validFolder_when_getFavorites_then_success() {

            Long userId = 1L;
            Long folderId = 10L;

            FavoriteFolder folder = createFolder(folderId, userId);

            Favorite favorite1 = createFavorite(folder, 1L);
            Favorite favorite2 = createFavorite(folder, 2L);

            given(favoriteFolderRepository.findByIdAndUser_Id(folderId, userId))
                    .willReturn(Optional.of(folder));

            given(favoriteRepository.findAllByFolder_IdOrderByIdAsc(folderId))
                    .willReturn(List.of(favorite1, favorite2));

            List<FavoriteResponse> result =
                    favoriteService.getListFavorites(userId, folderId);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("폴더는 존재하지만 즐겨찾기가 없으면 빈 리스트를 반환한다")
        void given_noFavorites_when_getFavorites_then_emptyList() {

            Long userId = 1L;
            Long folderId = 10L;

            FavoriteFolder folder = createFolder(folderId, userId);

            given(favoriteFolderRepository.findByIdAndUser_Id(folderId, userId))
                    .willReturn(Optional.of(folder));

            given(favoriteRepository.findAllByFolder_IdOrderByIdAsc(folderId))
                    .willReturn(List.of());

            List<FavoriteResponse> result =
                    favoriteService.getListFavorites(userId, folderId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("폴더가 존재하지 않으면 RESOURCE_NOT_FOUND 예외가 발생한다")
        void given_folderNotFound_when_getFavorites_then_throwException() {

            given(favoriteFolderRepository.findByIdAndUser_Id(10L, 1L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    favoriteService.getListFavorites(1L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    /* =========================
        Delete Favorite
     ========================= */

    @Nested
    @DisplayName("즐겨찾기 삭제")
    class DeleteFavorite {

        @Test
        @DisplayName("폴더와 즐겨찾기가 존재하면 정상적으로 삭제된다")
        void given_validFavorite_when_deleteFavorite_then_success() {

            Long userId = 1L;
            Long folderId = 10L;
            Long favoriteId = 100L;

            FavoriteFolder folder = createFolder(folderId, userId);
            Favorite favorite = createFavorite(folder, favoriteId);

            given(favoriteFolderRepository.findByIdAndUser_Id(folderId, userId))
                    .willReturn(Optional.of(folder));

            given(favoriteRepository.findByIdAndFolder_Id(favoriteId, folderId))
                    .willReturn(Optional.of(favorite));

            favoriteService.deleteFavorite(userId, folderId, favoriteId);

            then(favoriteRepository).should().delete(favorite);
        }

        @Test
        @DisplayName("폴더가 존재하지 않으면 RESOURCE_NOT_FOUND 예외가 발생한다")
        void given_folderNotFound_when_deleteFavorite_then_throwException() {

            given(favoriteFolderRepository.findByIdAndUser_Id(10L, 1L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    favoriteService.deleteFavorite(1L, 10L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("해당 폴더에 속한 즐겨찾기가 없으면 RESOURCE_NOT_FOUND 예외가 발생한다")
        void given_favoriteNotFound_when_deleteFavorite_then_throwException() {

            Long userId = 1L;
            Long folderId = 10L;

            FavoriteFolder folder = createFolder(folderId, userId);

            given(favoriteFolderRepository.findByIdAndUser_Id(folderId, userId))
                    .willReturn(Optional.of(folder));

            given(favoriteRepository.findByIdAndFolder_Id(100L, folderId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    favoriteService.deleteFavorite(userId, folderId, 100L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}