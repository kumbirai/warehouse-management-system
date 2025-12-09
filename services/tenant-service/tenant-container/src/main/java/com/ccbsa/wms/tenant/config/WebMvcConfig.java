package com.ccbsa.wms.tenant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Web MVC configuration.
 * Registers interceptors and other web-related configurations.
 * Ensures proper date/time serialization for REST API responses.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @NonNull
    private final RequestLoggingInterceptor requestLoggingInterceptor;

    public WebMvcConfig(@NonNull RequestLoggingInterceptor requestLoggingInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**",
                        "/error",
                        "/swagger-ui/**",
                        "/v3/api-docs/**");
    }

    /**
     * Configures Jackson ObjectMapper for REST API responses.
     * Ensures LocalDateTime is serialized as ISO-8601 strings (not timestamps).
     * This is important for frontend compatibility and human readability.
     */
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .modules(new JavaTimeModule())
                .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}

