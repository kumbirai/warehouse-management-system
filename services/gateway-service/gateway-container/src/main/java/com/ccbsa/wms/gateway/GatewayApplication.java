package com.ccbsa.wms.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * API Gateway Service Application
 * <p>
 * Provides:
 * - Request routing to backend services
 * - JWT token validation via Keycloak
 * - Tenant context extraction and propagation
 * - Rate limiting per tenant/user
 * - CORS handling
 * - Request/response transformation
 * - Service discovery via Eureka (auto-configured)
 * <p>
 * Note: This service does not require database connectivity.
 * DataSource, JPA, and Flyway auto-configurations are excluded.
 * Eureka client is auto-configured when spring-cloud-starter-netflix-eureka-client is on the classpath.
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class})
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class,
                args);
    }
}

