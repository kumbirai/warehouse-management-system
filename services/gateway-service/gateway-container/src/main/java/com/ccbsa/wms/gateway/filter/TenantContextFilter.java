package com.ccbsa.wms.gateway.filter;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Gateway filter that extracts tenant context from JWT token and injects it as headers.
 *
 * <p>This filter extracts contextual information from the JWT token and injects it
 * as HTTP headers for downstream services. This enables services to access tenant and user context without parsing JWT tokens themselves.
 *
 * <p>Extracts from JWT:
 * <ul>
 *   <li>tenant_id from JWT claim</li>
 *   <li>user_id from JWT subject (sub claim)</li>
 *   <li>roles from JWT realm_access.roles claim</li>
 * </ul>
 *
 * <p>Injects headers:
 * <ul>
 *   <li>X-Tenant-Id - Tenant identifier</li>
 *   <li>X-User-Id - User identifier</li>
 *   <li>X-Role - Comma-separated list of roles</li>
 * </ul>
 *
 * <p>If no JWT token is present, the filter allows the request to proceed without
 * modifying headers. This enables public endpoints to work correctly.
 */
@Component
public class TenantContextFilter extends AbstractGatewayFilterFactory<TenantContextFilter.Config> {
    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String X_TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_ROLE_HEADER = "X-Role";

    public TenantContextFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return ReactiveSecurityContextHolder.getContext().cast(SecurityContext.class).map(SecurityContext::getAuthentication)
                    .filter(auth -> auth instanceof JwtAuthenticationToken).cast(JwtAuthenticationToken.class).map(JwtAuthenticationToken::getToken).flatMap(jwt -> {
                        ServerHttpRequest request = exchange.getRequest();
                        ServerHttpRequest.Builder requestBuilder = request.mutate();

                        // Extract tenant ID from JWT
                        String tenantId = extractTenantId(jwt);
                        if (tenantId != null) {
                            requestBuilder.header(X_TENANT_ID_HEADER, tenantId);
                        }

                        // Extract user ID from JWT subject
                        String userId = jwt.getSubject();
                        if (userId != null) {
                            requestBuilder.header(X_USER_ID_HEADER, userId);
                        }

                        // Extract roles from JWT
                        String roles = extractRoles(jwt);
                        if (roles != null && !roles.isEmpty()) {
                            requestBuilder.header(X_ROLE_HEADER, roles);
                        }

                        ServerHttpRequest modifiedRequest = requestBuilder.build();
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    }).switchIfEmpty(chain.filter(exchange));
        };
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
     * Extracts roles from the JWT token's realm_access claim.
     *
     * <p>The roles are extracted from the nested structure:
     * realm_access.roles -> List<String>
     *
     * @param jwt The JWT token
     * @return Comma-separated list of roles, or null if not present
     */
    private String extractRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> realmAccessMap = (Map<String, Object>) realmAccess;
            Object rolesObj = realmAccessMap.get(ROLES_CLAIM);
            if (rolesObj instanceof List) {
                @SuppressWarnings("unchecked") List<String> roles = (List<String>) rolesObj;
                return String.join(",", roles);
            }
        }
        return null;
    }

    /**
     * Configuration class for TenantContextFilter. Currently empty but available for future configuration options.
     */
    public static class Config {
        // Configuration properties if needed
    }
}

