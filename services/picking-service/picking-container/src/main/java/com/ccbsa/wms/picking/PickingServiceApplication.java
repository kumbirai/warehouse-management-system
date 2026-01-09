package com.ccbsa.wms.picking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.ccbsa.wms.common.dataaccess.config.MultiTenantDataAccessConfig;

/**
 * Picking Service Application
 * <p>
 * Provides: - Order picking operations - Picking workflow management - Service discovery via Eureka (auto-configured)
 * <p>
 * Note: Eureka client is auto-configured when spring-cloud-starter-netflix-eureka-client is on the classpath.
 */
@SpringBootApplication(scanBasePackages = {"com.ccbsa.wms.picking", "com.ccbsa.wms.common.security", "com.ccbsa.common.cache"})
@Import(MultiTenantDataAccessConfig.class)
public class PickingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PickingServiceApplication.class, args);
    }
}

