package com.ccbsa.wms.common.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Base security configuration for backend services.
 * Imports SecurityConfig for tenant context interceptor.
 * Configures OAuth2 Resource Server with JWT token validation.
 * <p>
 * Services should extend this class or import it to enable security.
 * Services that need custom security configuration can override the securityFilterChain bean.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(SecurityConfig.class)
public class ServiceSecurityConfig {
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:7080/realms/wms-realm/protocol/openid-connect/certs}")
    private String jwkSetUri;

    /**
     * Base security filter chain configuration.
     * Only created if no other SecurityFilterChain bean exists, allowing services to override.
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/**",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated());

        return http.build();
    }

    /**
     * JWT decoder bean for OAuth2 Resource Server.
     * Only created if no other JwtDecoder bean exists, allowing services to override.
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .build();
    }

    /**
     * JWT authentication converter that extracts roles from Keycloak JWT tokens.
     * <p>
     * Extracts roles from the {@code realm_access.roles} claim in the JWT token
     * and converts them to Spring Security authorities with the {@code ROLE_} prefix.
     * This enables {@code @PreAuthorize("hasRole('ADMIN')")} annotations to work correctly.
     * <p>
     * Example: If the JWT contains {@code realm_access.roles: ["ADMIN", "USER"]},
     * this converter will create authorities {@code ROLE_ADMIN} and {@code ROLE_USER}.
     *
     * @return JwtAuthenticationConverter configured to extract roles from Keycloak JWT tokens
     */
    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthoritiesFromJwt);
        return converter;
    }

    /**
     * Extracts authorities (roles) from a JWT token.
     * <p>
     * Extracts roles from the {@code realm_access.roles} claim and converts them
     * to {@code GrantedAuthority} objects with the {@code ROLE_} prefix.
     *
     * @param jwt The JWT token
     * @return Collection of granted authorities extracted from the JWT
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
        return List.of();
    }
}

