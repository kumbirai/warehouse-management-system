package com.ccbsa.wms.gateway.filter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/**
 * Gateway filter that extracts or generates correlation ID and propagates it to downstream services.
 *
 * <p>This filter ensures that every request has a correlation ID for distributed tracing.
 * If a correlation ID is present in the incoming request header, it is used. Otherwise,
 * a new correlation ID is generated. The correlation ID is then added to the request headers
 * for all downstream service calls.</p>
 *
 * <p>Correlation ID header: X-Correlation-Id</p>
 *
 * <p>Usage in application.yml:</p>
 * <pre>{@code
 * spring:
 *   cloud:
 *     gateway:
 *       routes:
 *         - id: user-service
 *           filters:
 *             - CorrelationContext
 * }</pre>
 */
@Component
public class CorrelationContextFilter extends AbstractGatewayFilterFactory<CorrelationContextFilter.Config> {
    private static final Logger logger = LoggerFactory.getLogger(CorrelationContextFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    public CorrelationContextFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);

            // Generate correlation ID if not present
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = UUID.randomUUID().toString();
                logger.debug("Generated new correlation ID: {}", correlationId);
            } else {
                correlationId = correlationId.trim();
                logger.debug("Using existing correlation ID: {}", correlationId);
            }

            // Add correlation ID to request headers for downstream services
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .build();

            return chain.filter(exchange.mutate()
                    .request(modifiedRequest)
                    .build());
        };
    }

    /**
     * Configuration class for CorrelationContextFilter.
     * Currently empty but available for future configuration options.
     */
    public static class Config {
        // Configuration properties if needed
    }
}

