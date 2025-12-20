package com.ccbsa.wms.gateway.filter;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;

import reactor.core.publisher.Mono;

/**
 * Graceful Redis Rate Limiter that allows requests when Redis is unavailable.
 * <p>
 * This implementation wraps the standard RedisRateLimiter and provides graceful degradation by allowing all requests when Redis connection fails. This ensures the gateway remains
 * functional even when Redis is down.
 * <p>
 * Production Note: In production, consider implementing an in-memory fallback rate limiter or circuit breaker pattern for better control.
 */
public class GracefulRedisRateLimiter implements RateLimiter<RedisRateLimiter.Config> {
    private static final Logger logger = LoggerFactory.getLogger(GracefulRedisRateLimiter.class);
    private final RedisRateLimiter redisRateLimiter;
    private volatile boolean redisAvailable = true;

    /**
     * Constructs a new GracefulRedisRateLimiter wrapping the provided RedisRateLimiter.
     * <p>
     * The RedisRateLimiter is stored as a final field and used internally. It is not exposed through any public methods, maintaining proper encapsulation.
     *
     * @param redisRateLimiter the RedisRateLimiter to wrap, must not be null
     * @throws NullPointerException if redisRateLimiter is null
     */
    public GracefulRedisRateLimiter(RedisRateLimiter redisRateLimiter) {
        this.redisRateLimiter = Objects.requireNonNull(redisRateLimiter, "RedisRateLimiter cannot be null");
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        // Always try Redis first, but allow on error
        return redisRateLimiter.isAllowed(routeId, id).doOnNext(response -> {
            // Redis is working
            if (!redisAvailable) {
                logger.info("Redis connection restored. Re-enabling rate limiting.");
                redisAvailable = true;
            }
        }).doOnError(error -> {
            if (redisAvailable) {
                logger.warn(
                        "Redis connection error during rate limiting for route: {}, id: {}. " + "Allowing requests and disabling rate limiting until Redis is available. Error: {}",
                        routeId, id, error.getMessage());
                redisAvailable = false;
            }
        }).onErrorResume(error -> {
            // Allow request when Redis fails - graceful degradation
            logger.debug("Allowing request due to Redis error: {}", error.getMessage());
            return Mono.just(new Response(true, getDefaultHeaders()));
        });
    }

    /**
     * Returns default headers for rate limit response when Redis is unavailable.
     */
    private Map<String, String> getDefaultHeaders() {
        return Map.of("X-RateLimit-Remaining", "-1", "X-RateLimit-Requested-Tokens", "1", "X-RateLimit-Burst-Capacity", "unlimited", "X-RateLimit-Replenish-Rate", "unlimited");
    }

    @Override
    public Class<RedisRateLimiter.Config> getConfigClass() {
        return RedisRateLimiter.Config.class;
    }

    @Override
    public RedisRateLimiter.Config newConfig() {
        return redisRateLimiter.newConfig();
    }

    @Override
    public Map<String, RedisRateLimiter.Config> getConfig() {
        return redisRateLimiter.getConfig();
    }
}

