package com.station.alarm.station.service;


import com.station.alarm.global.external.tago.TagoStationClient;
import com.station.alarm.station.domain.Station;
import com.station.alarm.station.dto.StationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
// 의존성 주입을 빈에서 찾아 자동으로 해줌
// 구현체가 2개이면 오류 -> @Primary 로 우선순위 명시
@RequiredArgsConstructor
public class StationService {

    private final TagoStationClient stationClient;

    /**
     * 좌표 기반 정류장 조회
     * 추후 조회 범위 추가
     */
    public List<StationResponse> getNearbyStations(
            double lat,
            double lng
    ) {

//        // 먼저 로컬 저장소 확인
//        List<Station> cachedStations = stationRepository.findAll();
//
//        if (!cachedStations.isEmpty()) {
//            return cachedStations.stream()
//                    .map(StationResponse::from)
//                    .toList();
//        }
        
        // API 호출
        List<Station> stations = stationClient.fetchNearbyStations(lat, lng);


        return stations.stream()
                .map(StationResponse::from)
                .toList();

        /* 두 개의 차이점 및 문법 분석
        *  return stations.stream()
                .map(station ->
                        new StationResponse(
                                station.getNodeId(),
                                station.getName(),
                                station.getLatitude(),
                                station.getLongitude()
                        )
                )
                .toList();
        */
    }
}
