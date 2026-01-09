package com.ccbsa.wms.common.dataaccess.schema;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ensures tenant schemas exist and are migrated before use.
 * <p>
 * Acts as an on-demand safety net so write paths do not fail when asynchronous tenant-provisioning events are delayed or missing.
 * <p>
 * This component is shared across all services that use schema-per-tenant strategy.
 * Each service should configure Flyway migrations in their own {@code db/migration} directory.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "JdbcTemplate and DataSource are Spring-managed beans that are thread-safe and effectively immutable after "
        + "initialization. They are safe to store directly as infrastructure dependencies.")
public class TenantSchemaProvisioner {
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    /**
     * Ensures the tenant schema exists and has Flyway migrations applied.
     * <p>
     * This method always runs migrations, even if the schema already exists, to ensure
     * all migrations are applied. Flyway tracks applied migrations and will only run
     * new migrations that haven't been applied yet.
     * <p>
     * Uses NOT_SUPPORTED propagation to ensure schema operations run outside of
     * any existing transaction context, preventing connection leak warnings.
     *
     * @param schemaName schema to verify/create
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void ensureSchemaReady(String schemaName) {
        if (!schemaExists(schemaName)) {
            log.info("Tenant schema '{}' missing; creating on-demand", schemaName);
            createSchema(schemaName);
        } else {
            log.debug("Tenant schema '{}' already exists", schemaName);
        }

        // Always run migrations to ensure all migrations are applied
        // Flyway will only run migrations that haven't been applied yet
        log.info("Ensuring migrations are applied to schema '{}'", schemaName);
        runMigrations(schemaName);
    }

    private boolean schemaExists(String schemaName) {
        Boolean exists = jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)", Boolean.class, schemaName);
        return Boolean.TRUE.equals(exists);
    }

    private void createSchema(String schemaName) {
        try {
            // Double-check schema doesn't exist to avoid race conditions
            Boolean exists = jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)", Boolean.class, schemaName);
            if (Boolean.TRUE.equals(exists)) {
                log.debug("Tenant schema already exists: {}", schemaName);
                return;
            }

            jdbcTemplate.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName));
            log.info("Tenant schema created: {}", schemaName);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // Schema was created concurrently - treat as success
            // DuplicateKeyException wraps PSQLException for PostgreSQL duplicate key errors
            log.debug("Tenant schema already exists (created concurrently): {}", schemaName);
        }
    }

    /**
     * Runs Flyway migrations for the specified schema.
     * <p>
     * Called from ensureSchemaReady which already has NOT_SUPPORTED propagation,
     * allowing Flyway to manage its own connection and transaction lifecycle.
     * This prevents connection leaks when Flyway is called from within an existing transaction context.
     *
     * @param schemaName schema to migrate
     */
    private void runMigrations(String schemaName) {
        Flyway flyway = Flyway.configure().dataSource(dataSource).schemas(schemaName).locations("classpath:db/migration").baselineOnMigrate(true).load();

        int applied = flyway.migrate().migrationsExecuted;
        log.info("Flyway migrations applied to schema {}: {}", schemaName, applied);
    }
}
