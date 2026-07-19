package com.example.scaffold.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based rate limiting service
 * 
 * This service implements sliding window rate limiting using Redis counters
 * with automatic expiration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    /**
     * Check if a request is allowed based on rate limiting rules
     * 
     * @param key Unique identifier for the client/endpoint combination
     * @param maxRequests Maximum number of requests allowed
     * @param windowSeconds Time window in seconds
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        String redisKey = RATE_LIMIT_KEY_PREFIX + key;
        
        try {
            // Get current count
            String countStr = (String) redisTemplate.opsForValue().get(redisKey);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            
            if (currentCount >= maxRequests) {
                log.debug("Rate limit exceeded for key: {}, current: {}, max: {}", 
                         key, currentCount, maxRequests);
                return false;
            }
            
            // Increment counter
            Long newCount = redisTemplate.opsForValue().increment(redisKey);
            
            // Set expiration if this is the first request in the window
            if (newCount == 1) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
            }
            
            log.debug("Rate limit check passed for key: {}, count: {}/{}", 
                     key, newCount, maxRequests);
            return true;
            
        } catch (Exception e) {
            log.warn("Redis rate limiting failed for key: {}, allowing request", key, e);
            // If Redis fails, allow the request (fail open)
            return true;
        }
    }

    /**
     * Get remaining requests for a given key
     * 
     * @param key Unique identifier for the client/endpoint combination
     * @param maxRequests Maximum number of requests allowed
     * @return Number of remaining requests, or maxRequests if no current limit
     */
    public int getRemainingRequests(String key, int maxRequests) {
        String redisKey = RATE_LIMIT_KEY_PREFIX + key;
        
        try {
            String countStr = (String) redisTemplate.opsForValue().get(redisKey);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            return Math.max(0, maxRequests - currentCount);
        } catch (Exception e) {
            log.warn("Failed to get remaining requests for key: {}", key, e);
            return maxRequests; // If Redis fails, assume no requests made
        }
    }

    /**
     * Get TTL for a rate limit key
     * 
     * @param key Unique identifier for the client/endpoint combination
     * @return TTL in seconds, or -1 if key doesn't exist
     */
    public long getTtl(String key) {
        String redisKey = RATE_LIMIT_KEY_PREFIX + key;
        
        try {
            return redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to get TTL for key: {}", key, e);
            return -1;
        }
    }

    /**
     * Reset rate limit for a specific key (useful for testing or admin operations)
     * 
     * @param key Unique identifier for the client/endpoint combination
     */
    public void reset(String key) {
        String redisKey = RATE_LIMIT_KEY_PREFIX + key;
        
        try {
            redisTemplate.delete(redisKey);
            log.debug("Rate limit reset for key: {}", key);
        } catch (Exception e) {
            log.warn("Failed to reset rate limit for key: {}", key, e);
        }
    }

    /**
     * Build a rate limiting key from client identifier and endpoint
     * 
     * @param clientId Client identifier (IP address, user ID, etc.)
     * @param endpoint API endpoint path
     * @return Formatted rate limit key
     */
    public String buildKey(String clientId, String endpoint) {
        return clientId + ":" + endpoint;
    }
}