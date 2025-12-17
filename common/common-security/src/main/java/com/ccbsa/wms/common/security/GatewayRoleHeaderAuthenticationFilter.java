package com.ccbsa.wms.common.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that extracts roles from X-Role header (set by gateway) and sets them as Spring Security authorities.
 * <p>
 * This filter follows the architectural pattern where:
 * <ul>
 *   <li>Gateway extracts roles from JWT and sets X-Role header</li>
 *   <li>Microservices trust the gateway and use X-Role header</li>
 *   <li>Roles from header are converted to Spring Security authorities for @PreAuthorize to work</li>
 * </ul>
 * <p>
 * The filter runs after JWT authentication but before authorization, wrapping the JWT authentication
 * with authorities extracted from the X-Role header.
 * <p>
 * If X-Role header is not present, the filter falls back to extracting roles from JWT token
 * (for backward compatibility or direct service-to-service calls).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // Run after JWT authentication filter
public class GatewayRoleHeaderAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(GatewayRoleHeaderAuthenticationFilter.class);
    private static final String X_ROLE_HEADER = "X-Role";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Only process if we have a JWT authentication
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();

            // Extract roles from X-Role header (set by gateway) - preferred method
            Collection<GrantedAuthority> authorities = extractAuthoritiesFromHeader(request);

            // Fallback to JWT if header not present (for backward compatibility)
            if (authorities.isEmpty()) {
                logger.debug("X-Role header not present for path {}, falling back to JWT token roles", request.getRequestURI());
                authorities = extractAuthoritiesFromJwt(jwt);
                if (!authorities.isEmpty()) {
                    logger.debug("Extracted {} authorities from JWT token (fallback): {}", 
                        authorities.size(), 
                        authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
                }
            } else {
                logger.info("Extracted {} authorities from X-Role header for path {}: {}", 
                    authorities.size(),
                    request.getRequestURI(),
                    authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
            }

            // Create new authentication with authorities from header
            if (!authorities.isEmpty()) {
                JwtAuthenticationToken newAuth = new JwtAuthenticationToken(jwt, authorities);
                SecurityContextHolder.getContext().setAuthentication(newAuth);
                logger.info("Updated authentication with {} authorities for path {}", authorities.size(), request.getRequestURI());
            } else {
                logger.warn("No authorities found for path {} - neither X-Role header nor JWT roles present", request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts authorities from X-Role header (set by gateway).
     * <p>
     * The header contains comma-separated roles, e.g., "TENANT_ADMIN,USER,PICKER"
     * These are converted to Spring Security authorities with ROLE_ prefix.
     *
     * @param request HTTP request
     * @return Collection of granted authorities
     */
    private Collection<GrantedAuthority> extractAuthoritiesFromHeader(HttpServletRequest request) {
        String rolesHeader = request.getHeader(X_ROLE_HEADER);
        if (rolesHeader == null || rolesHeader.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> new SimpleGrantedAuthority(String.format("ROLE_%s", role)))
                .collect(Collectors.toList());
    }

    /**
     * Extracts authorities from JWT token (fallback method).
     * <p>
     * This is used when X-Role header is not present, for backward compatibility
     * or direct service-to-service calls that bypass the gateway.
     *
     * @param jwt JWT token
     * @return Collection of granted authorities
     */
    private Collection<GrantedAuthority> extractAuthoritiesFromJwt(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> realmAccessMap = (Map<String, Object>) realmAccess;
            Object rolesObj = realmAccessMap.get("roles");
            if (rolesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) rolesObj;
                return roles.stream()
                        .map(role -> new SimpleGrantedAuthority(String.format("ROLE_%s", role)))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}

