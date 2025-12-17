package com.ccbsa.wms.stockmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Stock Management Service Application
 * <p>
 * Provides: - Stock consignment management - Stock level tracking - Service discovery via Eureka (auto-configured)
 * <p>
 * Note: Eureka client is auto-configured when spring-cloud-starter-netflix-eureka-client is on the classpath.
 */
@SpringBootApplication(scanBasePackages = {"com.ccbsa.wms.stockmanagement", "com.ccbsa.wms.common.security", "com.ccbsa.common.cache"})
@EnableJpaRepositories(basePackages = "com.ccbsa.wms.stockmanagement.dataaccess.jpa")
@EntityScan(basePackages = "com.ccbsa.wms.stockmanagement.dataaccess.entity")
public class StockManagementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockManagementServiceApplication.class, args);
    }
}

