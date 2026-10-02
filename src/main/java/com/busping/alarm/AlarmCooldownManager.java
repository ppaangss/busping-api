package com.busping.alarm;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 알람 중복 발송 쿨다운 - 경계 플래핑(GPS 진동·재진입으로 앱이 위치를 다시 보내는 경우)에도
 * 같은 (디바이스, 정류장, 노선) 조합은 TTL 안에 한 번만 발송된다. 의도는 진입당 알람 1회.
 */
@Component
@RequiredArgsConstructor
public class AlarmCooldownManager {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${alarm.cooldown-seconds}")
    private long cooldownSeconds;

    /** 프리플라이트 - 쿨다운이 0 이하면 플래핑 시 중복 발송 무제한이므로 부팅을 막는다 */
    @PostConstruct
    void validate() {
        if (cooldownSeconds <= 0) {
            throw new IllegalStateException("alarm.cooldown-seconds는 양수여야 함: " + cooldownSeconds);
        }
    }

    /** 쿨다운 획득 시도 - true면 발송 가능, false면 TTL 안이라 스킵 (NX + TTL 원자 연산) */
    public boolean tryAcquireCooldown(
            UUID deviceId,
            String cityCode,
            String stationId,
            String routeId
    ) {

        String key = buildKey(deviceId, cityCode, stationId, routeId);

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(cooldownSeconds));

        return Boolean.TRUE.equals(success);
    }

    private String buildKey(
            UUID deviceId,
            String cityCode,
            String stationId,
            String routeId
    ) {
        return "alarm:cooldown:%s:%s:%s:%s"
                .formatted(deviceId, cityCode, stationId, routeId);
    }
}
