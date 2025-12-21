package com.ccbsa.wms.location.dataaccess.adapter;

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

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.dataaccess.entity.LocationEntity;
import com.ccbsa.wms.location.dataaccess.jpa.LocationJpaRepository;
import com.ccbsa.wms.location.dataaccess.mapper.LocationEntityMapper;
import com.ccbsa.wms.location.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCapacity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Repository Adapter: LocationRepositoryAdapter
 * <p>
 * Implements LocationRepository port interface. Adapts between domain Location aggregate and JPA LocationEntity.
 */
@Repository
public class LocationRepositoryAdapter implements LocationRepository {
    private static final Logger logger = LoggerFactory.getLogger(LocationRepositoryAdapter.class);

    private final LocationJpaRepository jpaRepository;
    private final LocationEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    public LocationRepositoryAdapter(LocationJpaRepository jpaRepository, LocationEntityMapper mapper, TenantSchemaResolver schemaResolver,
                                     TenantSchemaProvisioner schemaProvisioner) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.schemaResolver = schemaResolver;
        this.schemaProvisioner = schemaProvisioner;
    }

    @Override
    public Location save(Location location) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            logger.error("TenantContext is not set when saving location! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before saving location");
        }

        // Verify tenantId matches
        if (!tenantId.getValue().equals(location.getTenantId().getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Location: {}", tenantId.getValue(), location.getTenantId().getValue());
            throw new IllegalStateException("TenantContext tenantId does not match location tenantId");
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
        Optional<LocationEntity> existingEntity = jpaRepository.findByTenantIdAndId(location.getTenantId().getValue(), location.getId().getValue());

        LocationEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            updateEntityFromDomain(entity, location);
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity(location);
        }

        LocationEntity savedEntity = jpaRepository.save(entity);
        logger.debug("Location saved successfully to schema: '{}'", schemaName);

        // Domain events are preserved by the command handler before calling save()
        // The command handler gets domain events from the original location before save()
        // and publishes them after transaction commit. We return the saved location
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
     * Updates an existing entity with values from the domain model. Preserves JPA managed state and version for optimistic locking.
     *
     * @param entity   Existing JPA entity
     * @param location Domain location aggregate
     */
    private void updateEntityFromDomain(LocationEntity entity, Location location) {
        entity.setBarcode(location.getBarcode().getValue());
        entity.setCode(location.getCode());
        entity.setName(location.getName());
        entity.setType(location.getType());
        entity.setZone(location.getCoordinates().getZone());
        entity.setAisle(location.getCoordinates().getAisle());
        entity.setRack(location.getCoordinates().getRack());
        entity.setLevel(location.getCoordinates().getLevel());
        entity.setStatus(location.getStatus());

        // Update capacity
        LocationCapacity capacity = location.getCapacity();
        if (capacity != null) {
            entity.setCurrentQuantity(capacity.getCurrentQuantity());
            entity.setMaximumQuantity(capacity.getMaximumQuantity());
        }

        entity.setDescription(location.getDescription());
        entity.setLastModifiedAt(location.getLastModifiedAt());

        // Update parent location ID
        if (location.getParentLocationId() != null) {
            entity.setParentLocationId(location.getParentLocationId().getValue());
        } else {
            entity.setParentLocationId(null);
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
    public Optional<Location> findByIdAndTenantId(LocationId id, TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying location! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying location");
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
    public boolean existsByBarcodeAndTenantId(LocationBarcode barcode, TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when checking barcode existence! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before checking barcode existence");
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
        return jpaRepository.existsByTenantIdAndBarcode(tenantId.getValue(), barcode.getValue());
    }

    @Override
    public Optional<Location> findByBarcodeAndTenantId(LocationBarcode barcode, TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying location by barcode! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying location by barcode");
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
        return jpaRepository.findByTenantIdAndBarcode(tenantId.getValue(), barcode.getValue()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCodeAndTenantId(String code, TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when checking code existence! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before checking code existence");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Code must not be null for uniqueness check
        if (code == null || code.trim().isEmpty()) {
            return false;
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
        return jpaRepository.existsByTenantIdAndCode(tenantId.getValue(), code);
    }

    @Override
    public List<Location> findByTenantId(TenantId tenantId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying locations! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying locations");
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
        return jpaRepository.findByTenantId(tenantId.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}

