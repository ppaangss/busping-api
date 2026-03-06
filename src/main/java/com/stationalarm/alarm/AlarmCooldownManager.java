package com.stationalarm.alarm;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AlarmCooldownManager {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration COOLDOWN = Duration.ofMinutes(3);

    public boolean tryAcquireCooldown(
            Long userId,
            String cityCode,
            String stationId,
            String routeId
    ) {

        String key = buildKey(userId, cityCode, stationId, routeId);

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", COOLDOWN);

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
