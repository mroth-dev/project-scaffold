package com.example.scaffold.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class DatabaseHealthIndicatorTest {

    @Test
    void reportsUpWhenConnectionIsValid() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getDatabaseProductVersion()).thenReturn("18.4");

        Health health = new DatabaseHealthIndicator(dataSource).health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("PostgreSQL", health.getDetails().get("database"));
        assertEquals("18.4", health.getDetails().get("version"));
        assertInstanceOf(Long.class, health.getDetails().get("response_time_ms"));
    }

    @Test
    void reportsDownWhenConnectionIsInvalid() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(false);

        Health health = new DatabaseHealthIndicator(dataSource).health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Database connection is not valid", health.getDetails().get("error"));
    }

    @Test
    void reportsDownWhenConnectionFails() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        Health health = new DatabaseHealthIndicator(dataSource).health();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(((String) health.getDetails().get("error")).contains("Connection refused"));
    }
}
