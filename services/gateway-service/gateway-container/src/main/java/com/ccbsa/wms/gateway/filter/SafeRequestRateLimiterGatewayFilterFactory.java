package com.ccbsa.wms.gateway.filter;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter.Response;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;

import reactor.core.publisher.Mono;

/**
 * Custom RequestRateLimiterGatewayFilterFactory that safely handles committed responses.
 * <p>
 * This implementation extends the default RequestRateLimiterGatewayFilterFactory and fixes
 * the issue where rate limit headers are attempted to be added to an already committed
 * response (e.g., when returning a 429 TOO_MANY_REQUESTS status).
 * <p>
 * The fix ensures that:
 * <ul>
 *   <li>Rate limit headers are added before the response is committed</li>
 *   <li>If the response is already committed, header addition is gracefully skipped</li>
 *   <li>No UnsupportedOperationException is thrown when rate limits are exceeded</li>
 * </ul>
 * <p>
 * This is a production-grade fix that ensures the gateway remains stable even under
 * high load when rate limits are frequently exceeded.
 * <p>
 * This class is registered as a bean in RateLimiterConfig to override the default
 * RequestRateLimiterGatewayFilterFactory from Spring Cloud Gateway.
 */
public class SafeRequestRateLimiterGatewayFilterFactory extends org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory {
    private static final Logger logger = LoggerFactory.getLogger(SafeRequestRateLimiterGatewayFilterFactory.class);
    private static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String REPLENISH_RATE_HEADER = "X-RateLimit-Replenish-Rate";
    private static final String BURST_CAPACITY_HEADER = "X-RateLimit-Burst-Capacity";
    private static final String REQUESTED_TOKENS_HEADER = "X-RateLimit-Requested-Tokens";

    private final RateLimiter<?> rateLimiter;
    private final KeyResolver keyResolver;

    /**
     * Constructs a new SafeRequestRateLimiterGatewayFilterFactory.
     *
     * @param rateLimiter the rate limiter to use, must not be null
     * @param keyResolver the key resolver to use, must not be null
     * @throws NullPointerException if rateLimiter or keyResolver is null
     */
    public SafeRequestRateLimiterGatewayFilterFactory(RateLimiter<?> rateLimiter, KeyResolver keyResolver) {
        super(rateLimiter, keyResolver);
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "RateLimiter cannot be null");
        this.keyResolver = Objects.requireNonNull(keyResolver, "KeyResolver cannot be null");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String routeId = route != null && route.getId() != null ? route.getId() : "unknown";

            return keyResolver.resolve(exchange).flatMap(key -> {
                if (key == null || key.isEmpty()) {
                    logger.warn("Key resolver returned null or empty key for route: {}", routeId);
                    return chain.filter(exchange);
                }

                final String resolvedKey = key; // Capture key for error handler
                return rateLimiter.isAllowed(routeId, key).flatMap(response -> {
                    // Add rate limit headers before checking if request is allowed
                    // This ensures headers are added before the response is committed
                    ServerHttpResponse httpResponse = exchange.getResponse();

                    // Check if response is already committed before adding headers
                    if (!httpResponse.isCommitted()) {
                        addRateLimitHeaders(httpResponse, response, config);
                    } else {
                        logger.debug("Response already committed, skipping rate limit headers for route: {}, key: {}", routeId, resolvedKey);
                    }

                    // If request is not allowed, return 429 before committing response
                    if (!response.isAllowed()) {
                        logger.debug("Rate limit exceeded for route: {}, key: {}", routeId, resolvedKey);

                        // Ensure headers are set before setting status
                        if (!httpResponse.isCommitted()) {
                            try {
                                httpResponse.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                httpResponse.getHeaders().add("Content-Type", "application/json");

                                String errorBody = String.format("{\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Rate limit exceeded\",\"route\":\"%s\"}}", routeId);
                                byte[] errorBytes = errorBody.getBytes();
                                DataBuffer buffer = httpResponse.bufferFactory().wrap(errorBytes);
                                return httpResponse.writeWith(Mono.just(buffer));
                            } catch (UnsupportedOperationException e) {
                                // Response was committed between check and set, log and return empty
                                logger.debug("Response committed during rate limit response setup for route: {}, key: {}", routeId, resolvedKey);
                                return Mono.empty();
                            }
                        } else {
                            // Response already committed, just log and return
                            logger.debug("Response already committed when rate limit exceeded for route: {}, key: {}", routeId, resolvedKey);
                            return Mono.empty();
                        }
                    }

                    return chain.filter(exchange);
                }).onErrorResume(throwable -> {
                    logger.error("Error in rate limiter for route: {}, key: {}", routeId, resolvedKey, throwable);
                    // On error, allow the request to proceed (graceful degradation)
                    return chain.filter(exchange);
                });
            }).switchIfEmpty(chain.filter(exchange));
        };
    }

    /**
     * Safely adds rate limit headers to the response.
     * <p>
     * This method checks if the response is committed before attempting to add headers,
     * preventing UnsupportedOperationException when headers are read-only.
     *
     * @param response            the HTTP response
     * @param rateLimiterResponse the rate limiter response containing header values
     * @param config              the filter configuration
     */
    private void addRateLimitHeaders(ServerHttpResponse response, Response rateLimiterResponse, Config config) {
        if (response.isCommitted()) {
            logger.debug("Response already committed, skipping rate limit headers");
            return;
        }

        try {
            Map<String, String> headers = rateLimiterResponse.getHeaders();

            // Add remaining header
            String remaining = headers.get(REMAINING_HEADER);
            if (remaining != null) {
                response.getHeaders().add(REMAINING_HEADER, remaining);
            }

            // Add replenish rate header
            String replenishRate = headers.get(REPLENISH_RATE_HEADER);
            if (replenishRate != null) {
                response.getHeaders().add(REPLENISH_RATE_HEADER, replenishRate);
            }

            // Add burst capacity header
            String burstCapacity = headers.get(BURST_CAPACITY_HEADER);
            if (burstCapacity != null) {
                response.getHeaders().add(BURST_CAPACITY_HEADER, burstCapacity);
            }

            // Add requested tokens header
            String requestedTokens = headers.get(REQUESTED_TOKENS_HEADER);
            if (requestedTokens != null) {
                response.getHeaders().add(REQUESTED_TOKENS_HEADER, requestedTokens);
            }
        } catch (UnsupportedOperationException e) {
            // If headers are read-only (response committed), log and continue
            logger.debug("Cannot add rate limit headers - response is read-only: {}", e.getMessage());
        }
    }
}

