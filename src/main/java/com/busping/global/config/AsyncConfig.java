package com.busping.global.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 알람 평가 전용 워커 풀 - 핑의 외부 I/O 대기를 톰캣 풀에서 격리한다.
 *
 * 수치의 근거 (02/수치.md 사슬):
 * - 워커 20   = 유입 3/s × 아픈 날 처리 6초(TAGO 1s + FCM 5s) = 18 반올림
 *               → FCM 5초 지연 장애까지는 알람 전량 배달, 그보다 깊으면 폐기의 영역
 * - 큐 256    = 평시 소화 20/s × 목표 지연 D 15초 = 300 내림 (최악 대기 12.8초)
 *               → 장애를 버티는 양이 아니라 평시 버스트 흡수 + 신선도 상한 선언
 * - DiscardOldest = 큐가 차면 가장 낡은 이벤트를 버리고 최신을 받는다
 *               → 유실량은 물리(유입-소화)가 정하고, 정책은 "살아남는 알람의 신선도"를 정한다
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "alarmExecutor")
    public ThreadPoolTaskExecutor alarmExecutor(MeterRegistry meterRegistry) {

        Counter discarded = Counter.builder("alarm.queue.discarded")
                .description("큐 포화로 밀려난(가장 낡은) 알람 평가 작업 수")
                .register(meterRegistry);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("alarm-worker-");
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(256);

        // DiscardOldest + 폐기 카운터 - 기본 DiscardOldestPolicy는 조용히 버려서 유실이 관측 불가
        executor.setRejectedExecutionHandler((task, pool) -> {
            if (!pool.isShutdown()) {
                pool.getQueue().poll(); // 머리(가장 오래 기다린 작업) 제거
                discarded.increment();
                pool.execute(task);
            }
        });

        // 셧다운 시 큐를 기다리지 않고 버린다 - 재시작 = 낡은 백로그 청소 (도메인상 유실 허용)
        executor.setWaitForTasksToCompleteOnShutdown(false);

        return executor;
    }
}
