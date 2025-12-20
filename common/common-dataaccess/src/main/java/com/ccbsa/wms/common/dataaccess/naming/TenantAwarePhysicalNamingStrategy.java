package com.ccbsa.wms.common.dataaccess.naming;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;

/**
 * Hibernate PhysicalNamingStrategy that dynamically resolves tenant schema at runtime.
 * <p>
 * This strategy intercepts schema resolution during Hibernate operations and replaces the schema name with the tenant-specific schema resolved from TenantSchemaResolver.
 * <p>
 * This solves the issue where SpEL expressions in @Table annotations are not evaluated by Hibernate. Instead, we use a placeholder schema name in annotations and replace it
 * dynamically at runtime.
 * <p>
 * Usage in JPA entities:
 * <pre>
 * {@code
 * @Entity
 * @Table(name = "notifications", schema = "tenant_schema")
 * public class NotificationEntity {
 *     // schema "tenant_schema" will be replaced with actual tenant schema at runtime
 * }
 * }
 * </pre>
 */
@Component
public class TenantAwarePhysicalNamingStrategy implements PhysicalNamingStrategy, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(TenantAwarePhysicalNamingStrategy.class);
    private static final String PLACEHOLDER_SCHEMA = "tenant_schema";

    private ApplicationContext applicationContext;

    /**
     * Default constructor required by Hibernate.
     */
    public TenantAwarePhysicalNamingStrategy() {
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Identifier toPhysicalCatalogName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return identifier;
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        if (identifier == null) {
            return null;
        }

        String schemaName = identifier.getText();

        // Replace placeholder schema with actual tenant schema
        if (PLACEHOLDER_SCHEMA.equals(schemaName)) {
            try {
                TenantSchemaResolver resolver = getTenantSchemaResolver();
                if (resolver != null) {
                    String resolvedSchema = resolver.resolveSchema();
                    // Log schema resolution for debugging
                    logger.info("Resolved schema '{}' to '{}'", PLACEHOLDER_SCHEMA, resolvedSchema);
                    return Identifier.toIdentifier(resolvedSchema, identifier.isQuoted());
                } else {
                    logger.warn("TenantSchemaResolver not available - using default schema");
                }
            } catch (IllegalStateException e) {
                // Tenant context not set - this is expected during Hibernate initialization at startup
                // Return null to use default schema (public) where Flyway creates tables
                // The actual tenant schema will be resolved at runtime when tenant context is available
                logger.debug("Tenant context not set when resolving schema '{}' during initialization: {}. Using default schema (public). This is expected during startup.",
                        PLACEHOLDER_SCHEMA, e.getMessage());
                return null;
            }
        }

        return identifier;
    }

    /**
     * Gets the TenantSchemaResolver from Spring context.
     *
     * @return TenantSchemaResolver instance, or null if not available
     */
    private TenantSchemaResolver getTenantSchemaResolver() {
        if (applicationContext != null) {
            try {
                return applicationContext.getBean(TenantSchemaResolver.class);
            } catch (BeansException e) {
                // Bean not available - return null
                return null;
            }
        }
        return null;
    }

    @Override
    public Identifier toPhysicalTableName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return identifier;
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return identifier;
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return identifier;
    }
}

