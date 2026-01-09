package com.ccbsa.wms.picking.dataaccess.schema;

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
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "JdbcTemplate and DataSource are Spring-managed beans that are thread-safe and effectively immutable after "
        + "initialization.")
public class TenantSchemaProvisioner {
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    /**
     * Ensures the tenant schema exists and has Flyway migrations applied.
     *
     * @param schemaName schema to verify/create
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void ensureSchemaReady(String schemaName) {
        if (schemaExists(schemaName)) {
            return;
        }

        log.info("Tenant schema '{}' missing; creating and migrating on-demand", schemaName);
        createSchema(schemaName);
        runMigrations(schemaName);
    }

    private boolean schemaExists(String schemaName) {
        Boolean exists = jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)", Boolean.class, schemaName);
        return Boolean.TRUE.equals(exists);
    }

    private void createSchema(String schemaName) {
        jdbcTemplate.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName));
        log.info("Tenant schema created: {}", schemaName);
    }

    private void runMigrations(String schemaName) {
        Flyway flyway = Flyway.configure().dataSource(dataSource).schemas(schemaName).locations("classpath:db/migration").baselineOnMigrate(true).load();

        int applied = flyway.migrate().migrationsExecuted;
        log.info("Flyway migrations applied to schema {}: {}", schemaName, applied);
    }
}
