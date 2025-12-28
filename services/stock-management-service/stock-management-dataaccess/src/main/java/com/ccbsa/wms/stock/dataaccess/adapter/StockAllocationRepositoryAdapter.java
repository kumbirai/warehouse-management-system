package com.ccbsa.wms.stock.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.repository.StockAllocationRepository;
import com.ccbsa.wms.stock.dataaccess.entity.StockAllocationEntity;
import com.ccbsa.wms.stock.dataaccess.jpa.StockAllocationJpaRepository;
import com.ccbsa.wms.stock.dataaccess.mapper.StockAllocationEntityMapper;
import com.ccbsa.wms.stock.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: StockAllocationRepositoryAdapter
 * <p>
 * Implements StockAllocationRepository port interface. Adapts between domain StockAllocation aggregate and JPA StockAllocationEntity.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class StockAllocationRepositoryAdapter implements StockAllocationRepository {
    private final StockAllocationJpaRepository jpaRepository;
    private final StockAllocationEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public StockAllocation save(StockAllocation allocation) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.error("TenantContext is not set when saving stock allocation! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before saving stock allocation");
        }

        // Verify tenantId matches
        if (!tenantId.getValue().equals(allocation.getTenantId().getValue())) {
            log.error("TenantContext mismatch! Context: {}, StockAllocation: {}", tenantId.getValue(), allocation.getTenantId().getValue());
            throw new IllegalStateException("TenantContext tenantId does not match stock allocation tenantId");
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
        Optional<StockAllocationEntity> existingEntity = jpaRepository.findByTenantIdAndId(allocation.getTenantId().getValue(), allocation.getId().getValue());

        StockAllocationEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            updateEntityFromDomain(entity, allocation);
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity(allocation);
        }

        StockAllocationEntity savedEntity = jpaRepository.save(entity);
        log.debug("StockAllocation saved successfully to schema: '{}'", schemaName);

        // Domain events are preserved by the command handler before calling save()
        return mapper.toDomain(savedEntity);
    }

    /**
     * Validates that a schema name matches expected patterns to prevent SQL injection.
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
     * @param entity     Existing JPA entity
     * @param allocation Domain aggregate
     */
    private void updateEntityFromDomain(StockAllocationEntity entity, StockAllocation allocation) {
        // Update all mutable fields from domain model
        entity.setProductId(allocation.getProductId().getValue());
        entity.setLocationId(allocation.getLocationId() != null ? allocation.getLocationId().getValue() : null);
        entity.setStockItemId(allocation.getStockItemId().getValue());
        entity.setQuantity(allocation.getQuantity().getValue());
        entity.setAllocationType(allocation.getAllocationType());
        entity.setReferenceId(allocation.getReferenceId() != null ? allocation.getReferenceId().getValue() : null);
        entity.setStatus(allocation.getStatus());
        entity.setAllocatedBy(java.util.UUID.fromString(allocation.getAllocatedBy().getValue()));
        entity.setAllocatedAt(allocation.getAllocatedAt());

        // Update release fields if present
        if (allocation.getReleasedAt() != null) {
            entity.setReleasedAt(allocation.getReleasedAt());
            entity.setLastModifiedAt(allocation.getReleasedAt());
        } else {
            entity.setReleasedAt(null);
        }

        // Update notes if present
        if (allocation.getNotes() != null && !allocation.getNotes().isEmpty()) {
            entity.setNotes(allocation.getNotes().getValue());
        } else {
            entity.setNotes(null);
        }

        // Update lastModifiedAt if not set by release
        if (allocation.getReleasedAt() == null) {
            entity.setLastModifiedAt(allocation.getAllocatedAt());
        }

        // Version is managed by JPA - don't update it manually
    }

    /**
     * Executes the SET search_path SQL command.
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
     *
     * @param identifier The identifier to escape
     * @return Escaped identifier safe for use in SQL
     */
    private String escapeIdentifier(String identifier) {
        // PostgreSQL identifiers: wrap in double quotes and escape internal quotes
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public Optional<StockAllocation> findByIdAndTenantId(StockAllocationId id, TenantId tenantId) {
        return queryWithSchema(tenantId, () -> jpaRepository.findByTenantIdAndId(tenantId.getValue(), id.getValue()).map(mapper::toDomain));
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
            log.error("TenantContext is not set when querying stock allocations! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying stock allocations");
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
    public List<StockAllocation> findByTenantId(TenantId tenantId) {
        return queryWithSchema(tenantId, () -> jpaRepository.findByTenantId(tenantId.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public List<StockAllocation> findByTenantIdAndProductId(TenantId tenantId, ProductId productId) {
        return queryWithSchema(tenantId,
                () -> jpaRepository.findByTenantIdAndProductId(tenantId.getValue(), productId.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public List<StockAllocation> findByTenantIdAndProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId) {
        return queryWithSchema(tenantId,
                () -> jpaRepository.findByTenantIdAndProductIdAndLocationId(tenantId.getValue(), productId.getValue(), locationId.getValue()).stream().map(mapper::toDomain)
                        .collect(Collectors.toList()));
    }

    @Override
    public List<StockAllocation> findByStockItemId(StockItemId stockItemId) {
        // Note: This method doesn't have tenantId, but we need it for schema resolution
        // We'll use TenantContext
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.error("TenantContext is not set when querying stock allocations by stock item! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying stock allocations");
        }

        return queryWithSchema(tenantId, () -> jpaRepository.findByStockItemId(stockItemId.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public List<StockAllocation> findByStockItemIdAndStatus(StockItemId stockItemId, AllocationStatus status) {
        // Note: This method doesn't have tenantId, but we need it for schema resolution
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.error("TenantContext is not set when querying stock allocations by stock item and status! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying stock allocations");
        }

        return queryWithSchema(tenantId,
                () -> jpaRepository.findByStockItemIdAndStatus(stockItemId.getValue(), status).stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public List<StockAllocation> findByTenantIdAndReferenceId(TenantId tenantId, String referenceId) {
        return queryWithSchema(tenantId,
                () -> jpaRepository.findByTenantIdAndReferenceId(tenantId.getValue(), referenceId).stream().map(mapper::toDomain).collect(Collectors.toList()));
    }
}

