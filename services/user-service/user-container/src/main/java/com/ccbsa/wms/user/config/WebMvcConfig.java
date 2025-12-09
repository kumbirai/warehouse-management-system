package com.ccbsa.wms.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 * Registers interceptors and other web-related configurations.
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
}

