package com.busping.alarm.controller;

import com.busping.alarm.dto.LocationReportRequest;
import com.busping.alarm.service.AlarmEvaluationService;
import com.busping.device.domain.Device;
import com.busping.global.common.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices/me/location")
public class LocationController {

    private final AlarmEvaluationService alarmEvaluationService;

    /**
     * 위치 이벤트 보고 - 위치는 저장하지 않고, 이 요청 안에서 알람 평가까지 수행한다 (이벤트 드리븐)
     */
    @PostMapping
    public ResponseEntity<SuccessResponse<Void>> reportLocation(
            Device device,
            @Valid @RequestBody LocationReportRequest request
    ) {
        alarmEvaluationService.evaluate(device, request.latitude(), request.longitude());

        return SuccessResponse.of(
                HttpStatus.OK,
                "위치 이벤트가 처리되었습니다."
        );
    }
}
