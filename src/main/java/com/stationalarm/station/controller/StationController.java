package com.stationalarm.station.controller;


import com.stationalarm.station.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;

    @GetMapping("/nearby")
    public ResponseEntity<?> nearbyStations(
            @RequestParam double latitude,
            @RequestParam double longitude
            //@RequestParam(defaultValue = "500") int radius
    ) {
        //System.out.println("/api/stations/nearby/with-arrivals 호출\n 경도:" + lat + "\n 위도:" + lng);
        return ResponseEntity.ok(
                Map.of(
                        "status", 200,
                        "data", stationService.getNearbyStations(latitude, longitude)
                )
        );
    }

}

