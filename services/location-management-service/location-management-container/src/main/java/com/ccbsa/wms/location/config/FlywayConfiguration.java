package com.ccbsa.wms.location.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom Flyway Configuration
 * <p>
 * Provides automatic checksum repair on validation failure when enabled via FLYWAY_REPAIR_ON_MIGRATE environment variable (development only).
 * <p>
 * In production, use manual repair: mvn flyway:repair or flyway repair command.
 * <p>
 * This configuration uses FlywayMigrationStrategy to customize migration behavior and automatically repair checksums when validation fails.
 */
@Slf4j
@Configuration
public class FlywayConfiguration {

    @Value("${spring.flyway.repair-on-migrate:false}")
    private boolean repairOnMigrate;

    /**
     * Custom Flyway migration strategy that repairs checksums on validation failure.
     * <p>
     * When repair-on-migrate is enabled, this will automatically repair checksums if validation fails due to checksum mismatches.
     * <p>
     * This is the recommended way to customize Flyway behavior in Spring Boot.
     *
     * @param flyway Flyway instance
     * @return Migration strategy with repair capability
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(Flyway flyway) {
        return f -> {
            try {
                f.migrate();
            } catch (FlywayValidateException e) {
                if (repairOnMigrate && e.getMessage() != null && e.getMessage().contains("checksum mismatch")) {
                    log.warn("Flyway validation failed with checksum mismatch. Repairing checksums...");
                    log.warn("This should only be enabled in development environments.");
                    try {
                        f.repair();
                        log.info("Flyway checksums repaired successfully. Retrying migration...");
                        f.migrate();
                    } catch (Exception repairException) {
                        log.error("Failed to repair Flyway checksums", repairException);
                        throw new RuntimeException("Flyway checksum repair failed. Please run 'mvn flyway:repair' manually.", repairException);
                    }
                } else {
                    throw e;
                }
            }
        };
    }
}

