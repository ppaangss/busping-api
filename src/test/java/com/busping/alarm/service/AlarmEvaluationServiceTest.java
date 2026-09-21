package com.busping.alarm.service;

import com.busping.alarm.AlarmCooldownManager;
import com.busping.arrival.domain.Arrival;
import com.busping.arrival.service.ArrivalService;
import com.busping.device.domain.Device;
import com.busping.favorite.domain.Favorite;
import com.busping.favorite.domain.FavoriteRepository;
import com.busping.global.external.fcm.FcmPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlarmEvaluationServiceTest {

    // 서울시청 좌표를 기준점으로, 위도 오프셋으로 거리를 만든다 (1도 = 111,194.9m)
    private static final double BASE_LAT = 37.5665;
    private static final double BASE_LNG = 126.9780;
    private static final double LAT_499M = 0.00449; // 499.3m - 반경 안
    private static final double LAT_500M = 0.00450; // 500.4m - 반경 밖

    @Mock
    FavoriteRepository favoriteRepository;
    @Mock
    ArrivalService arrivalService;
    @Mock
    AlarmCooldownManager cooldownManager;
    @Mock
    FcmPort fcmPort;

    @InjectMocks
    AlarmEvaluationService alarmEvaluationService;

    @Test
    @DisplayName("알람이 꺼진 디바이스는 즐겨찾기 조회조차 하지 않는다")
    void alarmOffSkipsEverything() {
        Device device = deviceWithToken();
        device.updateAlarmEnabled(false);

        alarmEvaluationService.evaluate(device, BASE_LAT, BASE_LNG);

        verifyNoInteractions(favoriteRepository, arrivalService, cooldownManager, fcmPort);
    }

    @Test
    @DisplayName("즐겨찾기가 하나도 없으면 발송하지 않는다")
    void noFavoritesSendsNothing() {
        when(favoriteRepository.findAllByFolder_Device_Id(any())).thenReturn(List.of());

        alarmEvaluationService.evaluate(deviceWithToken(), BASE_LAT, BASE_LNG);

        verifyNoInteractions(arrivalService, cooldownManager, fcmPort);
    }

    @Test
    @DisplayName("500m 밖 즐겨찾기는 평가하지 않는다")
    void favoriteOutsideRadiusIsSkipped() {
        Favorite far = favoriteAt(BASE_LAT + 1.0, "ROUTE1"); // 약 111km
        when(favoriteRepository.findAllByFolder_Device_Id(any())).thenReturn(List.of(far));

        alarmEvaluationService.evaluate(deviceWithToken(), BASE_LAT, BASE_LNG);

        verifyNoInteractions(arrivalService, cooldownManager, fcmPort);
    }

    @Test
    @DisplayName("경계 바로 밖(500.4m)은 평가하지 않는다")
    void favoriteJustOutsideBoundaryIsSkipped() {
        Favorite justOutside = favoriteAt(BASE_LAT + LAT_500M, "ROUTE1");
        when(favoriteRepository.findAllByFolder_Device_Id(any())).thenReturn(List.of(justOutside));

        alarmEvaluationService.evaluate(deviceWithToken(), BASE_LAT, BASE_LNG);

        verifyNoInteractions(cooldownManager, fcmPort);
    }

    @Test
    @DisplayName("경계 바로 안(499.3m)은 평가하고 발송까지 간다")
    void favoriteJustInsideBoundaryIsEvaluated() {
        Favorite justInside = favoriteAt(BASE_LAT + LAT_499M, "ROUTE1");
        when(favoriteRepository.findAllByFolder_Device_Id(any())).thenReturn(List.of(justInside));
        when(cooldownManager.tryAcquireCooldown(any(), any(), any(), any())).thenReturn(true);
        when(arrivalService.getGroupedArrivals("23", "ST1"))
                .thenReturn(Map.of("ROUTE1", List.of(arrival(3, 2))));

        alarmEvaluationService.evaluate(deviceWithToken(), BASE_LAT, BASE_LNG);

        verify(fcmPort, times(1)).send(any(), any(), any());
    }

    @Test
    @DisplayName("쿨다운에 걸리면 도착정보를 조회하지 않는다 - TAGO 호출 절약")
    void cooldownBlocksArrivalFetch() {
        Favorite favorite = favoriteAt(BASE_LAT, "ROUTE1");
        when(favoriteRepository.findAllByFolder_Device_Id(any())).thenReturn(List.of(favorite));
        when(cooldownManager.tryAcquireCooldown(any(), any(), any(), any())).thenReturn(false);

        alarmEvaluationService.evaluate(deviceWithToken(), BASE_LAT, BASE_LNG);

        verifyNoInteractions(arrivalService, fcmPort);
    }

    @Test
    @DisplayName("도착정보에 해당 노선이 없으면 발송하지 않는다")
    void missingRouteInArrivalsSendsNothing() {
        Favorite favorite = favoriteAt(BASE_LAT, "ROUTE1");
        when(favoriteRepository.findAllByFolder_Device_Id(any())).thenReturn(List.of(favorite));
        when(cooldownManager.tryAcquireCooldown(any(), any(), any(), any())).thenReturn(true);
        when(arrivalService.getGroupedArrivals("23", "ST1")).thenReturn(Map.of()); // 다른 노선만 있거나 빈 응답

        alarmEvaluationService.evaluate(deviceWithToken(), BASE_LAT, BASE_LNG);

        verifyNoInteractions(fcmPort);
    }

    @Test
    @DisplayName("노선은 있지만 도착 목록이 비어 있으면 발송하지 않는다")
    void emptyArrivalListSendsNothing() {
        Favorite favorite = favoriteAt(BASE_LAT, "ROUTE1");
        when(favoriteRepository.findAllByFolder_Device_Id(any())).thenReturn(List.of(favorite));
        when(cooldownManager.tryAcquireCooldown(any(), any(), any(), any())).thenReturn(true);
        when(arrivalService.getGroupedArrivals("23", "ST1")).thenReturn(Map.of("ROUTE1", List.of()));

        alarmEvaluationService.evaluate(deviceWithToken(), BASE_LAT, BASE_LNG);

        verifyNoInteractions(fcmPort);
    }

    @Test
    @DisplayName("TAGO 조회가 예외를 던져도 위치 이벤트는 실패하지 않는다")
    void tagoFailureIsIsolated() {
        Favorite favorite = favoriteAt(BASE_LAT, "ROUTE1");
        when(favoriteRepository.findAllByFolder_Device_Id(any())).thenReturn(List.of(favorite));
        when(cooldownManager.tryAcquireCooldown(any(), any(), any(), any())).thenReturn(true);
        when(arrivalService.getGroupedArrivals("23", "ST1")).thenThrow(new RuntimeException("TAGO 장애"));

        assertThatCode(() -> alarmEvaluationService.evaluate(deviceWithToken(), BASE_LAT, BASE_LNG))
                .doesNotThrowAnyException();

        verifyNoInteractions(fcmPort);
    }

    @Test
    @DisplayName("FCM 토큰이 없으면 발송하지 않는다")
    void deviceWithoutTokenSendsNothing() {
        Device tokenless = Device.create(); // 토큰 미등록
        Favorite favorite = favoriteAt(BASE_LAT, "ROUTE1");
        when(favoriteRepository.findAllByFolder_Device_Id(any())).thenReturn(List.of(favorite));
        when(cooldownManager.tryAcquireCooldown(any(), any(), any(), any())).thenReturn(true);
        when(arrivalService.getGroupedArrivals("23", "ST1"))
                .thenReturn(Map.of("ROUTE1", List.of(arrival(3, 2))));

        alarmEvaluationService.evaluate(tokenless, BASE_LAT, BASE_LNG);

        verifyNoInteractions(fcmPort);
    }

    @Test
    @DisplayName("조건이 모두 충족되면 가장 가까운 도착으로 알람 내용을 만들어 1회 발송한다")
    void sendsOnceWithNearestArrival() {
        Favorite favorite = favoriteAt(BASE_LAT, "ROUTE1");
        when(favoriteRepository.findAllByFolder_Device_Id(any())).thenReturn(List.of(favorite));
        when(cooldownManager.tryAcquireCooldown(any(), any(), any(), any())).thenReturn(true);
        // 도착 2건 - 오름차순 첫 번째(3분)가 선택되어야 한다
        when(arrivalService.getGroupedArrivals("23", "ST1"))
                .thenReturn(Map.of("ROUTE1", List.of(arrival(3, 2), arrival(12, 8))));

        alarmEvaluationService.evaluate(deviceWithToken(), BASE_LAT, BASE_LNG);

        verify(fcmPort, times(1)).send("token", "77 (시청앞)", "3분 후 도착 (2정거장 전)");
    }

    @Test
    @DisplayName("같은 정류장·노선의 중복 즐겨찾기는 1회만 발송한다")
    void duplicateFavoritesSendOnce() {
        Favorite duplicate1 = favoriteAt(BASE_LAT, "ROUTE1");
        Favorite duplicate2 = favoriteAt(BASE_LAT, "ROUTE1"); // 다른 폴더에 같은 조합
        when(favoriteRepository.findAllByFolder_Device_Id(any())).thenReturn(List.of(duplicate1, duplicate2));
        when(cooldownManager.tryAcquireCooldown(any(), any(), any(), any())).thenReturn(true);
        when(arrivalService.getGroupedArrivals("23", "ST1"))
                .thenReturn(Map.of("ROUTE1", List.of(arrival(3, 2))));

        alarmEvaluationService.evaluate(deviceWithToken(), BASE_LAT, BASE_LNG);

        verify(cooldownManager, times(1)).tryAcquireCooldown(any(), any(), any(), any());
        verify(fcmPort, times(1)).send(any(), any(), any());
    }

    @Test
    @DisplayName("같은 정류장의 다른 노선 2개는 도착정보를 1회만 조회하고 각각 발송한다")
    void sameStationTwoRoutesFetchOnceSendTwice() {
        Favorite route1 = favoriteAt(BASE_LAT, "ROUTE1");
        Favorite route2 = favoriteAt(BASE_LAT, "ROUTE2");
        when(favoriteRepository.findAllByFolder_Device_Id(any())).thenReturn(List.of(route1, route2));
        when(cooldownManager.tryAcquireCooldown(any(), any(), any(), any())).thenReturn(true);
        when(arrivalService.getGroupedArrivals("23", "ST1")).thenReturn(Map.of(
                "ROUTE1", List.of(arrival(3, 2)),
                "ROUTE2", List.of(arrival(7, 5))
        ));

        alarmEvaluationService.evaluate(deviceWithToken(), BASE_LAT, BASE_LNG);

        verify(arrivalService, times(1)).getGroupedArrivals("23", "ST1");
        verify(fcmPort, times(2)).send(any(), any(), any());
    }

    // ===== 픽스처 =====

    private Device deviceWithToken() {
        Device device = Device.create();
        device.updateFcmToken("token");
        return device;
    }

    private Favorite favoriteAt(double latitude, String routeId) {
        return Favorite.create(null, "ST1", "시청앞", "23", latitude, BASE_LNG, routeId, "77");
    }

    private Arrival arrival(int remainingMinutes, int remainingStops) {
        return Arrival.builder()
                .routeId("ROUTE1")
                .busNumber("77")
                .remainingMinutes(remainingMinutes)
                .remainingStops(remainingStops)
                .build();
    }
}
