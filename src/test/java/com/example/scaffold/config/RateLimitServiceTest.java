package com.example.scaffold.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Test for Rate Limit Service
 */
class RateLimitServiceTest {

    @Test
    void buildKeyWorksCorrectly() {
        RateLimitService service = new RateLimitService(mock(org.springframework.data.redis.core.RedisTemplate.class));
        String key = service.buildKey("192.168.1.1", "/api/users");
        assertEquals("192.168.1.1:/api/users", key);
    }
}