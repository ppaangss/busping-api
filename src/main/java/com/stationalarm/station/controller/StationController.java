package com.stationalarm.station.controller;


import com.stationalarm.favorite.dto.FolderArrivalResponse;
import com.stationalarm.global.common.SuccessResponse;
import com.stationalarm.station.dto.StationResponse;
import com.stationalarm.station.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;

    @GetMapping("/nearby")
    public ResponseEntity<SuccessResponse<List<StationResponse>>> nearbyStations(
            @RequestParam double latitude,
            @RequestParam double longitude
            //@RequestParam(defaultValue = "500") int radius
    ) {
        return SuccessResponse.of(
                HttpStatus.OK,
                "정류장 조회 성공",
                stationService.getNearbyStations(latitude, longitude)
        );
    }

}

