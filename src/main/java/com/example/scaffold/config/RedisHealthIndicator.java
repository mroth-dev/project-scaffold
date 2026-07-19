package com.example.scaffold.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom Redis Health Indicator
 * 
 * This component provides detailed health information about Redis connectivity
 * and performance for monitoring and load balancer health checks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * Check Redis health and return status information
     */
    public Map<String, Object> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            
            if (connection == null) {
                health.put("status", "DOWN");
                health.put("error", "Unable to get Redis connection");
                return health;
            }

            // Test Redis connectivity with a simple ping
            long startTime = System.currentTimeMillis();
            String pong = connection.ping();
            long responseTime = System.currentTimeMillis() - startTime;

            // Check if ping was successful
            if (!"PONG".equals(pong)) {
                health.put("status", "DOWN");
                health.put("error", "Redis ping failed");
                health.put("response", pong);
                return health;
            }

            // Success response
            health.put("status", "UP");
            health.put("response_time_ms", responseTime);
            health.put("ping", pong);

            try {
                // Add server info if available
                Object infoObj = connection.info();
                String info = infoObj != null ? infoObj.toString() : null;
                if (info != null && !info.isEmpty()) {
                    // Parse version from info
                    String[] lines = info.split("\r\n");
                    for (String line : lines) {
                        if (line.startsWith("redis_version:")) {
                            health.put("version", line.split(":")[1]);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Could not retrieve Redis server info: {}", e.getMessage());
            }

            connection.close();
            return health;

        } catch (Exception e) {
            log.error("Redis health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return health;
        }
    }
}