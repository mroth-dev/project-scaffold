package com.example.scaffold.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Custom Redis Health Indicator
 *
 * This component provides detailed health information about Redis connectivity
 * and performance for monitoring and load balancer health checks. It replaces
 * Spring Boot's default Redis health contributor (Boot backs off when a bean
 * named "redisHealthIndicator" is present) so /actuator/health reports response
 * time and server version alongside the plain UP/DOWN status.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            long startTime = System.currentTimeMillis();
            String pong = connection.ping();
            long responseTime = System.currentTimeMillis() - startTime;

            if (!"PONG".equals(pong)) {
                return Health.down()
                        .withDetail("error", "Redis ping failed")
                        .withDetail("response", pong)
                        .build();
            }

            Health.Builder builder = Health.up()
                    .withDetail("response_time_ms", responseTime)
                    .withDetail("ping", pong);

            withServerVersion(connection, builder);

            return builder.build();
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down(e).build();
        }
    }

    private void withServerVersion(RedisConnection connection, Health.Builder builder) {
        try {
            Object infoObj = connection.info();
            String info = infoObj != null ? infoObj.toString() : null;
            if (info == null || info.isEmpty()) {
                return;
            }
            for (String line : info.split("\r\n")) {
                if (line.startsWith("redis_version:")) {
                    builder.withDetail("version", line.split(":")[1]);
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("Could not retrieve Redis server info: {}", e.getMessage());
        }
    }
}
