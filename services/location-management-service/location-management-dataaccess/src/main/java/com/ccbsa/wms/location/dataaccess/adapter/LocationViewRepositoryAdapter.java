package com.ccbsa.wms.location.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.location.application.service.port.data.LocationViewRepository;
import com.ccbsa.wms.location.application.service.port.data.dto.LocationView;
import com.ccbsa.wms.location.dataaccess.entity.LocationViewEntity;
import com.ccbsa.wms.location.dataaccess.jpa.LocationViewJpaRepository;
import com.ccbsa.wms.location.dataaccess.mapper.LocationViewEntityMapper;
import com.ccbsa.wms.location.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: LocationViewRepositoryAdapter
 * <p>
 * Implements LocationViewRepository data port interface. Provides read model access to location data.
 * <p>
 * This adapter handles tenant schema resolution and search_path setting for multi-tenant isolation.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class LocationViewRepositoryAdapter implements LocationViewRepository {
    private final LocationViewJpaRepository jpaRepository;
    private final LocationViewEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<LocationView> findByTenantIdAndId(TenantId tenantId, LocationId locationId) {
        return executeInTenantSchema(tenantId, () -> {
            Optional<LocationViewEntity> entity = jpaRepository.findByTenantIdAndId(tenantId.getValue(), locationId.getValue());
            return entity.map(mapper::toView);
        });
    }

    private <R> R executeInTenantSchema(TenantId tenantId, java.util.function.Supplier<R> action) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying location view!");
            throw new IllegalStateException("TenantContext must be set before querying location view");
        }

        // Verify tenantId matches
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Query: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match query tenantId");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        return action.get();
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }
        if ("public".equals(schemaName)) {
            return;
        }
        if (!schemaName.matches("^tenant_[a-zA-Z0-9_]+_schema$")) {
            throw new IllegalArgumentException(String.format("Invalid schema name format: '%s'", schemaName));
        }
    }

    private void setSearchPath(Session session, String schemaName) {
        session.doWork(connection -> executeSetSearchPath(connection, schemaName));
    }

    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name validated against expected patterns and escaped")
    private void executeSetSearchPath(Connection connection, String schemaName) {
        try (Statement stmt = connection.createStatement()) {
            String sql = String.format("SET search_path TO %s", escapeIdentifier(schemaName));
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set database schema", e);
        }
    }

    private String escapeIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public List<LocationView> findByTenantIdWithFilters(TenantId tenantId, String zone, String status, String search, int page, int size) {
        return executeInTenantSchema(tenantId, () -> {
            Pageable pageable = PageRequest.of(page, size);
            List<LocationViewEntity> entities = jpaRepository.findByTenantIdWithFilters(tenantId.getValue(), zone, status, search, pageable).getContent();
            return entities.stream().map(mapper::toView).collect(Collectors.toList());
        });
    }

    @Override
    public long countByTenantIdWithFilters(TenantId tenantId, String zone, String status, String search) {
        return executeInTenantSchema(tenantId, () -> jpaRepository.countByTenantIdWithFilters(tenantId.getValue(), zone, status, search));
    }

    @Override
    public List<LocationView> findAvailableLocations(TenantId tenantId) {
        return executeInTenantSchema(tenantId, () -> {
            // Available locations are those with status AVAILABLE or RESERVED
            List<LocationViewEntity> entities = jpaRepository.findByTenantIdAndStatusIn(tenantId.getValue(), Arrays.asList(LocationStatus.AVAILABLE, LocationStatus.RESERVED));
            return entities.stream().map(mapper::toView).collect(Collectors.toList());
        });
    }
}

