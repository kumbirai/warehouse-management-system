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
import com.ccbsa.wms.common.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.location.application.service.port.data.StockMovementViewRepository;
import com.ccbsa.wms.location.application.service.port.data.dto.StockMovementView;
import com.ccbsa.wms.location.dataaccess.entity.StockMovementViewEntity;
import com.ccbsa.wms.location.dataaccess.jpa.StockMovementViewJpaRepository;
import com.ccbsa.wms.location.dataaccess.mapper.StockMovementViewEntityMapper;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: StockMovementViewRepositoryAdapter
 * <p>
 * Implements StockMovementViewRepository data port interface. Provides read model access to stock movement data.
 * <p>
 * This adapter handles tenant schema resolution and search_path setting for multi-tenant isolation.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class StockMovementViewRepositoryAdapter implements StockMovementViewRepository {
    private final StockMovementViewJpaRepository jpaRepository;
    private final StockMovementViewEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<StockMovementView> findByTenantIdAndId(TenantId tenantId, StockMovementId stockMovementId) {
        return executeInTenantSchema(tenantId, () -> {
            Optional<StockMovementViewEntity> entity = jpaRepository.findByTenantIdAndId(tenantId.getValue(), stockMovementId.getValue());
            return entity.map(mapper::toView);
        });
    }

    private <R> R executeInTenantSchema(TenantId tenantId, java.util.function.Supplier<R> action) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying stock movement view!");
            throw new IllegalStateException("TenantContext must be set before querying stock movement view");
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
    public List<StockMovementView> findByTenantId(TenantId tenantId) {
        return executeInTenantSchema(tenantId, () -> {
            List<StockMovementViewEntity> entities = jpaRepository.findByTenantId(tenantId.getValue());
            return entities.stream().map(mapper::toView).collect(Collectors.toList());
        });
    }

    @Override
    public List<StockMovementView> findByTenantIdAndStockItemId(TenantId tenantId, String stockItemId) {
        return executeInTenantSchema(tenantId, () -> {
            List<StockMovementViewEntity> entities = jpaRepository.findByTenantIdAndStockItemId(tenantId.getValue(), stockItemId);
            return entities.stream().map(mapper::toView).collect(Collectors.toList());
        });
    }

    @Override
    public List<StockMovementView> findByTenantIdAndSourceLocationId(TenantId tenantId, LocationId sourceLocationId) {
        return executeInTenantSchema(tenantId, () -> {
            List<StockMovementViewEntity> entities = jpaRepository.findByTenantIdAndSourceLocationId(tenantId.getValue(), sourceLocationId.getValue());
            return entities.stream().map(mapper::toView).collect(Collectors.toList());
        });
    }

    @Override
    public List<StockMovementView> findByTenantIdAndDestinationLocationId(TenantId tenantId, LocationId destinationLocationId) {
        return executeInTenantSchema(tenantId, () -> {
            List<StockMovementViewEntity> entities = jpaRepository.findByTenantIdAndDestinationLocationId(tenantId.getValue(), destinationLocationId.getValue());
            return entities.stream().map(mapper::toView).collect(Collectors.toList());
        });
    }

    @Override
    public List<StockMovementView> findByTenantIdAndStatus(TenantId tenantId, MovementStatus status) {
        return executeInTenantSchema(tenantId, () -> {
            List<StockMovementViewEntity> entities = jpaRepository.findByTenantIdAndStatus(tenantId.getValue(), status);
            return entities.stream().map(mapper::toView).collect(Collectors.toList());
        });
    }
}

