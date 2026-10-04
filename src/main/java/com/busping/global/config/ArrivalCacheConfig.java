package com.busping.global.config;

import com.busping.global.external.tago.arrival.CachedTagoArrivalClient;
import com.busping.global.external.tago.arrival.FakeTagoArrivalClient;
import com.busping.global.external.tago.arrival.TagoArrivalClient;
import com.busping.global.external.tago.arrival.TagoArrivalPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 도착정보 캐시 조립 - TagoArrivalPort의 @Primary를 캐시 데코레이터로 세운다.
 * 원 출처(dev·benchmark는 fake, 그 외는 실 클라이언트)를 안에 감싸므로,
 * 도착정보가 필요한 세 경로(단일 조회·폴더 조회·알람)가 전부 이 검문소를 지난다.
 */
@Configuration
public class ArrivalCacheConfig {

    @Bean
    @Primary
    public TagoArrivalPort cachedTagoArrivalPort(
            ObjectProvider<FakeTagoArrivalClient> fakeClient,
            TagoArrivalClient tagoArrivalClient,
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${arrival-cache.ttl-seconds}") long ttlSeconds) {

        // 원 출처 선택 - dev·benchmark에선 fake 빈이 존재하므로 그걸 감싸고, 없으면(loadtest·운영) 실 클라이언트
        TagoArrivalPort origin = fakeClient.getIfAvailable();
        if (origin == null) {
            origin = tagoArrivalClient;
        }
        return new CachedTagoArrivalClient(origin, redisTemplate, objectMapper, meterRegistry, ttlSeconds);
    }
}
