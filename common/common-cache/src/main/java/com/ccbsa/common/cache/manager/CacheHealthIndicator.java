package com.ccbsa.common.cache.manager;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Redis Cache Health Indicator.
 * <p>
 * Checks Redis connectivity and reports health status.
 * <p>
 * Health States: - UP: Redis is reachable and responding to PING - DOWN: Redis is unreachable or not responding
 * <p>
 * Exposed via: /actuator/health
 */
@Component
public class CacheHealthIndicator
        implements HealthIndicator {

    private final RedisConnectionFactory connectionFactory;

    public CacheHealthIndicator(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            String pong = connection.ping();

            if ("PONG".equalsIgnoreCase(pong)) {
                return Health.up()
                        .withDetail("redis", "Available")
                        .withDetail("response", pong)
                        .build();
            } else {
                return Health.down()
                        .withDetail("redis", "Unexpected response")
                        .withDetail("response", pong)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "Unavailable")
                    .withException(e)
                    .build();
        }
    }
}
