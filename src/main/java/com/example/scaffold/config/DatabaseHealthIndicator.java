package com.example.scaffold.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom Database Health Indicator
 *
 * Reports connection-pool reachability and basic server details for
 * /actuator/health, so load balancers and the readiness probe can detect a
 * database outage. Registered under the bean name "dbHealthIndicator" -
 * Spring Boot's convention for the DataSource health contributor - so this
 * replaces the default "db" indicator instead of appearing alongside it.
 */
@Slf4j
@Component("dbHealthIndicator")
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            long startTime = System.currentTimeMillis();
            boolean valid = connection.isValid(VALIDATION_TIMEOUT_SECONDS);
            long responseTime = System.currentTimeMillis() - startTime;

            if (!valid) {
                return Health.down()
                        .withDetail("error", "Database connection is not valid")
                        .build();
            }

            Health.Builder builder = Health.up()
                    .withDetail("response_time_ms", responseTime);

            withDatabaseInfo(connection, builder);

            return builder.build();
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down(e).build();
        }
    }

    private void withDatabaseInfo(Connection connection, Health.Builder builder) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            builder.withDetail("database", metaData.getDatabaseProductName())
                    .withDetail("version", metaData.getDatabaseProductVersion());
        } catch (Exception e) {
            log.debug("Could not retrieve database metadata: {}", e.getMessage());
        }
    }
}
