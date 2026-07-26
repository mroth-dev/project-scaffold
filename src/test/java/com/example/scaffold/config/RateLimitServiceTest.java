package com.example.scaffold.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Test for Rate Limit Service
 */
class RateLimitServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private RateLimitService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new RateLimitService(redisTemplate);
    }

    @Test
    void buildKeyWorksCorrectly() {
        String key = service.buildKey("192.168.1.1", "/api/users");
        assertEquals("192.168.1.1:/api/users", key);
    }

    @Test
    void isAllowedOnFirstRequestSetsExpiration() {
        when(valueOperations.get("rate_limit:client:endpoint")).thenReturn(null);
        when(valueOperations.increment("rate_limit:client:endpoint")).thenReturn(1L);

        boolean allowed = service.isAllowed("client:endpoint", 5, 60);

        assertTrue(allowed);
        verify(redisTemplate).expire("rate_limit:client:endpoint", Duration.ofSeconds(60));
    }

    @Test
    void isAllowedWithinLimitDoesNotResetExpiration() {
        when(valueOperations.get("rate_limit:client:endpoint")).thenReturn("2");
        when(valueOperations.increment("rate_limit:client:endpoint")).thenReturn(3L);

        boolean allowed = service.isAllowed("client:endpoint", 5, 60);

        assertTrue(allowed);
        verify(redisTemplate, never()).expire(any(), any(Duration.class));
    }

    @Test
    void isAllowedRejectsOnceLimitReached() {
        when(valueOperations.get("rate_limit:client:endpoint")).thenReturn("5");

        boolean allowed = service.isAllowed("client:endpoint", 5, 60);

        assertFalse(allowed);
        verify(valueOperations, never()).increment(any());
    }

    @Test
    void isAllowedFailsOpenWhenRedisErrors() {
        when(valueOperations.get(any())).thenThrow(new RuntimeException("Redis unavailable"));

        boolean allowed = service.isAllowed("client:endpoint", 5, 60);

        assertTrue(allowed);
    }

    @Test
    void getRemainingRequestsComputesDifference() {
        when(valueOperations.get("rate_limit:client:endpoint")).thenReturn("3");

        assertEquals(2, service.getRemainingRequests("client:endpoint", 5));
    }

    @Test
    void getRemainingRequestsNeverGoesNegative() {
        when(valueOperations.get("rate_limit:client:endpoint")).thenReturn("9");

        assertEquals(0, service.getRemainingRequests("client:endpoint", 5));
    }

    @Test
    void getRemainingRequestsFallsBackToMaxWhenRedisErrors() {
        when(valueOperations.get(any())).thenThrow(new RuntimeException("Redis unavailable"));

        assertEquals(5, service.getRemainingRequests("client:endpoint", 5));
    }

    @Test
    void getTtlDelegatesToRedis() {
        when(redisTemplate.getExpire("rate_limit:client:endpoint", TimeUnit.SECONDS)).thenReturn(42L);

        assertEquals(42L, service.getTtl("client:endpoint"));
    }

    @Test
    void getTtlFallsBackToNegativeOneWhenRedisErrors() {
        when(redisTemplate.getExpire(any(), eq(TimeUnit.SECONDS))).thenThrow(new RuntimeException("boom"));

        assertEquals(-1L, service.getTtl("client:endpoint"));
    }

    @Test
    void resetDeletesTheKey() {
        service.reset("client:endpoint");

        verify(redisTemplate).delete("rate_limit:client:endpoint");
    }
}
