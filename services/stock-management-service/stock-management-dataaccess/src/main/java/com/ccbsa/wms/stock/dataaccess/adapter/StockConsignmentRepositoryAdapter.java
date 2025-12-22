package com.ccbsa.wms.stock.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.stock.application.service.port.repository.StockConsignmentRepository;
import com.ccbsa.wms.stock.dataaccess.entity.StockConsignmentEntity;
import com.ccbsa.wms.stock.dataaccess.jpa.StockConsignmentJpaRepository;
import com.ccbsa.wms.stock.dataaccess.mapper.StockConsignmentEntityMapper;
import com.ccbsa.wms.stock.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.stock.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Repository Adapter: StockConsignmentRepositoryAdapter
 * <p>
 * Implements StockConsignmentRepository port interface. Adapts between domain StockConsignment aggregate and JPA StockConsignmentEntity.
 */
@Repository
public class StockConsignmentRepositoryAdapter implements StockConsignmentRepository {
    private static final Logger logger = LoggerFactory.getLogger(StockConsignmentRepositoryAdapter.class);

    private final StockConsignmentJpaRepository jpaRepository;
    private final StockConsignmentEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    public StockConsignmentRepositoryAdapter(StockConsignmentJpaRepository jpaRepository, StockConsignmentEntityMapper mapper, TenantSchemaResolver schemaResolver,
                                             TenantSchemaProvisioner schemaProvisioner) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.schemaResolver = schemaResolver;
        this.schemaProvisioner = schemaProvisioner;
    }

    @Override
    public void save(StockConsignment consignment) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            logger.error("TenantContext is not set when saving consignment! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before saving consignment");
        }

        // Verify tenantId matches
        if (!tenantId.getValue().equals(consignment.getTenantId().getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Consignment: {}", tenantId.getValue(), consignment.getTenantId().getValue());
            throw new IllegalStateException("TenantContext tenantId does not match consignment tenantId");
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
        Optional<StockConsignmentEntity> existingEntity = jpaRepository.findByTenantIdAndId(consignment.getTenantId().getValue(), consignment.getId().getValue());

        StockConsignmentEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            updateEntityFromDomain(entity, consignment);
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity(consignment);
        }

        jpaRepository.save(entity);
        logger.debug("Consignment saved successfully to schema: '{}'", schemaName);

        // Domain events are preserved by the command handler before calling save()
        // The command handler gets domain events from the original consignment before save()
        // and publishes them after transaction commit.
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
     * Updates an existing entity with values from the domain model. Preserves JPA managed state and version for optimistic locking.
     *
     * @param entity      Existing JPA entity
     * @param consignment Domain consignment aggregate
     */
    private void updateEntityFromDomain(StockConsignmentEntity entity, StockConsignment consignment) {
        entity.setConsignmentReference(consignment.getConsignmentReference().getValue());
        entity.setWarehouseId(consignment.getWarehouseId().getValue());
        entity.setStatus(consignment.getStatus());
        entity.setReceivedAt(consignment.getReceivedAt());
        entity.setConfirmedAt(consignment.getConfirmedAt());
        entity.setReceivedBy(consignment.getReceivedBy() != null ? consignment.getReceivedBy().getValue() : null);
        entity.setLastModifiedAt(consignment.getLastModifiedAt());

        // Update line items - remove all existing and add new ones
        entity.getLineItems().clear();
        StockConsignmentEntity newEntity = mapper.toEntity(consignment);
        entity.getLineItems().addAll(newEntity.getLineItems());
        // Update line item references to point to this entity
        entity.getLineItems().forEach(lineItem -> lineItem.setConsignment(entity));

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
            logger.debug("Setting search_path to: {}", schemaName);
            stmt.execute(setSchemaSql);
        } catch (SQLException e) {
            logger.error("Failed to set search_path to schema '{}': {}", schemaName, e.getMessage(), e);
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
        return String.format("\"%s\"", identifier.replace("\"", "\"\""));
    }

    @Override
    public Optional<StockConsignment> findByIdAndTenantId(ConsignmentId id, TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying consignment! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying consignment");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

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
    public Optional<StockConsignment> findByConsignmentReferenceAndTenantId(ConsignmentReference reference, TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying consignment by reference! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying consignment by reference");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.findByTenantIdAndConsignmentReference(tenantId.getValue(), reference.getValue()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByConsignmentReferenceAndTenantId(ConsignmentReference reference, TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when checking consignment reference existence! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before checking consignment reference existence");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.existsByTenantIdAndConsignmentReference(tenantId.getValue(), reference.getValue());
    }
}

