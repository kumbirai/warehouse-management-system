package com.ccbsa.wms.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.ccbsa.common.keycloak.config.KeycloakConfig;

/**
 * User Service Application
 * <p>
 * Provides:
 * - User management operations
 * - Integration with Keycloak IAM
 * - User profile management
 * - Tenant-user relationship management
 * - BFF (Backend for Frontend) authentication endpoints
 * - Service discovery via Eureka (auto-configured)
 * <p>
 * Note: Eureka client is auto-configured when spring-cloud-starter-netflix-eureka-client is on the classpath.
 */
@SpringBootApplication(scanBasePackages = {"com.ccbsa.wms.user",
        "com.ccbsa.common.keycloak",
        "com.ccbsa.wms.common.security"})
@EnableConfigurationProperties(KeycloakConfig.class)
@EnableJpaRepositories(basePackages = "com.ccbsa.wms.user.dataaccess.jpa")
@EntityScan(basePackages = "com.ccbsa.wms.user.dataaccess.entity")
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class,
                args);
    }
}

