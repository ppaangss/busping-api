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

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices/me/location")
public class LocationController {

    private final AlarmEvaluationService alarmEvaluationService;

    /**
     * 위치 이벤트 보고 - 접수만 하고 즉시 응답한다. 평가(TAGO·FCM)는 alarmExecutor 워커가 수행.
     * 이 응답을 기다리는 사람은 아무도 없다 - 결과는 FCM 푸시라는 별도 채널로 배달된다.
     */
    @PostMapping
    public ResponseEntity<SuccessResponse<Void>> reportLocation(
            Device device,
            @Valid @RequestBody LocationReportRequest request
    ) {
        // receivedAt은 제출 순간에 찍는다 - 큐 대기 시간이 TTL 판정에 포함되도록 (호출자 계약)
        alarmEvaluationService.evaluateAsync(
                device, request.latitude(), request.longitude(), Instant.now());

        return SuccessResponse.of(
                HttpStatus.ACCEPTED,
                "위치 이벤트가 접수되었습니다."
        );
    }
}
