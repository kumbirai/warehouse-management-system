package com.ccbsa.wms.user.config;

import java.time.Instant;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.ccbsa.wms.common.security.ServiceSecurityConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * User Service Security Configuration
 *
 * <p>Configures security for user-service, allowing public access to BFF auth endpoints.
 * Overrides base ServiceSecurityConfig to allow public access to login/refresh endpoints.
 *
 * <p>This configuration ensures that OAuth2 Resource Server allows requests without tokens
 * for public endpoints, preventing 403 Forbidden errors on login/refresh endpoints.
 *
 * <p>Note: CORS is handled at the gateway level, not in this service.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class UserServiceSecurityConfig extends ServiceSecurityConfig {
    /**
     * Public endpoints that do not require authentication. Note: Gateway strips /api/v1 prefix, so these paths are what the service receives.
     */
    private static final Set<String> PUBLIC_ENDPOINTS =
            Set.of("/bff/auth/login", "/bff/auth/refresh", "/bff/auth/logout", "/actuator/health", "/actuator/info", "/error", "/swagger-ui", "/v3/api-docs");
    private final SecurityHeadersConfig securityHeadersConfig;

    /**
     * Public endpoints security filter chain - no OAuth2 Resource Server. This chain handles public endpoints (login, refresh, actuator, etc.) without OAuth2 Resource Server,
     * allowing unauthenticated access.
     *
     * <p>This chain is ordered first (Order 1) and uses securityMatcher to only
     * process specific public endpoints. All other requests will fall through to the protected endpoints chain.
     */

    @Bean
    @Order(1)
    public SecurityFilterChain publicEndpointsSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring public endpoints security filter chain (Order 1) - no OAuth2 Resource Server");
        log.info("Public endpoints: /bff/auth/login, /bff/auth/refresh, /bff/auth/logout, /actuator/**, /error, /swagger-ui/**, /v3/api-docs/**");
        http.securityMatcher(createPublicEndpointsMatcher()).csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(securityHeadersConfig.securityHeadersFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        SecurityFilterChain chain = http.build();
        log.info("Public endpoints security filter chain configured successfully");
        return chain;
    }

    /**
     * Creates a request matcher for public endpoints. This ensures that the public endpoints filter chain only processes these specific paths.
     */
    private RequestMatcher createPublicEndpointsMatcher() {
        return new RequestMatcher() {
            @Override
            public boolean matches(jakarta.servlet.http.HttpServletRequest request) {
                String path = request.getRequestURI();
                String method = request.getMethod();
                boolean matches = isPublicEndpoint(path);
                log.info("Public endpoints matcher checking: {} {} -> matches: {}", method, path, matches);
                if (matches) {
                    log.info("✅ Public endpoints matcher MATCHED path: {} {}", method, path);
                } else {
                    log.debug("❌ Public endpoints matcher did NOT match path: {} {}", method, path);
                }
                return matches;
            }
        };
    }

    /**
     * Protected endpoints security filter chain - with OAuth2 Resource Server.
     * This chain handles all other endpoints that require JWT authentication.
     *
     * <p>This chain is ordered second (Order 2) and does NOT use securityMatcher,
     * making it the default chain for all requests that don't match the public
     * endpoints chain. It requires OAuth2 Resource Server JWT authentication.
     */

    /**
     * Checks if the given path is a public endpoint that doesn't require authentication.
     * <p>
     * Handles paths with or without /api/v1 prefix since gateway may or may not strip it.
     *
     * @param path The request path to check
     * @return true if the path is a public endpoint, false otherwise
     */
    private boolean isPublicEndpoint(String path) {
        log.debug("Checking if path is public endpoint: {}", path);

        // Normalize path by removing /api/v1 prefix if present
        final String normalizedPath = path.startsWith("/api/v1") ? path.substring(7) // Remove "/api/v1" prefix
                : path;

        // Check exact matches first (with normalized path)
        if (PUBLIC_ENDPOINTS.contains(normalizedPath)) {
            log.debug("Path {} matched exact public endpoint (normalized: {})", path, normalizedPath);
            return true;
        }

        // Check if normalized path starts with any public endpoint prefix
        final String finalNormalizedPath = normalizedPath;
        boolean matches = PUBLIC_ENDPOINTS.stream().anyMatch(endpoint -> finalNormalizedPath.startsWith(endpoint));
        if (matches) {
            log.debug("Path {} matched public endpoint prefix (normalized: {})", path, normalizedPath);
        } else {
            log.debug("Path {} did NOT match any public endpoint (normalized: {}). Public endpoints: {}", path, normalizedPath, PUBLIC_ENDPOINTS);
        }
        return matches;
    }

    @Bean
    @Order(2)
    @Override
    public SecurityFilterChain securityFilterChain(HttpSecurity http, com.ccbsa.wms.common.security.GatewayRoleHeaderAuthenticationFilter gatewayRoleHeaderAuthenticationFilter)
            throws Exception {
        log.info("Configuring protected endpoints security filter chain (Order 2) - with OAuth2 Resource Server");
        http.securityMatcher(createProtectedEndpointsMatcher()).csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(securityHeadersConfig.securityHeadersFilter(), UsernamePasswordAuthenticationFilter.class).authorizeHttpRequests(auth -> auth
                        // All endpoints in this chain require authentication
                        .anyRequest().authenticated()).oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder()).jwtAuthenticationConverter(jwtAuthenticationConverter())))
                // Add filter to extract roles from X-Role header (set by gateway) after JWT BearerTokenAuthenticationFilter
                // Note: BearerTokenAuthenticationFilter is added by oauth2ResourceServer(), so we add our filter after it
                .addFilterAfter(gatewayRoleHeaderAuthenticationFilter, BearerTokenAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(createAuthenticationEntryPoint()).accessDeniedHandler(createAccessDeniedHandler()));

        SecurityFilterChain chain = http.build();
        log.info("Protected endpoints security filter chain configured successfully");
        return chain;
    }

    /**
     * Creates a request matcher for protected endpoints (excludes public endpoints). This ensures that the protected endpoints filter chain only processes non-public paths.
     */
    private RequestMatcher createProtectedEndpointsMatcher() {
        return new RequestMatcher() {
            @Override
            public boolean matches(jakarta.servlet.http.HttpServletRequest request) {
                String path = request.getRequestURI();
                String method = request.getMethod();
                boolean isPublic = isPublicEndpoint(path);
                boolean matches = !isPublic; // Match if NOT public
                log.info("Protected endpoints matcher checking: {} {} -> isPublic: {}, matches: {}", method, path, isPublic, matches);
                if (matches) {
                    log.info("✅ Protected endpoints matcher MATCHED path: {} {}", method, path);
                } else {
                    log.debug("❌ Protected endpoints matcher did NOT match (public endpoint): {} {}", method, path);
                }
                return matches;
            }
        };
    }

    /**
     * Creates a custom authentication entry point that allows public endpoints.
     *
     * <p>For public endpoints, allows the request to proceed without authentication by
     * not writing a response, which allows the filter chain to continue with anonymous authentication. For protected endpoints, returns 401 Unauthorized.
     *
     * <p>This is critical for public endpoints because OAuth2 Resource Server filter
     * will call this entry point when no token is present. For public endpoints, we must not write a response, allowing Spring Security to continue processing with anonymous
     * authentication, which is then permitted by permitAll().
     */
    private AuthenticationEntryPoint createAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            String path = request.getRequestURI();
            String method = request.getMethod();

            if (isPublicEndpoint(path)) {
                log.debug("Allowing unauthenticated access to public endpoint: {} {} - OAuth2 Resource Server will skip authentication", method, path);
                // For public endpoints, do NOT write a response
                // This allows Spring Security's filter chain to continue processing
                // The OAuth2 Resource Server filter will skip authentication when no token is present
                // and the request will proceed with anonymous authentication, which is permitted by permitAll()
                return;
            }

            log.warn("Authentication required for protected endpoint: {} {} - {}", method, path, authException.getMessage());
            // For protected endpoints, return 401 Unauthorized
            if (!response.isCommitted()) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter()
                        .write(String.format("{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\",\"timestamp\":\"%s\"}}", Instant.now().toString()));
            }
        };
    }

    /**
     * Creates a custom access denied handler that allows public endpoints.
     *
     * <p>For public endpoints, allows the request to proceed.
     * For protected endpoints, returns 403 Forbidden.
     */
    private AccessDeniedHandler createAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            String path = request.getRequestURI();
            String method = request.getMethod();

            if (isPublicEndpoint(path)) {
                log.debug("Allowing access to public endpoint despite access denied: {} {}", method, path);
                // For public endpoints, allow request to proceed
                // Do nothing - let the request continue
                return;
            }

            log.warn("Access denied for endpoint: {} {} - {}", method, path, accessDeniedException.getMessage());
            // For protected endpoints, return 403 Forbidden
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format("{\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"Access denied\",\"timestamp\":\"%s\"}}", Instant.now().toString()));
        };
    }
}

