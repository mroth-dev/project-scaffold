package com.example.scaffold.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate Limiting Configuration
 *
 * This configuration class sets up rate limiting with Redis-based counters
 * and configurable limits per endpoint type.
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitConfig {

    /**
     * Master switch for rate limiting enforcement
     */
    private boolean enabled = true;

    /**
     * Default rate limiting settings
     */
    private RateLimitSettings defaultSettings = new RateLimitSettings(100, 60);

    /**
     * Authentication endpoint rate limiting settings
     */
    private RateLimitSettings auth = new RateLimitSettings(5, 300);

    /**
     * API endpoint rate limiting settings
     */
    private RateLimitSettings api = new RateLimitSettings(1000, 60);

    /**
     * Custom rate limits by endpoint pattern
     */
    private Map<String, RateLimitSettings> endpoints = new HashMap<>();

    /**
     * Rate limiting service bean
     */
    @Bean
    public RateLimitService rateLimitService(StringRedisTemplate redisTemplate) {
        RateLimitService service = new RateLimitService(redisTemplate);
        log.info("Rate limiting service configured with default limit: {} requests per {} seconds",
                defaultSettings.getRequests(), defaultSettings.getWindow());
        return service;
    }

    /**
     * Get rate limit settings for a specific endpoint pattern
     */
    public RateLimitSettings getSettingsForEndpoint(String endpoint) {
        // Check for exact match first
        RateLimitSettings settings = endpoints.get(endpoint);
        if (settings != null) {
            return settings;
        }

        // Check for pattern matches
        for (Map.Entry<String, RateLimitSettings> entry : endpoints.entrySet()) {
            if (endpoint.matches(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Check endpoint type
        if (endpoint.startsWith("/auth/") || endpoint.contains("/login") || endpoint.contains("/register")) {
            return auth;
        } else if (endpoint.startsWith("/api/")) {
            return api;
        }

        // Return default settings
        return defaultSettings;
    }

    /**
     * Rate limit settings data class
     */
    @Data
    public static class RateLimitSettings {
        private int requests;
        private int window; // in seconds

        public RateLimitSettings() {}

        public RateLimitSettings(int requests, int window) {
            this.requests = requests;
            this.window = window;
        }
    }
}
