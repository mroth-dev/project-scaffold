package com.example.scaffold.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Cache Monitoring Service
 * 
 * This service provides monitoring and statistics for Redis cache operations
 * to help with performance analysis and troubleshooting.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheMonitoringService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Get comprehensive cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Overall Redis info
            stats.put("redis_info", getRedisInfo());
            
            // Cache-specific metrics
            stats.put("cache_metrics", getCacheMetrics());
            
            // Memory usage
            stats.put("memory_usage", getMemoryUsage());
            
        } catch (Exception e) {
            log.error("Error collecting cache statistics: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Get Redis server information
     */
    private Map<String, Object> getRedisInfo() {
        Map<String, Object> redisInfo = new HashMap<>();
        
        try {
            // Test connectivity
            String ping = redisTemplate.getConnectionFactory().getConnection().ping();
            redisInfo.put("ping", ping);
            redisInfo.put("connected", "PONG".equals(ping));
            
        } catch (Exception e) {
            log.error("Error getting Redis info: {}", e.getMessage());
            redisInfo.put("connected", false);
            redisInfo.put("error", e.getMessage());
        }
        
        return redisInfo;
    }
    
    /**
     * Get cache-specific metrics
     */
    private Map<String, Object> getCacheMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Get metrics for each cache type
            metrics.put(CacheConfig.USER_CACHE, getCacheMetricsForType(CacheConfig.USER_CACHE));
            metrics.put(CacheConfig.USER_BY_EMAIL_CACHE, getCacheMetricsForType(CacheConfig.USER_BY_EMAIL_CACHE));
            metrics.put(CacheConfig.USER_SEARCH_CACHE, getCacheMetricsForType(CacheConfig.USER_SEARCH_CACHE));
            metrics.put(CacheConfig.PRODUCT_CACHE, getCacheMetricsForType(CacheConfig.PRODUCT_CACHE));
            metrics.put(CacheConfig.CATEGORY_CACHE, getCacheMetricsForType(CacheConfig.CATEGORY_CACHE));
            metrics.put(CacheConfig.ORDER_CACHE, getCacheMetricsForType(CacheConfig.ORDER_CACHE));
            
        } catch (Exception e) {
            log.error("Error collecting cache metrics: {}", e.getMessage());
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }
    
    /**
     * Get metrics for specific cache type
     */
    private Map<String, Object> getCacheMetricsForType(String cacheType) {
        Map<String, Object> cacheMetrics = new HashMap<>();
        
        try {
            // Get all keys for this cache type
            String keyPattern = "scaffold:cache:" + cacheType + "*";
            Set<String> keys = redisTemplate.keys(keyPattern);
            
            cacheMetrics.put("key_count", keys != null ? keys.size() : 0);
            cacheMetrics.put("pattern", keyPattern);
            
            if (keys != null && !keys.isEmpty()) {
                // Sample a few keys to get TTL information
                String sampleKey = keys.iterator().next();
                Long ttl = redisTemplate.getExpire(sampleKey);
                cacheMetrics.put("sample_ttl_seconds", ttl);
            }
            
        } catch (Exception e) {
            log.debug("Error getting metrics for cache type '{}': {}", cacheType, e.getMessage());
            cacheMetrics.put("error", e.getMessage());
        }
        
        return cacheMetrics;
    }
    
    /**
     * Get memory usage information
     */
    private Map<String, Object> getMemoryUsage() {
        Map<String, Object> memoryInfo = new HashMap<>();
        
        try {
            // This would require Redis INFO command access
            // For now, we'll provide basic information
            memoryInfo.put("status", "monitoring_enabled");
            memoryInfo.put("note", "Detailed memory metrics require Redis INFO access");
            
        } catch (Exception e) {
            log.error("Error getting memory usage: {}", e.getMessage());
            memoryInfo.put("error", e.getMessage());
        }
        
        return memoryInfo;
    }
    
    /**
     * Log cache performance metrics
     */
    public void logCachePerformance() {
        try {
            Map<String, Object> stats = getCacheStatistics();
            log.info("=== Cache Performance Report ===");
            
            Map<String, Object> cacheMetrics = (Map<String, Object>) stats.get("cache_metrics");
            if (cacheMetrics != null) {
                cacheMetrics.forEach((cacheName, metrics) -> {
                    if (metrics instanceof Map) {
                        Map<String, Object> cacheStats = (Map<String, Object>) metrics;
                        Object keyCount = cacheStats.get("key_count");
                        Object ttl = cacheStats.get("sample_ttl_seconds");
                        
                        log.info("  {}: {} keys, TTL: {} seconds", 
                            cacheName, 
                            keyCount != null ? keyCount : "unknown",
                            ttl != null ? ttl : "unknown"
                        );
                    }
                });
            }
            
            Map<String, Object> redisInfo = (Map<String, Object>) stats.get("redis_info");
            if (redisInfo != null) {
                log.info("  Redis Status: {}", redisInfo.get("connected"));
            }
            
        } catch (Exception e) {
            log.error("Error logging cache performance: {}", e.getMessage());
        }
    }
    
    /**
     * Clear expired keys manually (if needed)
     */
    public int clearExpiredKeys() {
        int cleared = 0;
        
        try {
            // Get all cache keys
            Set<String> allKeys = redisTemplate.keys("scaffold:cache:*");
            
            if (allKeys != null) {
                for (String key : allKeys) {
                    Long ttl = redisTemplate.getExpire(key);
                    if (ttl != null && ttl == -2) { // Key doesn't exist (expired)
                        redisTemplate.delete(key);
                        cleared++;
                    }
                }
            }
            
            if (cleared > 0) {
                log.info("Manually cleared {} expired cache keys", cleared);
            }
            
        } catch (Exception e) {
            log.error("Error clearing expired keys: {}", e.getMessage());
        }
        
        return cleared;
    }
}