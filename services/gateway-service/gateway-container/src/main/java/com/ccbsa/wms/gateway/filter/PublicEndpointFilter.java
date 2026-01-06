package com.ccbsa.wms.gateway.filter;

import java.util.Set;

import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * WebFilter that marks public endpoints to skip OAuth2 Resource Server authentication.
 *
 * <p>This filter runs before Spring Security filters and sets an attribute on the exchange
 * to indicate that the request is for a public endpoint. This allows downstream filters to skip authentication for these endpoints.
 *
 * <p>Public endpoints:
 * <ul>
 *   <li>/actuator/health</li>
 *   <li>/actuator/info</li>
 *   <li>/api/v1/bff/auth/login</li>
 *   <li>/api/v1/bff/auth/refresh</li>
 *   <li>/api/v1/bff/auth/logout</li>
 * </ul>
 */
@Slf4j
@Component
@Order(-100) // Run before Spring Security filters
public class PublicEndpointFilter implements WebFilter {
    /**
     * Attribute key to mark public endpoints.
     */
    public static final String IS_PUBLIC_ENDPOINT_ATTR = "IS_PUBLIC_ENDPOINT";
    /**
     * Public endpoints that do not require authentication.
     */
    private static final Set<String> PUBLIC_ENDPOINTS =
            Set.of("/actuator/health", "/actuator/info", "/api/v1/bff/auth/login", "/api/v1/bff/auth/refresh", "/api/v1/bff/auth/logout");

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isPublicEndpoint(path)) {
            log.debug("Marking public endpoint: {}", path);
            // Set attribute to indicate this is a public endpoint
            exchange.getAttributes().put(IS_PUBLIC_ENDPOINT_ATTR, true);
        }

        return chain.filter(exchange);
    }

    /**
     * Checks if the given path is a public endpoint.
     *
     * @param path The request path to check
     * @return true if the path is a public endpoint, false otherwise
     */
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.contains(path);
    }
}

