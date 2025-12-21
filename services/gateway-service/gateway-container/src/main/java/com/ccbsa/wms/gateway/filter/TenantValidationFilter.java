package com.ccbsa.wms.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Gateway filter that validates tenant ID from JWT matches the requested tenant.
 *
 * <p>This filter ensures that:
 * <ul>
 *   <li>Tenant ID is present in JWT token</li>
 *   <li>If X-Tenant-Id header is present, it matches the JWT tenant ID</li>
 *   <li>Prevents cross-tenant access attempts</li>
 * </ul>
 *
 * <p>The filter extracts the tenant ID from the JWT token and injects it as the
 * X-Tenant-Id header for downstream services. If a tenant ID is explicitly requested
 * via the X-Tenant-Id header, it validates that it matches the JWT tenant ID.
 */
@Component
public class TenantValidationFilter extends AbstractGatewayFilterFactory<TenantValidationFilter.Config> {
    private static final Logger logger = LoggerFactory.getLogger(TenantValidationFilter.class);
    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String X_TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String TENANTS_LIST_PATH = "/api/v1/tenants";
    private static final java.util.Set<String> PUBLIC_ENDPOINTS =
            java.util.Set.of("/actuator/health", "/actuator/info", "/api/v1/bff/auth/login", "/api/v1/bff/auth/refresh", "/api/v1/bff/auth/logout");

