package com.example.scaffold.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies application.yaml's app.rate-limit.* properties actually bind onto
 * RateLimitConfig - property-binding mismatches (field vs. yaml key name)
 * fail silently, so this needs a real Spring context, not a POJO test.
 */
@SpringBootTest
class RateLimitConfigBindingTest {

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @Test
    void customEndpointOverridesBindFromYaml() {
        RateLimitConfig.RateLimitSettings login = rateLimitConfig.getEndpoints().get("/api/auth/login");

        assertEquals(10, login.getRequests());
        assertEquals(900, login.getWindow());
    }

    @Test
    void defaultTierBindsFromYaml() {
        assertEquals(100, rateLimitConfig.getDefaultSettings().getRequests());
        assertEquals(60, rateLimitConfig.getDefaultSettings().getWindow());
    }
}
