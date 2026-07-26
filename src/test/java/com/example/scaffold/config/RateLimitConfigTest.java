package com.example.scaffold.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimitConfigTest {

    private RateLimitConfig config;

    @BeforeEach
    void setUp() {
        config = new RateLimitConfig();
        config.setDefaultSettings(new RateLimitConfig.RateLimitSettings(100, 60));
        config.setAuth(new RateLimitConfig.RateLimitSettings(5, 300));
        config.setApi(new RateLimitConfig.RateLimitSettings(1000, 60));
        config.setEndpoints(Map.of(
                "/api/auth/login", new RateLimitConfig.RateLimitSettings(10, 900),
                "/api/products/\\d+", new RateLimitConfig.RateLimitSettings(20, 60)));
    }

    @Test
    void exactEndpointMatchTakesPriority() {
        RateLimitConfig.RateLimitSettings settings = config.getSettingsForEndpoint("/api/auth/login");

        assertEquals(10, settings.getRequests());
        assertEquals(900, settings.getWindow());
    }

    @Test
    void regexEndpointPatternMatches() {
        RateLimitConfig.RateLimitSettings settings = config.getSettingsForEndpoint("/api/products/42");

        assertEquals(20, settings.getRequests());
        assertEquals(60, settings.getWindow());
    }

    @Test
    void unconfiguredAuthEndpointFallsBackToAuthTier() {
        assertSame(config.getAuth(), config.getSettingsForEndpoint("/auth/refresh"));
        assertSame(config.getAuth(), config.getSettingsForEndpoint("/api/users/register"));
    }

    @Test
    void unconfiguredApiEndpointFallsBackToApiTier() {
        assertSame(config.getApi(), config.getSettingsForEndpoint("/api/orders"));
    }

    @Test
    void nonApiEndpointFallsBackToDefaultTier() {
        assertSame(config.getDefaultSettings(), config.getSettingsForEndpoint("/products"));
    }
}
