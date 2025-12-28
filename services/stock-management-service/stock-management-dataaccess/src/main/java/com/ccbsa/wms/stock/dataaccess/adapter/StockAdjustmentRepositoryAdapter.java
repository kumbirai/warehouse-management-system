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
import com.ccbsa.wms.stock.application.service.port.repository.StockAdjustmentRepository;
import com.ccbsa.wms.stock.dataaccess.entity.StockAdjustmentEntity;
import com.ccbsa.wms.stock.dataaccess.jpa.StockAdjustmentJpaRepository;
import com.ccbsa.wms.stock.dataaccess.mapper.StockAdjustmentEntityMapper;
import com.ccbsa.wms.stock.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.stock.domain.core.entity.StockAdjustment;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAdjustmentId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: StockAdjustmentRepositoryAdapter
 * <p>
 * Implements StockAdjustmentRepository port interface. Adapts between domain StockAdjustment aggregate and JPA StockAdjustmentEntity.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class StockAdjustmentRepositoryAdapter implements StockAdjustmentRepository {
    private final StockAdjustmentJpaRepository jpaRepository;
    private final StockAdjustmentEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public StockAdjustment save(StockAdjustment adjustment) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.error("TenantContext is not set when saving stock adjustment! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before saving stock adjustment");
        }

        // Verify tenantId matches
        if (!tenantId.getValue().equals(adjustment.getTenantId().getValue())) {
            log.error("TenantContext mismatch! Context: {}, StockAdjustment: {}", tenantId.getValue(), adjustment.getTenantId().getValue());
            throw new IllegalStateException("TenantContext tenantId does not match stock adjustment tenantId");
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
        Optional<StockAdjustmentEntity> existingEntity = jpaRepository.findByTenantIdAndId(adjustment.getTenantId().getValue(), adjustment.getId().getValue());

        StockAdjustmentEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            updateEntityFromDomain(entity, adjustment);
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity(adjustment);
        }

        StockAdjustmentEntity savedEntity = jpaRepository.save(entity);
        log.debug("StockAdjustment saved successfully to schema: '{}'", schemaName);

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
     * @param adjustment Domain aggregate
     */
    private void updateEntityFromDomain(StockAdjustmentEntity entity, StockAdjustment adjustment) {
        // Update all mutable fields from domain model
        entity.setProductId(adjustment.getProductId().getValue());
        entity.setLocationId(adjustment.getLocationId() != null ? adjustment.getLocationId().getValue() : null);
        entity.setStockItemId(adjustment.getStockItemId() != null ? adjustment.getStockItemId().getValue() : null);
        entity.setAdjustmentType(adjustment.getAdjustmentType());
        entity.setQuantity(adjustment.getQuantity().getValue());
        entity.setReason(adjustment.getReason());
        entity.setAdjustedBy(java.util.UUID.fromString(adjustment.getAdjustedBy().getValue()));
        entity.setAdjustedAt(adjustment.getAdjustedAt());
        entity.setQuantityBefore(adjustment.getQuantityBefore());
        entity.setQuantityAfter(adjustment.getQuantityAfter());
        entity.setLastModifiedAt(adjustment.getAdjustedAt());

        // Update optional fields
        if (adjustment.getNotes() != null) {
            entity.setNotes(adjustment.getNotes());
        } else {
            entity.setNotes(null);
        }
        if (adjustment.getAuthorizationCode() != null) {
            entity.setAuthorizationCode(adjustment.getAuthorizationCode());
        } else {
            entity.setAuthorizationCode(null);
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
    public Optional<StockAdjustment> findByIdAndTenantId(StockAdjustmentId id, TenantId tenantId) {
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
            log.error("TenantContext is not set when querying stock adjustments! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying stock adjustments");
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
    public List<StockAdjustment> findByTenantId(TenantId tenantId) {
        return queryWithSchema(tenantId, () -> jpaRepository.findByTenantId(tenantId.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public List<StockAdjustment> findByTenantIdAndProductId(TenantId tenantId, ProductId productId) {
        return queryWithSchema(tenantId,
                () -> jpaRepository.findByTenantIdAndProductId(tenantId.getValue(), productId.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList()));
    }

    @Override
    public List<StockAdjustment> findByTenantIdAndProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId) {
        return queryWithSchema(tenantId,
                () -> jpaRepository.findByTenantIdAndProductIdAndLocationId(tenantId.getValue(), productId.getValue(), locationId.getValue()).stream().map(mapper::toDomain)
                        .collect(Collectors.toList()));
    }

    @Override
    public List<StockAdjustment> findByTenantIdAndStockItemId(TenantId tenantId, StockItemId stockItemId) {
        return queryWithSchema(tenantId,
                () -> jpaRepository.findByTenantIdAndStockItemId(tenantId.getValue(), stockItemId.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList()));
    }
}

