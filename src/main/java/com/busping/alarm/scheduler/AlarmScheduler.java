package com.busping.alarm.scheduler;

import com.busping.alarm.service.AlarmCoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AlarmScheduler {

    private final AlarmCoreService coreService;

    // 이전 실행이 끝난 뒤 30초 후에 다시 실행
    // 임시로 테스트를 위해 30000000으로 길게 해놓음
    // 실제는 30000으로 30초마다
    @Scheduled(fixedDelayString = "${alarm.batch.delay-ms}")
    public void run() {
        log.info("알람 배치 실행 중...");
        coreService.runCycle();
    }
}