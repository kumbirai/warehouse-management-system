package com.ccbsa.wms.reconciliation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Reconciliation Service Application
 * <p>
 * Provides: - Stock reconciliation operations - Reconciliation workflow management - Service discovery via Eureka (auto-configured)
 * <p>
 * Note: Eureka client is auto-configured when spring-cloud-starter-netflix-eureka-client is on the classpath.
 */
@SpringBootApplication(scanBasePackages = {"com.ccbsa.wms.reconciliation", "com.ccbsa.wms.common.security", "com.ccbsa.common.cache"})
public class ReconciliationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReconciliationServiceApplication.class, args);
    }
}

