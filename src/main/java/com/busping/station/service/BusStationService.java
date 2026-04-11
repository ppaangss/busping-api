package com.busping.station.service;

import com.busping.global.util.DistanceUtils;
import com.busping.station.domain.BusStation;
import com.busping.station.domain.BusStationRepository;
import com.busping.station.dto.StationResponse;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 사용자 위치 기반으로 주변 버스 정류장을 조회하는 서비스.
 * 외부 API 없이 DB만 사용하며, 바운딩 박스 1차 필터 후 Haversine 2차 필터로 정확한 반경 검색을 수행한다.
 */
@Service
@RequiredArgsConstructor
public class BusStationService {

    private static final double SEARCH_RADIUS_METERS = 1_000; // 검색 반경 (미터)
    private static final int MAX_RESULTS = 20; // 최대 반환 정류장 수Z

    private final BusStationRepository busStationRepository;

    /**
     * 사용자 좌표 주변의 버스 정류장을 DB에서 조회해 가까운 순서대로 반환한다.
     */
    public List<StationResponse> getNearbyStations(
            double lat,
            double lng
    ) {
        // 1차 필터: 위경도 범위로 바운딩 박스를 구성해 DB에서 후보군을 빠르게 추출
        double latitudeDelta = metersToLatitudeDelta(SEARCH_RADIUS_METERS);
        double longitudeDelta = metersToLongitudeDelta(lat, SEARCH_RADIUS_METERS);

        List<BusStation> stations =
                busStationRepository.findByLatitudeBetweenAndLongitudeBetween(
                        lat - latitudeDelta,
                        lat + latitudeDelta,
                        lng - longitudeDelta,
                        lng + longitudeDelta
                );

        // 2차 필터: Haversine 공식으로 실거리를 계산해 정확한 반경 내 정류장만 추출, 가까운 순 정렬
        return stations.stream()
                .filter(station -> isWithinRadius(lat, lng, station))
                .sorted(Comparator.comparingDouble(
                        station -> DistanceUtils.calculateDistance(
                                lat,
                                lng,
                                station.getLatitude(),
                                station.getLongitude()
                        )
                ))
                .limit(MAX_RESULTS)
                .map(StationResponse::from)
                .toList();
    }

    /**
     * 정류장이 검색 반경 안에 포함되는지 실거리 기준으로 판별한다. (HaverSine)
     */
    private boolean isWithinRadius(
            double lat,
            double lng,
            BusStation station
    ) {
        return DistanceUtils.calculateDistance(
                lat,
                lng,
                station.getLatitude(),
                station.getLongitude()
        ) <= SEARCH_RADIUS_METERS;
    }

    /**
     * 미터 단위 반경을 위도 범위로 변환한다.
     */
    private double metersToLatitudeDelta(double meters) {
        return meters / 111_320d;
    }

    /**
     * 미터 단위 반경을 경도 범위로 변환한다.
     */
    private double metersToLongitudeDelta(
            double latitude,
            double meters
    ) {
        double cosLatitude = Math.cos(Math.toRadians(latitude));

        if (Math.abs(cosLatitude) < 1e-12) {
            return 180d;
        }

        return meters / (111_320d * cosLatitude);
    }
}
