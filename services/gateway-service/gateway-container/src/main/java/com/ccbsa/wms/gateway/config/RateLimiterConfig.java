package com.ccbsa.wms.gateway.config;

import java.net.InetSocketAddress;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.server.reactive.ServerHttpRequest;

import com.ccbsa.wms.gateway.filter.GracefulRedisRateLimiter;

import reactor.core.publisher.Mono;

/**
 * Configuration for rate limiting. Provides a unified key resolver that intelligently determines the rate limit key based on available request context (user ID > tenant ID > IP
 * address).
 * <p>
 * Also configures a graceful Redis rate limiter that allows requests when Redis is unavailable.
 */
@Configuration
public class RateLimiterConfig {
    /**
     * Unified key resolver for rate limiting. Priority order: 1. User ID (X-User-Id header) - most specific, enables per-user rate limiting 2. Tenant ID (X-Tenant-Id header) -
     * enables per-tenant rate limiting 3. IP address - fallback for
     * unauthenticated requests
     * <p>
     * This approach provides the most granular rate limiting possible based on available context, while gracefully degrading to less specific keys when needed.
     */
    @Bean
    public KeyResolver keyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();

            // Priority 1: User-based rate limiting (most granular)
            String userId = request.getHeaders()
                    .getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(String.format("user:%s", userId));
            }

            // Priority 2: Tenant-based rate limiting
            String tenantId = request.getHeaders()
                    .getFirst("X-Tenant-Id");
            if (tenantId != null && !tenantId.isEmpty()) {
                return Mono.just(String.format("tenant:%s", tenantId));
            }

            // Priority 3: IP-based rate limiting (fallback for unauthenticated requests)
            String remoteAddress = "unknown";
            InetSocketAddress remoteAddr = request.getRemoteAddress();
            if (remoteAddr != null && remoteAddr.getAddress() != null) {
                remoteAddress = remoteAddr.getAddress()
                        .getHostAddress();
            }
            return Mono.just(String.format("ip:%s", remoteAddress));
        };
    }

    /**
     * Primary rate limiter that gracefully handles Redis failures. This bean wraps Spring's auto-configured RedisRateLimiter and provides graceful degradation when Redis is
     * unavailable.
     * <p>
     * Spring Cloud Gateway will use this bean when RequestRateLimiter filter is configured in routes. The @Primary annotation ensures this bean is used instead of the default
     * RedisRateLimiter.
     */
    @Bean
    @Primary
    public GracefulRedisRateLimiter gracefulRedisRateLimiter(RedisRateLimiter redisRateLimiter) {
        return new GracefulRedisRateLimiter(redisRateLimiter);
    }
}

