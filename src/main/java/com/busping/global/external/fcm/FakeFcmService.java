package com.busping.global.external.fcm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * FCM 실 발송 대체 - 발송 내용을 로그로만 남긴다 (Firebase 키 불필요)
 * fcm.fake-latency-ms로 지연 주입 가능 - FCM 지연·장애 시나리오 재현용 (실 FcmService처럼 호출 스레드가 블로킹된다)
 */
@Slf4j
@Profile({"dev", "loadtest"}) // loadtest - FCM은 측정 대상에서 제외, 발송은 로그로만 (SLO는 FCM 인계 기준)
@Primary
@Service
public class FakeFcmService implements FcmPort {

    @Value("${fcm.fake-latency-min-ms:${fcm.fake-latency-ms:0}}")
    private long fakeLatencyMinMs;

    @Value("${fcm.fake-latency-max-ms:${fcm.fake-latency-ms:0}}")
    private long fakeLatencyMaxMs;

    /**
     * 가짜 발송 - 지연 주입 후 로그만 남긴다. 실 FcmService처럼 완료까지 호출 스레드를 돌려주지 않는다
     */
    @Override
    public void send(String fcmToken, String title, String body) {
        simulateLatency();
        log.info("[FCM Fake] 발송 - title={}, body={}", title, body);
    }

    /**
     * min~max 사이 균등 랜덤 시간만큼 호출 스레드를 재운다 - max가 0 이하면 즉시 반환 (평시 기본값)
     * 랜덤 범위는 busy 그래프에 자연스러운 출렁임과 완만한 회복 꼬리를 만든다 (고정값이면 직선·절벽)
     */
    private void simulateLatency() {
        long max = Math.max(fakeLatencyMinMs, fakeLatencyMaxMs);
        if (max <= 0) {
            return;
        }
        long min = Math.max(0, Math.min(fakeLatencyMinMs, fakeLatencyMaxMs));
        long delay = (min == max) ? max : ThreadLocalRandom.current().nextLong(min, max + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

