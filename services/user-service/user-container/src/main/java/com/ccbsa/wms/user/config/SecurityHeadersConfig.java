package com.ccbsa.wms.user.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Security headers configuration. Adds security headers to all responses for production security.
 */
@Configuration
public class SecurityHeadersConfig {
    @Bean
    public OncePerRequestFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    /**
     * Static inner class for security headers filter.
     * <p>
     * This filter adds security headers to all HTTP responses.
     */
    private static class SecurityHeadersFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
                throws ServletException, IOException {

            // Security headers
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("X-XSS-Protection", "1; mode=block");
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            response.setHeader("X-Permitted-Cross-Domain-Policies", "none");

            filterChain.doFilter(request, response);
        }
    }
}

