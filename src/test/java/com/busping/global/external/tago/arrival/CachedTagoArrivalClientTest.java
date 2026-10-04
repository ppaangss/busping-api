package com.busping.global.external.tago.arrival;

import com.busping.arrival.domain.Arrival;
import com.busping.global.exception.custom.ExternalApiException;
import com.busping.global.exception.errorcode.TagoErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 캐시 데코레이터 단위 검증 - 저장/비저장 규칙과 장애 폴스루.
 * Redis·원 클라이언트는 mock - 실 Redis를 끼운 경로는 ArrivalCacheIntegrationTest가 검증한다.
 */
class CachedTagoArrivalClientTest {

    private static final String KEY = "arrival:cache:25:STA-001";

    private TagoArrivalPort delegate;
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOps;
    private ObjectMapper objectMapper;
    private CachedTagoArrivalClient cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        delegate = mock(TagoArrivalPort.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        objectMapper = new ObjectMapper();
        cache = new CachedTagoArrivalClient(delegate, redisTemplate, objectMapper, new SimpleMeterRegistry(), 15);
    }

    @Test
    @DisplayName("미스면 원 호출 결과를 TTL과 함께 저장하고 그대로 반환한다")
    void missFetchesAndStoresWithTtl() {
        List<Arrival> arrivals = List.of(arrival("R-1", 3));
        when(valueOps.get(KEY)).thenReturn(null);
        when(delegate.fetchRealtimeArrivals("25", "STA-001")).thenReturn(arrivals);

        List<Arrival> result = cache.fetchRealtimeArrivals("25", "STA-001");

        assertThat(result).isEqualTo(arrivals);
        verify(valueOps).set(eq(KEY), anyString(), eq(Duration.ofSeconds(15)));
    }

    @Test
    @DisplayName("히트면 원 호출 없이 캐시 내용으로 응답한다")
    void hitSkipsDelegate() throws Exception {
        List<Arrival> arrivals = List.of(arrival("R-1", 3), arrival("R-2", 12));
        when(valueOps.get(KEY)).thenReturn(objectMapper.writeValueAsString(arrivals));

        List<Arrival> result = cache.fetchRealtimeArrivals("25", "STA-001");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRouteId()).isEqualTo("R-1");
        assertThat(result.get(1).getRemainingMinutes()).isEqualTo(12);
        verify(delegate, never()).fetchRealtimeArrivals(any(), any());
    }

    @Test
    @DisplayName("빈 응답은 캐시하지 않는다 - 없는 도착정보가 TTL 동안 박제되면 안 됨")
    void emptyResponseIsNotCached() {
        when(valueOps.get(KEY)).thenReturn(null);
        when(delegate.fetchRealtimeArrivals("25", "STA-001")).thenReturn(List.of());

        List<Arrival> result = cache.fetchRealtimeArrivals("25", "STA-001");

        assertThat(result).isEmpty();
        verify(valueOps, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("원 호출 실패는 그대로 전파되고 캐시에 아무것도 남지 않는다")
    void failureIsPropagatedAndNotCached() {
        when(valueOps.get(KEY)).thenReturn(null);
        when(delegate.fetchRealtimeArrivals("25", "STA-001"))
                .thenThrow(new ExternalApiException(TagoErrorCode.COMMUNICATION_ERROR));

        assertThatThrownBy(() -> cache.fetchRealtimeArrivals("25", "STA-001"))
                .isInstanceOf(ExternalApiException.class);
        verify(valueOps, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("Redis 장애는 미스로 취급하고 원 호출로 폴스루한다 - 캐시는 의존성이 아니다")
    void redisFailureFallsThroughToDelegate() {
        List<Arrival> arrivals = List.of(arrival("R-1", 3));
        when(valueOps.get(KEY)).thenThrow(new RuntimeException("redis down"));
        when(delegate.fetchRealtimeArrivals("25", "STA-001")).thenReturn(arrivals);

        List<Arrival> result = cache.fetchRealtimeArrivals("25", "STA-001");

        assertThat(result).isEqualTo(arrivals);
    }

    @Test
    @DisplayName("TTL이 양수가 아니면 부팅을 막는다")
    void nonPositiveTtlFailsFast() {
        assertThatThrownBy(() -> new CachedTagoArrivalClient(
                delegate, redisTemplate, objectMapper, new SimpleMeterRegistry(), 0))
                .isInstanceOf(IllegalStateException.class);
    }

    private Arrival arrival(String routeId, int remainingMinutes) {
        return Arrival.builder()
                .routeId(routeId)
                .busNumber("77")
                .remainingMinutes(remainingMinutes)
                .remainingStops(2)
                .routeType("일반버스")
                .vehicleType("일반차량")
                .build();
    }
}
