package com.ccbsa.wms.gateway.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Security configuration for the API Gateway.
 *
 * <p>Configures OAuth2 Resource Server with JWT token validation.
 * Allows public access to BFF authentication endpoints without requiring JWT tokens.
 *
 * <p>Public endpoints:
 * <ul>
 *   <li>/actuator/health - Health check endpoint</li>
 *   <li>/actuator/info - Service info endpoint</li>
 *   <li>/api/v1/bff/auth/login - User login endpoint</li>
 *   <li>/api/v1/bff/auth/refresh - Token refresh endpoint</li>
 *   <li>/api/v1/bff/auth/logout - User logout endpoint</li>
 * </ul>
 *
 * <p>All other endpoints require valid JWT tokens for authentication.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(GatewaySecurityConfig.class);
    /**
     * Public endpoints that do not require authentication.
     */
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of("/actuator/health",
            "/actuator/info",
            "/api/v1/bff/auth/login",
            "/api/v1/bff/auth/refresh",
            "/api/v1/bff/auth/logout");
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * Configures the security filter chain for the gateway.
     *
     * <p>Security configuration:
     * <ul>
     *   <li>CSRF protection is disabled (stateless API)</li>
     *   <li>Public endpoints are permitted without authentication</li>
     *   <li>All other endpoints require JWT authentication</li>
     *   <li>OAuth2 Resource Server validates JWT tokens using Keycloak JWK set</li>
     * </ul>
     *
     * <p>For public endpoints, when OAuth2 Resource Server can't find a token, it calls
     * the authenticationEntryPoint. For public endpoints, we return Mono.empty() which
     * allows the request to proceed with anonymous authentication, which is permitted by permitAll().
     *
     * @param http ServerHttpSecurity builder
     * @return Configured SecurityWebFilterChain
     */
    /**
     * Public endpoints security filter chain - processes first, allows anonymous access.
     * CORS is handled by Spring Cloud Gateway's global CORS configuration in application.yml.
     * This chain also handles OPTIONS (preflight) requests for CORS.
     */
    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityWebFilterChain publicSecurityWebFilterChain(ServerHttpSecurity http) {
        http.securityMatcher(exchange ->
                {
                    String path = exchange.getRequest()
                            .getPath()
                            .value();
                    String method = exchange.getRequest()
                            .getMethod()
                            .name();

                    logger.debug("Public chain checking path: {} method: {}",
                            path,
                            method);

                    // Allow all OPTIONS requests for CORS preflight
                    if ("OPTIONS".equals(method)) {
                        logger.info("Matched OPTIONS request for CORS preflight: {}",
                                path);
                        return org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult.match();
                    }

                    boolean isPublic = isPublicEndpoint(path);
                    logger.debug("Is public endpoint: {}",
                            isPublic);
                    if (isPublic) {
                        logger.info("Matched public endpoint: {}",
                                path);
                        return org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult.match();
                    }
                    return org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult.notMatch();
                })
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges.anyExchange()
                        .permitAll());

        return http.build();
    }

    /**
     * Checks if the given path is a public endpoint that doesn't require authentication.
     *
     * @param path The request path to check
     * @return true if the path is a public endpoint, false otherwise
     */
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.contains(path);
    }

    /**
     * Creates a CORS configuration source for Spring Security.
     *
     * <p>This configuration allows cross-origin requests from the frontend origins.
     * The configuration matches the global CORS configuration in application.yml
     * and ensures that Spring Security does not block CORS headers.
     *
     * <p>Allowed origins:
     * <ul>
     *   <li>http://localhost:3000, https://localhost:3000</li>
     *   <li>http://localhost:5173, https://localhost:5173</li>
     *   <li>http://192.168.50.30:3000, https://192.168.50.30:3000</li>
     * </ul>
     *
     * @return CorsConfigurationSource configured with allowed origins, methods, and headers
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOrigin("http://localhost:3000");
        corsConfig.addAllowedOrigin("https://localhost:3000");
        corsConfig.addAllowedOrigin("http://localhost:5173");
        corsConfig.addAllowedOrigin("https://localhost:5173");
        corsConfig.addAllowedOrigin("http://192.168.50.30:3000");
        corsConfig.addAllowedOrigin("https://192.168.50.30:3000");
        corsConfig.addAllowedMethod("GET");
        corsConfig.addAllowedMethod("POST");
        corsConfig.addAllowedMethod("PUT");
        corsConfig.addAllowedMethod("PATCH");
        corsConfig.addAllowedMethod("DELETE");
        corsConfig.addAllowedMethod("OPTIONS");
        corsConfig.addAllowedHeader("*");
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        logger.info("CORS configuration created with allowed origins: {}",
                corsConfig.getAllowedOrigins());
        return source;
    }

    /**
     * Protected endpoints security filter chain - processes second, requires JWT authentication.
     * CORS is handled by Spring Cloud Gateway's global CORS configuration in application.yml.
     *
     * <p>This chain only processes requests that don't match public endpoints.
     * It uses a securityMatcher that excludes public endpoints to ensure they
     * are handled by the public chain (Order 1).
     */
    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityWebFilterChain protectedSecurityWebFilterChain(ServerHttpSecurity http) {
        http.securityMatcher(exchange ->
                {
                    String path = exchange.getRequest()
                            .getPath()
                            .value();
                    // Only match if NOT a public endpoint
                    boolean isPublic = isPublicEndpoint(path);
                    if (!isPublic) {
                        logger.debug("Protected chain matched path: {}",
                                path);
                        return org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult.match();
                    }
                    return org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult.notMatch();
                })
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges.anyExchange()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder())
                        .jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(new JwtAuthenticationConverter()))));

        return http.build();
    }

    /**
     * Creates a JWT decoder bean for OAuth2 Resource Server.
     *
     * <p>The decoder validates JWT tokens using the Keycloak JWK set URI.
     * Tokens must be issued by the configured Keycloak realm.
     *
     * @return ReactiveJwtDecoder configured with Keycloak JWK set URI
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        logger.info("Configuring JWT decoder with JWK set URI: {}",
                jwkSetUri);
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri)
                .build();
    }
}

