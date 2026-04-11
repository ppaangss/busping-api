package com.busping.station.controller;

import com.busping.global.common.SuccessResponse;
import com.busping.station.dto.StationResponse;
import com.busping.station.service.BusStationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stations")
public class BusStationController {

    private final BusStationService busStationService;

    /**
     * 클라이언트 좌표를 기준으로 주변 버스 정류장 목록을 조회한다.
     */
    @GetMapping("/nearby")
    public ResponseEntity<SuccessResponse<List<StationResponse>>> nearbyStations(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        long start = System.nanoTime();
        List<StationResponse> stations =
                busStationService.getNearbyStations(latitude, longitude);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        log.debug("[Bus station nearby lookup completed] resultCount={}, elapsedMs={}ms",
                stations.size(),
                elapsedMs);

        return SuccessResponse.of(
                HttpStatus.OK,
                "정류장 조회 성공",
                stations
        );
    }
}
