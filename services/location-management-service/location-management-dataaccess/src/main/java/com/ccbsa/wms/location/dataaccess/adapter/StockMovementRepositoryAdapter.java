package com.ccbsa.wms.location.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.location.application.service.port.repository.StockMovementRepository;
import com.ccbsa.wms.location.dataaccess.entity.StockMovementEntity;
import com.ccbsa.wms.location.dataaccess.jpa.StockMovementJpaRepository;
import com.ccbsa.wms.location.dataaccess.mapper.StockMovementEntityMapper;
import com.ccbsa.wms.location.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.location.domain.core.entity.StockMovement;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: StockMovementRepositoryAdapter
 * <p>
 * Implements StockMovementRepository port interface. Adapts between domain StockMovement aggregate and JPA StockMovementEntity.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class StockMovementRepositoryAdapter implements StockMovementRepository {
    private final StockMovementJpaRepository jpaRepository;
    private final StockMovementEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public StockMovement save(StockMovement stockMovement) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.error("TenantContext is not set when saving stock movement! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before saving stock movement");
        }

        // Verify tenantId matches
        if (!tenantId.getValue().equals(stockMovement.getTenantId().getValue())) {
            log.error("TenantContext mismatch! Context: {}, StockMovement: {}", tenantId.getValue(), stockMovement.getTenantId().getValue());
            throw new IllegalStateException("TenantContext tenantId does not match stock movement tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Check if entity already exists to handle version correctly
        Optional<StockMovementEntity> existingEntity = jpaRepository.findByTenantIdAndId(stockMovement.getTenantId().getValue(), stockMovement.getId().getValue());

        StockMovementEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            updateEntityFromDomain(entity, stockMovement);
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity(stockMovement);
        }

        StockMovementEntity savedEntity = jpaRepository.save(entity);
        log.debug("StockMovement saved successfully to schema: '{}'", schemaName);

        // Domain events are preserved by the command handler before calling save()
        // The command handler gets domain events from the original stockMovement before save()
        // and publishes them after transaction commit. We return the saved stockMovement
        // which may not have events, but that's OK since events are already captured.
        return mapper.toDomain(savedEntity);
    }

    /**
     * Validates that a schema name matches expected patterns to prevent SQL injection.
     * <p>
     * Schema names must match one of these patterns:
     * - 'public' (for legacy data)
     * - 'tenant_*_schema' (for tenant-specific schemas)
     *
     * @param schemaName The schema name to validate
     * @throws IllegalArgumentException if schema name does not match expected patterns
     */
    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }

        // Allow 'public' schema
        if ("public".equals(schemaName)) {
            return;
        }

        // Allow tenant schemas matching pattern: tenant_*_schema
        if (schemaName.matches("^tenant_[a-zA-Z0-9_]+_schema$")) {
            return;
        }

        throw new IllegalArgumentException(String.format("Invalid schema name format: '%s'. Expected 'public' or 'tenant_*_schema' pattern", schemaName));
    }

    /**
     * Sets the PostgreSQL search_path for the current database connection.
     * <p>
     * This method sets the search_path to the specified schema name.
     * The schema name must be validated before calling this method.
     *
     * @param session    Hibernate session
     * @param schemaName Validated schema name
     */
    private void setSearchPath(Session session, String schemaName) {
        session.doWork(connection -> executeSetSearchPath(connection, schemaName));
    }

    /**
     * Updates an existing entity with values from the domain model.
     * Preserves JPA managed state and version for optimistic locking.
     *
     * @param entity        Existing JPA entity
     * @param stockMovement Domain aggregate
     */
    private void updateEntityFromDomain(StockMovementEntity entity, StockMovement stockMovement) {
        // Update all mutable fields from domain model
        entity.setStockItemId(stockMovement.getStockItemId().getValueAsString());
        entity.setProductId(stockMovement.getProductId().getValue());
        entity.setSourceLocationId(stockMovement.getSourceLocationId().getValue());
        entity.setDestinationLocationId(stockMovement.getDestinationLocationId().getValue());
        entity.setQuantity(stockMovement.getQuantity().getValue());
        entity.setMovementType(stockMovement.getMovementType());
        entity.setReason(stockMovement.getReason());
        entity.setStatus(stockMovement.getStatus());
        entity.setInitiatedBy(java.util.UUID.fromString(stockMovement.getInitiatedBy().getValue()));
        entity.setInitiatedAt(stockMovement.getInitiatedAt());

        // Update completion fields if present
        if (stockMovement.getCompletedBy() != null) {
            entity.setCompletedBy(java.util.UUID.fromString(stockMovement.getCompletedBy().getValue()));
        }
        if (stockMovement.getCompletedAt() != null) {
            entity.setCompletedAt(stockMovement.getCompletedAt());
            entity.setLastModifiedAt(stockMovement.getCompletedAt());
        } else {
            entity.setCompletedBy(null);
            entity.setCompletedAt(null);
        }

        // Update cancellation fields if present
        if (stockMovement.getCancelledBy() != null) {
            entity.setCancelledBy(java.util.UUID.fromString(stockMovement.getCancelledBy().getValue()));
        }
        if (stockMovement.getCancelledAt() != null) {
            entity.setCancelledAt(stockMovement.getCancelledAt());
            entity.setLastModifiedAt(stockMovement.getCancelledAt());
        } else {
            entity.setCancelledBy(null);
            entity.setCancelledAt(null);
        }
        if (stockMovement.getCancellationReason() != null) {
            entity.setCancellationReason(stockMovement.getCancellationReason().getValue());
        } else {
            entity.setCancellationReason(null);
        }

        // Update lastModifiedAt if not set by completion/cancellation
        if (stockMovement.getCompletedAt() == null && stockMovement.getCancelledAt() == null) {
            entity.setLastModifiedAt(stockMovement.getInitiatedAt());
        }

        // Version is managed by JPA - don't update it manually
    }

    /**
     * Executes the SET search_path SQL command.
     * <p>
     * This method is separated to allow proper SpotBugs annotation placement.
     *
     * @param connection Database connection
     * @param schemaName Validated and escaped schema name
     */
    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated against expected patterns (tenant_*_schema or public) "
            + "and properly escaped using escapeIdentifier() method. " + "PostgreSQL SET search_path command does not support parameterized queries.")
    private void executeSetSearchPath(Connection connection, String schemaName) {
        try (Statement stmt = connection.createStatement()) {
            String setSchemaSql = String.format("SET search_path TO %s", escapeIdentifier(schemaName));
            log.debug("Setting search_path to: {}", schemaName);
            stmt.execute(setSchemaSql);
        } catch (SQLException e) {
            log.error("Failed to set search_path to schema '{}': {}", schemaName, e.getMessage(), e);
            throw new RuntimeException("Failed to set database schema", e);
        }
    }

    /**
     * Escapes a PostgreSQL identifier to prevent SQL injection.
     * <p>
     * PostgreSQL identifiers are case-sensitive when quoted. This method wraps
     * the identifier in double quotes and escapes any internal quotes.
     *
     * @param identifier The identifier to escape
     * @return Escaped identifier safe for use in SQL
     */
    private String escapeIdentifier(String identifier) {
        // PostgreSQL identifiers: wrap in double quotes and escape internal quotes
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public Optional<StockMovement> findByIdAndTenantId(StockMovementId id, TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying stock movement! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying stock movement");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), id.getValue()).map(mapper::toDomain);
    }

    @Override
    public List<StockMovement> findByTenantId(TenantId tenantId) {
        return queryWithSchema(tenantId, () -> jpaRepository.findByTenantId(tenantId.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    /**
     * Helper method to execute queries with schema resolution.
     *
     * @param tenantId Tenant ID
     * @param query    Query to execute
     * @param <T>      Return type
     * @return Query result
     */
    private <T> T queryWithSchema(TenantId tenantId, java.util.function.Supplier<T> query) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying stock movements! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying stock movements");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now execute the query (will use the schema set in search_path)
        return query.get();
    }

    @Override
    public List<StockMovement> findByTenantIdAndStockItemId(TenantId tenantId, String stockItemId) {
        return queryWithSchema(tenantId,
                () -> jpaRepository.findByTenantIdAndStockItemId(tenantId.getValue(), stockItemId).stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public List<StockMovement> findByTenantIdAndSourceLocationId(TenantId tenantId, LocationId sourceLocationId) {
        return queryWithSchema(tenantId, () -> jpaRepository.findByTenantIdAndSourceLocationId(tenantId.getValue(), sourceLocationId.getValue()).stream().map(mapper::toDomain)
                .collect(Collectors.toList()));
    }

    @Override
    public List<StockMovement> findByTenantIdAndDestinationLocationId(TenantId tenantId, LocationId destinationLocationId) {
        return queryWithSchema(tenantId,
                () -> jpaRepository.findByTenantIdAndDestinationLocationId(tenantId.getValue(), destinationLocationId.getValue()).stream().map(mapper::toDomain)
                        .collect(Collectors.toList()));
    }

    @Override
    public List<StockMovement> findByTenantIdAndStatus(TenantId tenantId, MovementStatus status) {
        return queryWithSchema(tenantId, () -> jpaRepository.findByTenantIdAndStatus(tenantId.getValue(), status).stream().map(mapper::toDomain).collect(Collectors.toList()));
    }
}

