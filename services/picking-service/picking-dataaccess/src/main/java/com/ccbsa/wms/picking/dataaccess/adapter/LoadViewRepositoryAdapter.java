package com.ccbsa.wms.picking.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.LoadNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.picking.application.service.port.data.LoadViewRepository;
import com.ccbsa.wms.picking.application.service.query.dto.LoadView;
import com.ccbsa.wms.picking.dataaccess.entity.LoadEntity;
import com.ccbsa.wms.picking.dataaccess.jpa.LoadJpaRepository;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: LoadViewRepositoryAdapter
 * <p>
 * Implements LoadViewRepository data port interface. Provides read model access to load data.
 * <p>
 * This adapter handles tenant schema resolution and search_path setting for multi-tenant isolation.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class LoadViewRepositoryAdapter implements LoadViewRepository {
    private final LoadJpaRepository jpaRepository;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<LoadView> findByIdAndTenantId(LoadId id, TenantId tenantId) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying load view!");
            throw new IllegalStateException("TenantContext must be set before querying load view");
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

        // Query entity
        Optional<LoadEntity> entityOpt = jpaRepository.findByTenantIdAndId(tenantId.getValue(), id.getValue());
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        // Map to view DTO
        return Optional.of(toView(entityOpt.get()));
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
     * Maps LoadEntity to LoadView DTO.
     *
     * @param entity LoadEntity to map
     * @return LoadView DTO
     */
    private LoadView toView(LoadEntity entity) {
        return LoadView.builder().id(LoadId.of(entity.getId())).loadNumber(LoadNumber.of(entity.getLoadNumber())).status(entity.getStatus()).createdAt(entity.getCreatedAt())
                .plannedAt(entity.getPlannedAt()).orderCount(entity.getOrders() != null ? entity.getOrders().size() : 0).build();
    }

    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated")
    private void executeSetSearchPath(Connection connection, String schemaName) {
        try (Statement stmt = connection.createStatement()) {
            String setSchemaSql = String.format("SET search_path TO %s", escapeIdentifier(schemaName));
            stmt.execute(setSchemaSql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set database schema", e);
        }
    }

    private String escapeIdentifier(String identifier) {
        return String.format("\"%s\"", identifier.replace("\"", "\"\""));
    }

    @Override
    public List<LoadView> findByTenantId(TenantId tenantId, int page, int size) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying load views!");
            throw new IllegalStateException("TenantContext must be set before querying load views");
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

        // Query entities with pagination
        Pageable pageable = PageRequest.of(page, size);
        List<LoadEntity> entities = jpaRepository.findByTenantIdOrderByCreatedAtDesc(tenantId.getValue(), pageable);

        // Map to views
        return entities.stream().map(this::toView).collect(Collectors.toList());
    }
}
