package com.example.scaffold.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.scaffold.user.UserService;

import java.util.Set;

/**
 * Cache Warmup Service
 * 
 * This service handles cache warming strategies for frequently accessed data
 * to improve application performance during startup and operation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheWarmupService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    
    /**
     * Warm up caches when application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmupCaches() {
        log.info("Starting cache warmup process...");
        
        try {
            // Warm up critical caches
            warmupUserCaches();
            warmupCategoryCaches();
            
            log.info("Cache warmup completed successfully");
        } catch (Exception e) {
            log.error("Error during cache warmup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Warm up user-related caches
     */
    private void warmupUserCaches() {
        log.debug("Warming up user caches...");
        
        try {
            // Use UserService to warm up the cache
            userService.warmupUserCache();
            
            Cache userCache = cacheManager.getCache(CacheConfig.USER_CACHE);
            Cache userByEmailCache = cacheManager.getCache(CacheConfig.USER_BY_EMAIL_CACHE);
            
            if (userCache != null && userByEmailCache != null) {
                log.debug("User cache structures initialized and warmed");
            }
        } catch (Exception e) {
            log.error("Error warming up user caches: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Warm up category caches
     */
    private void warmupCategoryCaches() {
        log.debug("Warming up category caches...");
        
        // Categories are good candidates for warmup since they're:
        // - Accessed frequently
        // - Change infrequently
        // - Relatively small in size
        
        Cache categoryCache = cacheManager.getCache(CacheConfig.CATEGORY_CACHE);
        if (categoryCache != null) {
            log.debug("Category cache structure initialized");
        }
    }
    
    /**
     * Manual cache warmup for specific cache
     */
    public void warmupCache(String cacheName) {
        log.info("Manual warmup requested for cache: {}", cacheName);
        
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("Cache '{}' not found for warmup", cacheName);
            return;
        }
        
        switch (cacheName) {
            case CacheConfig.USER_CACHE:
            case CacheConfig.USER_BY_EMAIL_CACHE:
            case CacheConfig.USER_SEARCH_CACHE:
                warmupUserCaches();
                break;
            case CacheConfig.CATEGORY_CACHE:
                warmupCategoryCaches();
                break;
            default:
                log.debug("No specific warmup strategy for cache: {}", cacheName);
        }
    }
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        log.info("Clearing all caches...");
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.debug("Cleared cache: {}", cacheName);
            }
        });
        
        log.info("All caches cleared");
    }
    
    /**
     * Clear specific cache
     */
    public void clearCache(String cacheName) {
        log.info("Clearing cache: {}", cacheName);
        
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("Cache '{}' cleared", cacheName);
        } else {
            log.warn("Cache '{}' not found", cacheName);
        }
    }
    
    /**
     * Get cache statistics
     */
    public void logCacheStats() {
        log.info("Cache Statistics:");
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            try {
                // Get cache size using Redis commands
                Set<String> keys = redisTemplate.keys(cacheName + "*");
                int size = keys != null ? keys.size() : 0;
                
                log.info("  {}: {} entries", cacheName, size);
            } catch (Exception e) {
                log.warn("Could not get stats for cache '{}': {}", cacheName, e.getMessage());
            }
        });
    }
}