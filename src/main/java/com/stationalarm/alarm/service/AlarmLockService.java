package com.stationalarm.alarm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AlarmLockService {

    private final StringRedisTemplate redisTemplate;

    // 나중에 고치기
    private static final String LOCK_KEY = "lock:alarm-batch";

    public boolean tryLock(String value, Duration ttl) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, value, ttl);

        return Boolean.TRUE.equals(success);
    }

    public void unlock(String value) {
        String current = redisTemplate.opsForValue().get(LOCK_KEY);
        if (value.equals(current)) {
            redisTemplate.delete(LOCK_KEY);
        }
    }
}