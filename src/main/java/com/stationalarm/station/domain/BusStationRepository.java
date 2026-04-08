package com.stationalarm.station.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusStationRepository extends JpaRepository<BusStation, Long> {

    /**
     * 위경도 범위로 후보 정류장을 먼저 좁혀서 조회한다.
     */
    List<BusStation> findByLatitudeBetweenAndLongitudeBetween(
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude
    );
}
