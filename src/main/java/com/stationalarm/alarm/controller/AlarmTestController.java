package com.stationalarm.alarm.controller;

import com.stationalarm.alarm.service.AlarmCoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AlarmTestController {

    private final AlarmCoreService alarmCoreService;

    @PostMapping("/test/alarm/run")
    public String runAlarmCycle() {

        alarmCoreService.runCycle();

        return "alarm cycle executed";
    }
}