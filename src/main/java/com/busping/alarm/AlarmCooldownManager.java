package com.busping.alarm;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AlarmCooldownManager {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${alarm.batch.cooldown-seconds}")
    private long cooldownSeconds;

    public boolean tryAcquireCooldown(
            Long userId,
            String cityCode,
            String stationId,
            String routeId
    ) {

        String key = buildKey(userId, cityCode, stationId, routeId);

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(cooldownSeconds));

        return Boolean.TRUE.equals(success);
    }

    private String buildKey(
            Long userId,
            String cityCode,
            String stationId,
            String routeId
    ) {
        return "alarm:cooldown:%d:%s:%s:%s"
                .formatted(userId, cityCode, stationId, routeId);
    }
}
