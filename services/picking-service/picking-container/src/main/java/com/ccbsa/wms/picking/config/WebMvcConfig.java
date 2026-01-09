package com.ccbsa.wms.picking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ccbsa.wms.common.security.TenantContextInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Web MVC Configuration: WebMvcConfig
 * <p>
 * Configures REST API ObjectMapper separation and request interceptors.
 * <p>
 * CRITICAL: This configuration ensures ObjectMapper separation for production-grade implementation:
 * - REST API ObjectMapper (@Primary): No type information, used for HTTP message conversion
 * - Kafka ObjectMapper (kafkaObjectMapper): With type information, used for Kafka messaging
 * - Redis Cache ObjectMapper (redisCacheObjectMapper): With type information, used for caching
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @NonNull
    private final TenantContextInterceptor tenantContextInterceptor;

    public WebMvcConfig(@NonNull TenantContextInterceptor tenantContextInterceptor) {
        this.tenantContextInterceptor = tenantContextInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Register TenantContextInterceptor to ensure tenant context is set from X-Tenant-Id header
        registry.addInterceptor(tenantContextInterceptor).addPathPatterns("/**").excludePathPatterns("/actuator/**", "/error", "/swagger-ui/**", "/v3/api-docs/**");
    }

    /**
     * Configures Jackson ObjectMapper for REST API responses.
     * <p>
     * PRODUCTION-GRADE DESIGN: This configuration is explicitly scoped for REST API use only.
     * It does NOT include type information, which is required for Kafka but should NOT be
     * present in REST API responses consumed by frontend clients.
     * <p>
     * Ensures LocalDateTime is serialized as ISO-8601 strings (not timestamps).
     * This is important for frontend compatibility and human readability.
     */
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder().modules(new JavaTimeModule()).featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .postConfigurer(objectMapper -> {
                    // Disable default typing which adds type information to JSON
                    // This ensures REST API responses are clean JSON without @class or array wrappers
                    objectMapper.setDefaultTyping(null);
                });
    }

    /**
     * Primary ObjectMapper bean for REST API HTTP message conversion.
     * <p>
     * PRODUCTION-GRADE DESIGN: This ObjectMapper is marked as @Primary ONLY for HTTP message conversion.
     * This is the default ObjectMapper used by Spring Boot's MappingJackson2HttpMessageConverter
     * for REST API serialization/deserialization.
     * <p>
     * SEPARATION OF CONCERNS:
     * - This ObjectMapper (@Primary): Used by Spring MVC for HTTP message conversion (REST API)
     * - "kafkaObjectMapper" bean: Used explicitly by Kafka components (with type information)
     * - "redisCacheObjectMapper" bean: Used explicitly by Redis cache (with type information)
     * <p>
     * The @Primary annotation here is NOT a workaround - it's a design choice to designate
     * the default ObjectMapper for HTTP. All other ObjectMappers are explicitly named and
     * injected using @Qualifier, ensuring clear separation of concerns.
     * <p>
     * Built from Jackson2ObjectMapperBuilder to ensure proper configuration without type information.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();
        // Explicitly ensure no type information (redundant but defensive)
        mapper.setDefaultTyping(null);
        return mapper;
    }
}
