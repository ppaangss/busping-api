package com.stationalarm.alarm.scheduler;

import com.stationalarm.alarm.service.AlarmCoreService;
import com.stationalarm.alarm.service.AlarmLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class AlarmScheduler {

    private final AlarmLockService lockService;
    private final AlarmCoreService coreService;

    // @Value("${server.port}")
    // private String port;

    // 이전 실행이 끝난 뒤 30초 후에 다시 실행
    // 임시로 테스트를 위해 30000000으로 길게 해놓음
    // 실제는 30000으로 30초마다
    @Scheduled(fixedDelay = 30000000)
    public void run() {
        String lockValue = UUID.randomUUID().toString();

        // 왜 TTL을 40초로 했는가?
        // 스케줄은 30초 마다 실행, TTL은 스케줄보다 길어야 안전하다.
        // 배치를 하기전에 다음 서버가 락을 가져가서는 안된다.
        boolean locked = lockService.tryLock(lockValue, Duration.ofSeconds(40));

        if (!locked) {
            log.info("다른 서버가 실행 중 → 스킵");
            return;
        }

        // log.info("서버 포트 = {}, 락 획득 성공", port);

        try {
            // TODO: 여기서 알람 배치 로직 실행
            log.info("알람 배치 실행 중...");
            // coreService.runCycle();
            Thread.sleep(5000); // 5초 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lockService.unlock(lockValue);
            log.info("락 해제 완료");
        }
    }
}