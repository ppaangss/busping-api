package com.busping.alarm.controller;

import com.busping.alarm.service.AlarmCoreService;
import com.busping.global.common.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AlarmTestController {

    private final AlarmCoreService alarmCoreService;

    @PostMapping("/test/alarm/run")
    public ResponseEntity<SuccessResponse<Void>> runAlarmCycle() {

        alarmCoreService.runCycle();

        return SuccessResponse.of(
                HttpStatus.OK,
                "alarm cycle executed"
        );
    }
}