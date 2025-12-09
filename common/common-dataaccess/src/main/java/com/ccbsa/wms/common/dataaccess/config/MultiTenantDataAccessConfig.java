package com.ccbsa.wms.common.dataaccess.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.dataaccess.naming.TenantAwarePhysicalNamingStrategy;

/**
 * Configuration for multi-tenant data access.
 * <p>
 * Provides auto-configuration for multi-tenant schema resolution.
 * Services can import this configuration to enable schema-per-tenant isolation.
 * <p>
 * The {@link TenantSchemaResolver} component is automatically registered
 * and can be used in JPA entity annotations with placeholder schema:
 * <pre>
 * {@code
 * @Entity
 * @Table(name = "users", schema = "tenant_schema")
 * public class UserEntity {
 *     // schema "tenant_schema" will be replaced with actual tenant schema at runtime
 * }
 * }
 * </pre>
 * <p>
 * The {@link TenantAwarePhysicalNamingStrategy} is registered as a bean and should be
 * configured in application.yml:
 * <pre>
 * {@code
 * spring:
 *   jpa:
 *     properties:
 *       hibernate:
 *         physical_naming_strategy: com.ccbsa.wms.common.dataaccess.naming.TenantAwarePhysicalNamingStrategy
 * }
 * </pre>
 * <p>
 * Usage in services:
 * <pre>
 * {@code
 * @Configuration
 * @Import(MultiTenantDataAccessConfig.class)
 * public class ServiceConfiguration {
 *     // TenantSchemaResolver and TenantAwarePhysicalNamingStrategy are now available
 * }
 * }
 * </pre>
 */
@Configuration
@ComponentScan(basePackageClasses = {TenantSchemaResolver.class, TenantAwarePhysicalNamingStrategy.class})
public class MultiTenantDataAccessConfig {

    /**
     * Registers TenantAwarePhysicalNamingStrategy as a Spring bean.
     * <p>
     * This allows the naming strategy to access Spring beans (like TenantSchemaResolver)
     * through ApplicationContextAware.
     *
     * @return TenantAwarePhysicalNamingStrategy instance
     */
    @Bean
    public TenantAwarePhysicalNamingStrategy tenantAwarePhysicalNamingStrategy() {
        return new TenantAwarePhysicalNamingStrategy();
    }
}

