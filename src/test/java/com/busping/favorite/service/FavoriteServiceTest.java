package com.busping.favorite.service;

import com.busping.device.domain.Device;
import com.busping.favorite.domain.Favorite;
import com.busping.favorite.domain.FavoriteFolder;
import com.busping.favorite.domain.FavoriteFolderRepository;
import com.busping.favorite.domain.FavoriteRepository;
import com.busping.favorite.dto.FavoriteCreateRequest;
import com.busping.global.exception.custom.BusinessException;
import com.busping.global.exception.errorcode.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    private static final UUID DEVICE_ID = UUID.randomUUID();
    private static final Long FOLDER_ID = 1L;

    @Mock
    FavoriteRepository favoriteRepository;
    @Mock
    FavoriteFolderRepository favoriteFolderRepository;

    @InjectMocks
    FavoriteService favoriteService;

    @Test
    @DisplayName("없는 폴더 또는 남의 폴더에는 노선을 추가할 수 없다")
    void createInForeignFolderIsRejected() {
        // 소유권 검증 쿼리가 빈 값 - 폴더가 없거나 deviceId가 다른 경우 모두 이 모양이다
        when(favoriteFolderRepository.findByIdAndDevice_Id(FOLDER_ID, DEVICE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.createFavorite(DEVICE_ID, FOLDER_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 폴더에 같은 정류장·노선 조합은 중복으로 거부된다")
    void duplicateStationRouteIsRejected() {
        when(favoriteFolderRepository.findByIdAndDevice_Id(FOLDER_ID, DEVICE_ID))
                .thenReturn(Optional.of(myFolder()));
        when(favoriteRepository.existsByFolder_IdAndStationIdAndRouteId(FOLDER_ID, "ST1", "ROUTE1"))
                .thenReturn(true);

        assertThatThrownBy(() -> favoriteService.createFavorite(DEVICE_ID, FOLDER_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.DUPLICATE_RESOURCE);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("검증을 통과하면 요청 내용대로 즐겨찾기가 저장된다")
    void createSavesFavoriteWithRequestValues() {
        FavoriteFolder folder = myFolder();
        when(favoriteFolderRepository.findByIdAndDevice_Id(FOLDER_ID, DEVICE_ID))
                .thenReturn(Optional.of(folder));
        when(favoriteRepository.existsByFolder_IdAndStationIdAndRouteId(FOLDER_ID, "ST1", "ROUTE1"))
                .thenReturn(false);

        favoriteService.createFavorite(DEVICE_ID, FOLDER_ID, request());

        ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteRepository).save(captor.capture());
        Favorite saved = captor.getValue();
        assertThat(saved.getStationId()).isEqualTo("ST1");
        assertThat(saved.getRouteId()).isEqualTo("ROUTE1");
        assertThat(saved.getFolder()).isSameAs(folder);
    }

    @Test
    @DisplayName("남의 폴더의 노선 목록은 조회할 수 없다")
    void listOfForeignFolderIsRejected() {
        when(favoriteFolderRepository.findByIdAndDevice_Id(FOLDER_ID, DEVICE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.getListFavorites(DEVICE_ID, FOLDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("내 폴더라도 그 폴더에 없는 노선은 삭제할 수 없다")
    void deleteFavoriteNotInFolderIsRejected() {
        when(favoriteFolderRepository.findByIdAndDevice_Id(FOLDER_ID, DEVICE_ID))
                .thenReturn(Optional.of(myFolder()));
        when(favoriteRepository.findByIdAndFolder_Id(99L, null)) // 단위 테스트라 폴더 id는 null
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.deleteFavorite(DEVICE_ID, FOLDER_ID, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);

        verify(favoriteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("내 폴더의 노선은 삭제된다")
    void deleteFavoriteInMyFolder() {
        FavoriteFolder folder = myFolder();
        Favorite favorite = Favorite.create(folder, "ST1", "시청앞", "23", 37.5, 127.0, "ROUTE1", "77");
        when(favoriteFolderRepository.findByIdAndDevice_Id(FOLDER_ID, DEVICE_ID))
                .thenReturn(Optional.of(folder));
        when(favoriteRepository.findByIdAndFolder_Id(99L, null))
                .thenReturn(Optional.of(favorite));

        favoriteService.deleteFavorite(DEVICE_ID, FOLDER_ID, 99L);

        verify(favoriteRepository).delete(favorite);
    }

    // ===== 픽스처 =====

    private FavoriteFolder myFolder() {
        return FavoriteFolder.create(Device.create(), "출근길");
    }

    private FavoriteCreateRequest request() {
        return FavoriteCreateRequest.builder()
                .stationId("ST1")
                .stationName("시청앞")
                .regionCode("23")
                .latitude(37.5665)
                .longitude(126.9780)
                .routeId("ROUTE1")
                .routeName("77")
                .build();
    }
}
