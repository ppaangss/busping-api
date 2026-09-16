package com.busping.alarm;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 알람 재발송 쿨다운 - "체류 중 10분마다 재알림" 정의를 Redis TTL로 구현한다.
 * 같은 (디바이스, 정류장, 노선) 조합은 TTL 안에 한 번만 발송된다.
 */
@Component
@RequiredArgsConstructor
public class AlarmCooldownManager {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${alarm.cooldown-seconds}")
    private long cooldownSeconds;

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
