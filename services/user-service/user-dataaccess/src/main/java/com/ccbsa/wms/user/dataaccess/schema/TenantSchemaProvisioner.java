package com.ccbsa.wms.user.dataaccess.schema;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Ensures tenant schemas exist and are migrated before use.
 * <p>
 * Acts as an on-demand safety net so write paths do not fail when
 * asynchronous tenant-provisioning events are delayed or missing.
 */
@Component
public class TenantSchemaProvisioner {
    private static final Logger logger = LoggerFactory.getLogger(TenantSchemaProvisioner.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "JdbcTemplate and DataSource are Spring-managed beans that are thread-safe and effectively immutable after "
            + "initialization. They are safe to store directly as infrastructure dependencies.")
    public TenantSchemaProvisioner(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    /**
     * Ensures the tenant schema exists and has Flyway migrations applied.
     *
     * @param schemaName schema to verify/create
     */
    public void ensureSchemaReady(String schemaName) {
        if (schemaExists(schemaName)) {
            return;
        }

        logger.info("Tenant schema '{}' missing; creating and migrating on-demand", schemaName);
        createSchema(schemaName);
        runMigrations(schemaName);
    }

    private boolean schemaExists(String schemaName) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)",
                Boolean.class,
                schemaName
        );
        return Boolean.TRUE.equals(exists);
    }

    private void createSchema(String schemaName) {
        jdbcTemplate.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName));
        logger.info("Tenant schema created: {}", schemaName);
    }

    private void runMigrations(String schemaName) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        int applied = flyway.migrate().migrationsExecuted;
        logger.info("Flyway migrations applied to schema {}: {}", schemaName, applied);
    }
}