    public TenantValidationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> ReactiveSecurityContextHolder.getContext().cast(SecurityContext.class).map(SecurityContext::getAuthentication).filter(Objects::nonNull)
                .filter(authentication -> authentication instanceof JwtAuthenticationToken).cast(JwtAuthenticationToken.class).map(JwtAuthenticationToken::getToken)
                .flatMap(jwt -> {
                    String jwtTenantId = extractTenantId(jwt);
                    String path = exchange.getRequest().getPath().value();
                    String method = exchange.getRequest().getMethod().name();

                    // Check if user has SYSTEM_ADMIN or ADMIN role
                    boolean isSystemAdmin = hasSystemAdminRole(jwt);
                    boolean isAdmin = hasAdminRole(jwt);
                    boolean isListEndpoint = isTenantsListEndpoint(path, method);

                    // Allow SYSTEM_ADMIN users to bypass tenant validation for all endpoints
                    if (isSystemAdmin) {
                        logger.debug("SYSTEM_ADMIN user detected for {} {} - bypassing tenant validation", method, path);
                        // SYSTEM_ADMIN users don't need tenant_id, allow request to proceed
                        ServerHttpRequest request = exchange.getRequest();
                        ServerHttpRequest.Builder requestBuilder = request.mutate();
                        // Don't set X-Tenant-Id header for SYSTEM_ADMIN
                        ServerHttpRequest modifiedRequest = requestBuilder.build();
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    }

                    // Allow ADMIN users to access list endpoint without tenant_id requirement
                    if (isListEndpoint && isAdmin) {
                        logger.debug("ADMIN user detected for list endpoint {} {} - bypassing tenant validation", method, path);
                        ServerHttpRequest request = exchange.getRequest();
                        ServerHttpRequest.Builder requestBuilder = request.mutate();
                        // Don't set X-Tenant-Id header for list endpoint
                        ServerHttpRequest modifiedRequest = requestBuilder.build();
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    }

                    // For non-SYSTEM_ADMIN and non-ADMIN users, or non-list endpoints, tenant_id is required
                    if (jwtTenantId == null || jwtTenantId.isEmpty()) {
                        logger.warn("Tenant ID not found in JWT token for {} {} from {}", method, path, exchange.getRequest().getRemoteAddress());
                        return handleError(exchange, HttpStatus.FORBIDDEN, "Tenant ID not found in token");
                    }

                    ServerHttpRequest request = exchange.getRequest();
                    String requestedTenantId = request.getHeaders().getFirst(X_TENANT_ID_HEADER);

                    // If tenant ID is explicitly requested, validate it matches JWT tenant
                    if (requestedTenantId != null && !requestedTenantId.isEmpty()) {
                        if (!jwtTenantId.equals(requestedTenantId)) {
                            logger.warn("Tenant ID mismatch for {} {}: JWT tenant={}, requested tenant={}", method, path, jwtTenantId, requestedTenantId);
                            return handleError(exchange, HttpStatus.FORBIDDEN, String.format("Tenant ID mismatch: token tenant does not match requested tenant"));
                        }
                    }

                    // Ensure X-Tenant-Id header is set from JWT
                    ServerHttpRequest.Builder requestBuilder = request.mutate();
                    requestBuilder.header(X_TENANT_ID_HEADER, jwtTenantId);

                    logger.debug("Tenant validation passed for {} {}: tenant={}", method, path, jwtTenantId);

                    ServerHttpRequest modifiedRequest = requestBuilder.build();
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                })
                // If SecurityContext is empty or authentication is not JWT, this means:
                // 1. The OAuth2 Resource Server should have already rejected unauthenticated requests
                // 2. If we reach here without authentication, it's an error condition
                // However, we should not block the request here - let it proceed and let the
                // downstream service or OAuth2 Resource Server handle it
                .switchIfEmpty(Mono.defer(() -> {
                    String path = exchange.getRequest().getPath().value();
                    String method = exchange.getRequest().getMethod().name();
                    // Check if this is a public endpoint - if so, no authentication is expected
                    boolean isPublicEndpoint = PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
                    if (!isPublicEndpoint) {
                        // Only log at DEBUG level for protected endpoints since OAuth2 Resource Server
                        // should have already validated authentication. If authentication is truly missing,
                        // the downstream service will reject it with a proper error response.
                        logger.debug("No JWT authentication found in SecurityContext for {} {} - OAuth2 Resource Server should have validated this", method, path);
                    }
                    // Allow request to proceed - OAuth2 Resource Server should have handled authentication
                    // If authentication is truly missing, the downstream service will reject it
                    return chain.filter(exchange);
                }));
    }

    /**
     * Extracts the tenant ID from the JWT token.
     *
     * @param jwt The JWT token
     * @return The tenant ID if present, null otherwise
     */
    private String extractTenantId(Jwt jwt) {
        Object tenantIdClaim = jwt.getClaim(TENANT_ID_CLAIM);
        if (tenantIdClaim != null) {
            return tenantIdClaim.toString();
        }
        return null;
    }

    /**
     * Checks if the JWT token contains the SYSTEM_ADMIN role.
     *
     * @param jwt The JWT token
     * @return true if the token contains SYSTEM_ADMIN role, false otherwise
     */
    private boolean hasSystemAdminRole(Jwt jwt) {
        return hasRole(jwt, SYSTEM_ADMIN_ROLE);
    }

    /**
     * Checks if the JWT token contains the ADMIN role.
     *
     * @param jwt The JWT token
     * @return true if the token contains ADMIN role, false otherwise
     */
    private boolean hasAdminRole(Jwt jwt) {
        return hasRole(jwt, ADMIN_ROLE);
    }

    /**
     * Checks if the request is for the tenants list endpoint. The list endpoint is GET /api/v1/tenants (without an ID path parameter).
     *
     * @param path   The request path
     * @param method The HTTP method
     * @return true if this is the list endpoint, false otherwise
     */
    private boolean isTenantsListEndpoint(String path, String method) {
        return "GET".equals(method) && TENANTS_LIST_PATH.equals(path);
    }

    /**
     * Handles errors by returning an appropriate HTTP response.
     *
     * @param exchange The server web exchange
     * @param status   The HTTP status code
     * @param message  The error message
     * @return Mono that completes when the error response is written
     */
    private Mono<Void> handleError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        String errorBody = String.format("{\"error\":{\"code\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}}", status.name(), message, Instant.now().toString());

        byte[] errorBytes = Objects.requireNonNull(errorBody.getBytes(StandardCharsets.UTF_8), "Error body bytes cannot be null");
        DataBuffer buffer = response.bufferFactory().wrap(errorBytes);

        Mono<DataBuffer> bufferMono = Objects.requireNonNull(Mono.just(buffer), "Buffer mono cannot be null");
        return response.writeWith(bufferMono);
    }

    /**
     * Checks if the JWT token contains a specific role.
     *
     * @param jwt  The JWT token
     * @param role The role to check for
     * @return true if the token contains the role, false otherwise
     */
    private boolean hasRole(Jwt jwt, String role) {
        Object realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> realmAccessMap = (Map<String, Object>) realmAccess;
            Object rolesObj = realmAccessMap.get(ROLES_CLAIM);
            if (rolesObj instanceof List) {
                @SuppressWarnings("unchecked") List<String> roles = (List<String>) rolesObj;
                return roles.contains(role);
            }
        }
        return false;
    }

    /**
     * Configuration class for TenantValidationFilter. Currently empty but available for future configuration options.
     */
    public static class Config {
        // Configuration properties if needed
    }
}

