package com.ccbsa.wms.stock.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.stock.application.service.port.data.StockItemViewRepository;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockItemView;
import com.ccbsa.wms.stock.dataaccess.entity.StockItemViewEntity;
import com.ccbsa.wms.stock.dataaccess.jpa.StockItemViewJpaRepository;
import com.ccbsa.wms.stock.dataaccess.mapper.StockItemViewEntityMapper;
import com.ccbsa.wms.stock.dataaccess.schema.TenantSchemaProvisioner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: StockItemViewRepositoryAdapter
 * <p>
 * Implements StockItemViewRepository data port interface. Provides read model access to stock item data.
 * <p>
 * This adapter handles tenant schema resolution and search_path setting for multi-tenant isolation.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class StockItemViewRepositoryAdapter implements StockItemViewRepository {
    private final StockItemViewJpaRepository jpaRepository;
    private final StockItemViewEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<StockItemView> findByTenantIdAndId(TenantId tenantId, StockItemId stockItemId) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying stock item view!");
            throw new IllegalStateException("TenantContext must be set before querying stock item view");
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
        Optional<StockItemViewEntity> entityOpt = jpaRepository.findByTenantIdAndId(tenantId.getValue(), stockItemId.getValue());
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
    public List<StockItemView> findByTenantId(TenantId tenantId) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying stock item views!");
            throw new IllegalStateException("TenantContext must be set before querying stock item views");
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

        // Query view entities
        List<StockItemViewEntity> entities = jpaRepository.findByTenantId(tenantId.getValue());

        // Map to views
        return entities.stream().map(mapper::toView).collect(Collectors.toList());
    }

    @Override
    public List<StockItemView> findByTenantIdAndClassification(TenantId tenantId, StockClassification classification) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying stock item views by classification!");
            throw new IllegalStateException("TenantContext must be set before querying stock item views");
        }

        // Verify tenantId matches
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Query: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match query tenantId");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        log.info("Resolved schema name: '{}' for tenantId: '{}', classification: '{}'", schemaName, tenantId.getValue(), classification);

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Query view entities by classification
        List<StockItemViewEntity> entities = jpaRepository.findByTenantIdAndClassification(tenantId.getValue(), classification);
        log.info("Found {} stock item view entities for tenantId: '{}', classification: '{}' in schema: '{}'", entities.size(), tenantId.getValue(), classification, schemaName);

        // Map to views
        return entities.stream().map(mapper::toView).collect(Collectors.toList());
    }
}

