package com.ccbsa.wms.stock.dataaccess.adapter;

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

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.stock.application.service.port.data.StockAllocationViewRepository;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockAllocationView;
import com.ccbsa.wms.stock.dataaccess.entity.StockAllocationViewEntity;
import com.ccbsa.wms.stock.dataaccess.jpa.StockAllocationViewJpaRepository;
import com.ccbsa.wms.stock.dataaccess.mapper.StockAllocationViewEntityMapper;
import com.ccbsa.wms.stock.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: StockAllocationViewRepositoryAdapter
 * <p>
 * Implements StockAllocationViewRepository data port interface. Provides read model access to allocation data.
 * <p>
 * This adapter handles tenant schema resolution and search_path setting for multi-tenant isolation.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class StockAllocationViewRepositoryAdapter implements StockAllocationViewRepository {
    private final StockAllocationViewJpaRepository jpaRepository;
    private final StockAllocationViewEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<StockAllocationView> findByTenantIdAndId(TenantId tenantId, StockAllocationId allocationId) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying allocation view!");
            throw new IllegalStateException("TenantContext must be set before querying allocation view");
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

        // Query view entity
        Optional<StockAllocationViewEntity> entityOpt = jpaRepository.findByTenantIdAndId(tenantId.getValue(), allocationId.getValue());
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        // Map to view DTO
        return Optional.of(mapper.toView(entityOpt.get()));
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
    public List<StockAllocationView> findByTenantId(TenantId tenantId, int page, int size) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying allocation views!");
            throw new IllegalStateException("TenantContext must be set before querying allocation views");
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

        // Create pageable
        Pageable pageable = PageRequest.of(page, size);

        // Query view entities with pagination
        List<StockAllocationViewEntity> entities = jpaRepository.findByTenantIdOrderByAllocatedAtDesc(tenantId.getValue(), pageable);

        // Map to views
        return entities.stream().map(mapper::toView).collect(Collectors.toList());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when counting allocation views!");
            throw new IllegalStateException("TenantContext must be set before counting allocation views");
        }

        // Verify tenantId matches
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Query: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match query tenantId");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        return jpaRepository.countByTenantId(tenantId.getValue());
    }
}

