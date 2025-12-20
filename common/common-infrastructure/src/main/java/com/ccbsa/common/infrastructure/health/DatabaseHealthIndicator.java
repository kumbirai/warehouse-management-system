package com.ccbsa.common.infrastructure.health;

import java.util.Objects;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Health indicator for database connectivity.
 */
public class DatabaseHealthIndicator implements HealthIndicator {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    @Override
    public Health health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Health.up().withDetail("database", "Available").build();
        } catch (Exception e) {
            return Health.down().withDetail("database", "Unavailable").withException(e).build();
        }
    }
}

