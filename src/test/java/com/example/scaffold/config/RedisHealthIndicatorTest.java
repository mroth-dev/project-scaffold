package com.example.scaffold.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class RedisHealthIndicatorTest {

    @Test
    void reportsUpWhenPingSucceeds() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");

        Health health = new RedisHealthIndicator(connectionFactory).health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("PONG", health.getDetails().get("ping"));
        assertInstanceOf(Long.class, health.getDetails().get("response_time_ms"));
    }

    @Test
    void reportsDownWhenPingIsUnexpected() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("WRONG");

        Health health = new RedisHealthIndicator(connectionFactory).health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("WRONG", health.getDetails().get("response"));
    }

    @Test
    void reportsDownWhenConnectionFails() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        when(connectionFactory.getConnection())
                .thenThrow(new RedisConnectionFailureException("Unable to connect to Redis"));

        Health health = new RedisHealthIndicator(connectionFactory).health();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(((String) health.getDetails().get("error")).contains("Unable to connect to Redis"));
    }
}
