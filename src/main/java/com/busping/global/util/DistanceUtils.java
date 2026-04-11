package com.busping.global.util;

public final class DistanceUtils {

    private static final double EARTH_RADIUS = 6371000; // meters

    private DistanceUtils() {
        // 인스턴스 생성 방지
    }

    /**
     * Haversine
     * 두 위경도 좌표 사이의 거리를 미터 단위로 반환한다.
     *
     * @param lat1 시작 위도
     * @param lon1 시작 경도
     * @param lat2 도착 위도
     * @param lon2 도착 경도
     * @return 두 지점 사이 거리 (meters)
     */
    public static double calculateDistance(
            double lat1, double lon1,
            double lat2, double lon2
    ) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}
