package com.busping.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DistanceUtilsTest {

    // 자오선 호 길이 - 같은 경도에서 위도 1도 차이는 pi * R / 180 으로 손계산 가능
    private static final double METERS_PER_LAT_DEGREE = Math.PI * 6_371_000 / 180; // 111,194.9m

    @Test
    @DisplayName("같은 좌표 사이의 거리는 0이다")
    void sameCoordinateIsZeroDistance() {
        double distance = DistanceUtils.calculateDistance(37.5665, 126.9780, 37.5665, 126.9780);

        assertThat(distance).isZero();
    }

    @Test
    @DisplayName("같은 경도에서 위도 1도 차이는 자오선 호 길이(약 111,195m)다")
    void oneLatitudeDegreeIsMeridianArcLength() {
        double distance = DistanceUtils.calculateDistance(37.0, 126.9780, 38.0, 126.9780);

        assertThat(distance).isCloseTo(METERS_PER_LAT_DEGREE, within(1.0));
    }

    @Test
    @DisplayName("거리는 대칭이다 - 출발과 도착을 바꿔도 같다")
    void distanceIsSymmetric() {
        double forward = DistanceUtils.calculateDistance(37.5665, 126.9780, 37.4979, 127.0276);
        double backward = DistanceUtils.calculateDistance(37.4979, 127.0276, 37.5665, 126.9780);

        assertThat(forward).isEqualTo(backward);
    }

    @Test
    @DisplayName("위도 60도에서 경도 1도 차이는 자오선 호의 절반(cos 60도 = 0.5)이다")
    void oneLongitudeDegreeShrinksByCosineOfLatitude() {
        // 경도 방향 거리는 위도에 따라 cos(위도)배로 줄어든다 - 라디안 변환 실수를 잡는 케이스
        double distance = DistanceUtils.calculateDistance(60.0, 126.0, 60.0, 127.0);

        assertThat(distance).isCloseTo(METERS_PER_LAT_DEGREE * 0.5, within(2.0));
    }

    @Test
    @DisplayName("서울 위도에서 위도 0.0045도 차이는 알람 반경 500m 근처다")
    void alarmRadiusScaleAtSeoulLatitude() {
        // 0.0045도 * 111,194.9m = 500.4m - 알람 반경 필터가 다루는 도메인 스케일
        double distance = DistanceUtils.calculateDistance(37.5665, 126.9780, 37.5710, 126.9780);

        assertThat(distance).isCloseTo(500.4, within(0.5));
    }
}
