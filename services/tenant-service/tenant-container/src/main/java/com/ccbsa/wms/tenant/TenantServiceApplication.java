package com.ccbsa.wms.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.ccbsa.common.keycloak.config.KeycloakConfig;

/**
 * Tenant Service Application
 * <p>
 * Provides: - Tenant lifecycle management (create, activate, deactivate, suspend) - Tenant configuration management - Tenant metadata management - Keycloak realm management
 * integration - Service discovery via Eureka (auto-configured)
 * <p>
 * Note: Eureka client is auto-configured when spring-cloud-starter-netflix-eureka-client is on the classpath.
 */
@SpringBootApplication(scanBasePackages = {"com.ccbsa.wms.tenant", "com.ccbsa.common.keycloak", "com.ccbsa.wms.common.security"})
@EnableConfigurationProperties(KeycloakConfig.class)
@EnableJpaRepositories(basePackages = "com.ccbsa.wms.tenant.dataaccess.jpa")
@EntityScan(basePackages = "com.ccbsa.wms.tenant.dataaccess.entity")
public class TenantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenantServiceApplication.class, args);
    }
}

