package com.ccbsa.wms.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Integration Service Application
 * <p>
 * Provides: - External system integration (e.g., D365, CSV imports) - Integration event processing - Service discovery via Eureka (auto-configured)
 * <p>
 * Note: Eureka client is auto-configured when spring-cloud-starter-netflix-eureka-client is on the classpath.
 */
@SpringBootApplication(scanBasePackages = {"com.ccbsa.wms.integration", "com.ccbsa.wms.common.security", "com.ccbsa.common.cache"})
public class IntegrationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationServiceApplication.class, args);
    }
}

