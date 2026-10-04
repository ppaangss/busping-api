package com.busping.global.external.tago.arrival;

import com.busping.arrival.domain.Arrival;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;

/**
 * 정류장 단위 TTL 캐시 데코레이터 - 도착정보는 유저별이 아니라 정류장별 데이터다.
 * 같은 정류장의 첫 요청만 TAGO로 가고, TTL 동안의 나머지 요청은 캐시 한 장으로 응답한다.
 * TAGO 호출량 상한 = 고유 정류장 수 x (1/TTL) - 유저 수와 무관해진다.
 *
 * - 키: cityCode:nodeId - TAGO 응답이 정류장 단위(전 노선 포함)라 노선은 키에 불필요
 * - TTL: 조회 자동 갱신 주기(30초)보다 짧아야 한다 - 유저가 다시 조회할 때 자기 캐시는
 *   반드시 만료돼 있어야 "갱신 = 직전보다 새 데이터" 계약이 지켜진다
 * - 실패·빈 응답은 캐시하지 않는다 - 순간 장애가 TTL 동안 전 유저에게 복사되는 것 방지
 * - Redis 장애는 미스로 취급하고 원 호출로 폴스루 - 캐시는 최적화지 의존성이 아니다
 */
@Slf4j
public class CachedTagoArrivalClient implements TagoArrivalPort {

    private static final String KEY_PREFIX = "arrival:cache:";
    private static final TypeReference<List<Arrival>> ARRIVAL_LIST = new TypeReference<>() {};

    private final TagoArrivalPort delegate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final Counter hits;
    private final Counter misses;

    public CachedTagoArrivalClient(TagoArrivalPort delegate,
                                   RedisTemplate<String, Object> redisTemplate,
                                   ObjectMapper objectMapper,
                                   MeterRegistry meterRegistry,
                                   long ttlSeconds) {
        if (ttlSeconds <= 0) {
            throw new IllegalStateException("arrival-cache.ttl-seconds는 양수여야 함: " + ttlSeconds);
        }
        this.delegate = delegate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.hits = meterRegistry.counter("arrival.cache.requests", "result", "hit");
        this.misses = meterRegistry.counter("arrival.cache.requests", "result", "miss");
    }

    @Override
    public List<Arrival> fetchRealtimeArrivals(String cityCode, String nodeId) {
        // 키에 노선이 없는 이유: TAGO 응답이 정류장 단위(전 노선 포함)라 노선으로 쪼개면 같은 답을 여러 장으로 저장하게 됨
        String key = KEY_PREFIX + cityCode + ":" + nodeId;

        // 1. 캐시 먼저 - 적중이면 TAGO에 안 간다 (miss 수 = 실제 TAGO 호출 수)
        List<Arrival> cached = readCache(key);
        if (cached != null) {
            hits.increment();
            return cached;
        }

        // 2. 미스면 원 호출 - 실패는 예외로 전파되므로 저장 코드에 도달하지 않는다 (실패 캐시 금지)
        misses.increment();
        List<Arrival> arrivals = delegate.fetchRealtimeArrivals(cityCode, nodeId);

        // 3. 빈 응답은 저장하지 않는다 - "버스 없음"이 TTL 동안 전 유저에게 박제되면 안 됨
        if (!arrivals.isEmpty()) {
            writeCache(key, arrivals);
        }
        return arrivals;
    }

    /** 캐시 조회 실패(Redis 장애·역직렬화 오류)는 미스로 취급 - 원 호출로 폴스루 */
    private List<Arrival> readCache(String key) {
        try {
            Object json = redisTemplate.opsForValue().get(key);
            return json == null ? null : objectMapper.readValue((String) json, ARRIVAL_LIST);
        } catch (Exception e) {
            log.warn("[ARRIVAL CACHE] 조회 실패 - 캐시 없이 진행 ({})", e.getMessage());
            return null;
        }
    }

    /** 저장 실패는 응답에 영향 없음 - 다음 요청이 원 호출로 갈 뿐 */
    private void writeCache(String key, List<Arrival> arrivals) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(arrivals), ttl);
        } catch (Exception e) {
            log.warn("[ARRIVAL CACHE] 저장 실패 ({})", e.getMessage());
        }
    }
}
