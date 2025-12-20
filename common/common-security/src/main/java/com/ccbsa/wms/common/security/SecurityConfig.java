package com.ccbsa.wms.common.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Security configuration that registers the tenant context interceptor. Services should import this configuration to enable tenant context extraction.
 * <p>
 * Note: TenantContextInterceptor is a @Component and will be auto-wired by Spring.
 */
@Configuration
public class SecurityConfig implements WebMvcConfigurer {
    @NonNull
    private final TenantContextInterceptor tenantContextInterceptor;

    public SecurityConfig(@NonNull TenantContextInterceptor tenantContextInterceptor) {
        this.tenantContextInterceptor = tenantContextInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(tenantContextInterceptor).addPathPatterns("/**").excludePathPatterns("/actuator/**", "/error", "/swagger-ui/**", "/v3/api-docs/**",
                // BFF authentication endpoints are public and don't have tenant context yet
                // Note: Gateway strips /api/v1 prefix, so these paths are what the service receives
                "/bff/auth/login", "/bff/auth/refresh", "/bff/auth/logout");
    }
}

