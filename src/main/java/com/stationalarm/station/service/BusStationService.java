package com.stationalarm.station.service;

import com.stationalarm.global.util.DistanceUtils;
import com.stationalarm.station.domain.BusStation;
import com.stationalarm.station.domain.BusStationRepository;
import com.stationalarm.station.dto.StationResponse;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusStationService {

    private static final double SEARCH_RADIUS_METERS = 1_000;
    private static final int MAX_RESULTS = 20;

    private final BusStationRepository busStationRepository;

    /**
     * 사용자 좌표 주변의 버스 정류장을 DB에서 조회해 가까운 순서대로 반환한다.
     */
    public List<StationResponse> getNearbyStations(
            double lat,
            double lng
    ) {
        double latitudeDelta = metersToLatitudeDelta(SEARCH_RADIUS_METERS);
        double longitudeDelta = metersToLongitudeDelta(lat, SEARCH_RADIUS_METERS);

        List<BusStation> stations =
                busStationRepository.findByLatitudeBetweenAndLongitudeBetween(
                        lat - latitudeDelta,
                        lat + latitudeDelta,
                        lng - longitudeDelta,
                        lng + longitudeDelta
                );

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
