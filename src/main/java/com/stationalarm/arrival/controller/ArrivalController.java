package com.stationalarm.arrival.controller;

import com.stationalarm.arrival.domain.Arrival;
import com.stationalarm.arrival.service.ArrivalService;
import com.stationalarm.global.external.tago.arrival.TagoArrivalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class ArrivalController {
    private final ArrivalService arrivalService;

    @GetMapping("/{stationId}/realtime")
    public ResponseEntity<?> getRealtimeArrivals(
            @PathVariable String stationId,
            @RequestParam String cityCode
    ) {

        List<Arrival> response =
                arrivalService.getRealtimeArrivals(cityCode, stationId);

        return ResponseEntity.ok(
                Map.of(
                        "status", 200,
                        "data", response
                )
        );
    }
}
