package com.ccbsa.wms.picking.dataaccess.adapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.hibernate.Session;
import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.picking.application.service.port.repository.PickingListReferenceGenerator;
import com.ccbsa.wms.picking.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListReference;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter: PickingListReferenceGeneratorAdapter
 * <p>
 * Implements PickingListReferenceGenerator port interface.
 * Generates unique picking list references in format: PICK-{YYYYMMDD}-{sequence}
 * <p>
 * Uses a database sequence or counter table to ensure uniqueness per tenant per day.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PickingListReferenceGeneratorAdapter implements PickingListReferenceGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REFERENCE_PREFIX = "PICK-";

    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PickingListReference generate(TenantId tenantId) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when generating picking list reference!");
            throw new IllegalStateException("TenantContext must be set before generating picking list reference");
        }

        // Verify tenantId matches
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Request: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match request tenantId");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Generate reference: PICK-{YYYYMMDD}-{sequence}
        String datePrefix = LocalDate.now().format(DATE_FORMATTER);
        int sequence = getNextSequence(tenantId, datePrefix, schemaName);
        String reference = String.format("%s%s-%03d", REFERENCE_PREFIX, datePrefix, sequence);

        log.debug("Generated picking list reference: {} for tenant: {}", reference, tenantId.getValue());
        return PickingListReference.of(reference);
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }
        if ("public".equals(schemaName) || schemaName.matches("^tenant_[a-zA-Z0-9_]+_schema$")) {
            return;
        }
        throw new IllegalArgumentException(String.format("Invalid schema name format: '%s'", schemaName));
    }

    private void setSearchPath(Session session, String schemaName) {
        session.doWork(connection -> executeSetSearchPath(connection, schemaName));
    }

    /**
     * Gets the next sequence number for the given tenant and date.
     * Uses a counter table to track sequences per tenant per day.
     *
     * @param tenantId   Tenant identifier
     * @param datePrefix Date prefix (YYYYMMDD)
     * @param schemaName Schema name
     * @return Next sequence number (1-based)
     */
    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated")
    private int getNextSequence(TenantId tenantId, String datePrefix, String schemaName) {
        Session session = entityManager.unwrap(Session.class);
        return session.doReturningWork(connection -> {
            try {
                // Create counter table if it doesn't exist
                createCounterTableIfNotExists(connection, schemaName);

                // Get or create counter entry for this tenant and date
                String key = String.format("%s_%s", tenantId.getValue(), datePrefix);
                int currentSequence = getCurrentSequence(connection, key, schemaName);
                int nextSequence = currentSequence + 1;

                // Update or insert counter
                updateCounter(connection, key, nextSequence, schemaName);

                return nextSequence;
            } catch (SQLException e) {
                log.error("Failed to generate sequence for tenant: {}, date: {}", tenantId.getValue(), datePrefix, e);
                throw new RuntimeException("Failed to generate picking list reference sequence", e);
            }
        });
    }

    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated")
    private void executeSetSearchPath(Connection connection, String schemaName) {
        try (PreparedStatement stmt = connection.prepareStatement(String.format("SET search_path TO %s", escapeIdentifier(schemaName)))) {
            stmt.execute();
        } catch (SQLException e) {
            String errorMessage = String.format("Failed to set database schema: %s. Root cause: %s", schemaName, e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated")
    private void createCounterTableIfNotExists(Connection connection, String schemaName) throws SQLException {
        String escapedSchema = escapeIdentifier(schemaName);
        String sql = String.format("CREATE TABLE IF NOT EXISTS %s.picking_list_reference_counters (" + "key VARCHAR(255) PRIMARY KEY, " + "sequence INTEGER NOT NULL DEFAULT 0, "
                + "last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP" + ")", escapedSchema);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated")
    private int getCurrentSequence(Connection connection, String key, String schemaName) throws SQLException {
        String escapedSchema = escapeIdentifier(schemaName);
        String sql = String.format("SELECT sequence FROM %s.picking_list_reference_counters WHERE key = ?", escapedSchema);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("sequence");
                }
            }
        }
        return 0; // First sequence for this key
    }

    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated")
    private void updateCounter(Connection connection, String key, int sequence, String schemaName) throws SQLException {
        String escapedSchema = escapeIdentifier(schemaName);
        // Try UPDATE first
        String updateSql = String.format("UPDATE %s.picking_list_reference_counters SET sequence = ?, last_updated = CURRENT_TIMESTAMP WHERE key = ?", escapedSchema);
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setInt(1, sequence);
            stmt.setString(2, key);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                // No row exists, INSERT
                String insertSql = String.format("INSERT INTO %s.picking_list_reference_counters (key, sequence, last_updated) VALUES (?, ?, CURRENT_TIMESTAMP)", escapedSchema);
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, key);
                    insertStmt.setInt(2, sequence);
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    private String escapeIdentifier(String identifier) {
        return String.format("\"%s\"", identifier.replace("\"", "\"\""));
    }
}
