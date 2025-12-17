package com.ccbsa.wms.returns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Returns Service Application
 * <p>
 * Provides: - Return order processing - Returns workflow management - Service discovery via Eureka (auto-configured)
 * <p>
 * Note: Eureka client is auto-configured when spring-cloud-starter-netflix-eureka-client is on the classpath.
 */
@SpringBootApplication(scanBasePackages = {"com.ccbsa.wms.returns", "com.ccbsa.wms.common.security", "com.ccbsa.common.cache"})
public class ReturnsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReturnsServiceApplication.class, args);
    }
}

