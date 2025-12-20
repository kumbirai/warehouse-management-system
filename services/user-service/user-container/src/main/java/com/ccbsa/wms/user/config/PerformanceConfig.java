package com.ccbsa.wms.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Performance configuration. Configures performance-related settings for the application.
 */
@Configuration
public class PerformanceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Configure static resource caching
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/").setCachePeriod(3600); // Cache for 1 hour
    }
}

