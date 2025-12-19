package com.ccbsa.wms.location;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Location Management Service Application
 * <p>
 * Provides: - Warehouse location management - Location barcode generation and validation - Location capacity tracking - Service discovery via Eureka (auto-configured)
 * <p>
 * Note: Eureka client is auto-configured when spring-cloud-starter-netflix-eureka-client is on the classpath.
 */
@SpringBootApplication(scanBasePackages = {"com.ccbsa.wms.location", "com.ccbsa.wms.common.security", "com.ccbsa.common.cache"})
@EnableJpaRepositories(basePackages = "com.ccbsa.wms.location.dataaccess.jpa")
@EntityScan(basePackages = "com.ccbsa.wms.location.dataaccess.entity")
public class LocationManagementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocationManagementServiceApplication.class, args);
    }
}

