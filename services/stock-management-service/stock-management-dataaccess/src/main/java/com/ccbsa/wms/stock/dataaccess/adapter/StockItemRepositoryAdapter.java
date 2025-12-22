package com.ccbsa.wms.stock.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.dataaccess.entity.StockItemEntity;
import com.ccbsa.wms.stock.dataaccess.jpa.StockItemJpaRepository;
import com.ccbsa.wms.stock.dataaccess.mapper.StockItemEntityMapper;
import com.ccbsa.wms.stock.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Repository Adapter: StockItemRepositoryAdapter
 * <p>
 * Implements StockItemRepository port interface. Adapts between domain StockItem aggregate and JPA StockItemEntity.
 */
@Repository
public class StockItemRepositoryAdapter implements StockItemRepository {
    private static final Logger logger = LoggerFactory.getLogger(StockItemRepositoryAdapter.class);

    private final StockItemJpaRepository jpaRepository;
    private final StockItemEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    public StockItemRepositoryAdapter(StockItemJpaRepository jpaRepository, StockItemEntityMapper mapper, TenantSchemaResolver schemaResolver,
                                      TenantSchemaProvisioner schemaProvisioner) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.schemaResolver = schemaResolver;
        this.schemaProvisioner = schemaProvisioner;
    }

    @Override
    public StockItem save(StockItem stockItem) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            logger.error("TenantContext is not set when saving stock item! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before saving stock item");
        }

        // Verify tenantId matches
        if (!tenantId.getValue().equals(stockItem.getTenantId().getValue())) {
            logger.error("TenantContext mismatch! Context: {}, StockItem: {}", tenantId.getValue(), stockItem.getTenantId().getValue());
            throw new IllegalStateException("TenantContext tenantId does not match stock item tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Check if entity already exists to handle version correctly
        Optional<StockItemEntity> existingEntity = jpaRepository.findByTenantIdAndId(stockItem.getTenantId().getValue(), stockItem.getId().getValue());

        StockItemEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            updateEntityFromDomain(entity, stockItem);
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity(stockItem);
        }

        StockItemEntity savedEntity = jpaRepository.save(entity);
        logger.debug("Stock item saved successfully to schema: '{}'", schemaName);

        // Return domain entity (mapped from saved entity)
        return mapper.toDomain(savedEntity);
    }

    /**
     * Validates that a schema name matches expected patterns to prevent SQL injection.
     */
    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }

        if ("public".equals(schemaName)) {
            return;
        }

        if (schemaName.matches("^tenant_[a-zA-Z0-9_]+_schema$")) {
            return;
        }

        throw new IllegalArgumentException(String.format("Invalid schema name format: '%s'. Expected 'public' or 'tenant_*_schema' pattern", schemaName));
    }

    /**
     * Sets the PostgreSQL search_path for the current database connection.
     */
    private void setSearchPath(Session session, String schemaName) {
        session.doWork(connection -> executeSetSearchPath(connection, schemaName));
    }

    /**
     * Updates an existing entity with values from the domain model.
     */
    private void updateEntityFromDomain(StockItemEntity entity, StockItem stockItem) {
        entity.setProductId(stockItem.getProductId().getValue());
        entity.setLocationId(stockItem.getLocationId() != null ? stockItem.getLocationId().getValue() : null);
        entity.setQuantity(stockItem.getQuantity().getValue());
        entity.setExpirationDate(stockItem.getExpirationDate() != null ? stockItem.getExpirationDate().getValue() : null);
        entity.setClassification(stockItem.getClassification());
        entity.setConsignmentId(stockItem.getConsignmentId() != null ? stockItem.getConsignmentId().getValue() : null);
        entity.setLastModifiedAt(stockItem.getLastModifiedAt());

        // Version is managed by JPA - don't update it manually
    }

    /**
     * Executes the SET search_path SQL command.
     */
    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated against expected patterns and properly escaped")
    private void executeSetSearchPath(Connection connection, String schemaName) {
        try (Statement stmt = connection.createStatement()) {
            String setSchemaSql = String.format("SET search_path TO %s", escapeIdentifier(schemaName));
            logger.debug("Setting search_path to: {}", schemaName);
            stmt.execute(setSchemaSql);
        } catch (SQLException e) {
            logger.error("Failed to set search_path to schema '{}': {}", schemaName, e.getMessage(), e);
            throw new RuntimeException("Failed to set database schema", e);
        }
    }

    /**
     * Escapes a PostgreSQL identifier to prevent SQL injection.
     */
    private String escapeIdentifier(String identifier) {
        return String.format("\"%s\"", identifier.replace("\"", "\"\""));
    }

    @Override
    public Optional<StockItem> findById(StockItemId stockItemId, TenantId tenantId) {
        verifyTenantContext(tenantId);
        prepareSchema(tenantId);

        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), stockItemId.getValue()).map(mapper::toDomain);
    }

    /**
     * Verifies TenantContext is set and matches the provided tenantId.
     */
    private void verifyTenantContext(TenantId tenantId) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set");
        }

        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }
    }

    /**
     * Prepares the schema for database operations.
     */
    private String prepareSchema(TenantId tenantId) {
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        return schemaName;
    }

    @Override
    public List<StockItem> findByConsignmentId(ConsignmentId consignmentId, TenantId tenantId) {
        verifyTenantContext(tenantId);
        prepareSchema(tenantId);

        return jpaRepository.findByTenantIdAndConsignmentId(tenantId.getValue(), consignmentId.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<StockItem> findByClassification(StockClassification classification, TenantId tenantId) {
        verifyTenantContext(tenantId);
        prepareSchema(tenantId);

        return jpaRepository.findByTenantIdAndClassification(tenantId.getValue(), classification).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsById(StockItemId stockItemId, TenantId tenantId) {
        verifyTenantContext(tenantId);
        prepareSchema(tenantId);

        return jpaRepository.existsByTenantIdAndId(tenantId.getValue(), stockItemId.getValue());
    }
}

