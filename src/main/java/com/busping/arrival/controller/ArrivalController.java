package com.busping.arrival.controller;

import com.busping.arrival.domain.Arrival;
import com.busping.arrival.dto.StationArrivalResponse;
import com.busping.arrival.service.ArrivalService;
import com.busping.global.common.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<SuccessResponse<StationArrivalResponse>> getRealtimeArrivals(
            @PathVariable String stationId,
            @RequestParam String cityCode
    ) {

        StationArrivalResponse response =
                arrivalService.getGroupedArrivalsResponse(cityCode, stationId);

        return SuccessResponse.of(
                HttpStatus.OK,
                "정류장 기준 도착 정보 조회 성공",
                response
        );
    }
}
