package com.ccbsa.wms.stock.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.stock.application.service.port.data.StockConsignmentViewRepository;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockConsignmentView;
import com.ccbsa.wms.stock.dataaccess.entity.StockConsignmentViewEntity;
import com.ccbsa.wms.stock.dataaccess.jpa.StockConsignmentViewJpaRepository;
import com.ccbsa.wms.stock.dataaccess.mapper.StockConsignmentViewEntityMapper;
import com.ccbsa.wms.stock.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: StockConsignmentViewRepositoryAdapter
 * <p>
 * Implements StockConsignmentViewRepository data port interface. Provides read model access to consignment data.
 * <p>
 * This adapter handles tenant schema resolution and search_path setting for multi-tenant isolation.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class StockConsignmentViewRepositoryAdapter implements StockConsignmentViewRepository {
    private final StockConsignmentViewJpaRepository jpaRepository;
    private final StockConsignmentViewEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<StockConsignmentView> findByTenantIdAndId(TenantId tenantId, ConsignmentId consignmentId) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying consignment view!");
            throw new IllegalStateException("TenantContext must be set before querying consignment view");
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

        // Query view entity (with line items loaded)
        Optional<StockConsignmentViewEntity> entityOpt = jpaRepository.findByTenantIdAndId(tenantId.getValue(), consignmentId.getValue());
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        StockConsignmentViewEntity entity = entityOpt.get();
        // Line items are loaded via @OneToMany relationship

        // Map to view DTO
        return Optional.of(mapper.toView(entity));
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
    public List<StockConsignmentView> findByTenantId(TenantId tenantId, int page, int size) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying consignment views!");
            throw new IllegalStateException("TenantContext must be set before querying consignment views");
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
        List<StockConsignmentViewEntity> entities = jpaRepository.findByTenantIdOrderByCreatedAtDesc(tenantId.getValue(), pageable);

        // Map to views (line items loaded via @OneToMany)
        return entities.stream().map(mapper::toView).collect(Collectors.toList());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when counting consignment views!");
            throw new IllegalStateException("TenantContext must be set before counting consignment views");
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

    @Override
    public List<StockConsignmentView> findByTenantId(TenantId tenantId, int page, int size, Integer expiringWithinDays) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying consignment views!");
            throw new IllegalStateException("TenantContext must be set before querying consignment views");
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

        // Calculate expiration date threshold
        LocalDate expirationDateThreshold = LocalDate.now().plusDays(expiringWithinDays);

        // Calculate pagination parameters for native query
        int pageSize = size;
        long offset = (long) page * size;

        // Use EntityManager to execute native query with schema-qualified table names
        // This ensures the query works correctly with schema-per-tenant pattern
        String escapedSchemaName = escapeIdentifier(schemaName);
        String queryString = String.format(
                "SELECT c.id FROM %s.stock_consignments c " + "WHERE c.tenant_id = :tenantId " + "AND EXISTS (" + "    SELECT 1 FROM %s.consignment_line_items li "
                        + "    WHERE li.consignment_id = c.id " + "    AND li.expiration_date IS NOT NULL " + "    AND li.expiration_date <= :expirationDateThreshold" + ") "
                        + "ORDER BY c.created_at DESC " + "LIMIT :pageSize OFFSET :offset", escapedSchemaName, escapedSchemaName);

        jakarta.persistence.Query query = entityManager.createNativeQuery(queryString);
        query.setParameter("tenantId", tenantId.getValue());
        query.setParameter("expirationDateThreshold", expirationDateThreshold);
        query.setParameter("pageSize", pageSize);
        query.setParameter("offset", offset);

        @SuppressWarnings("unchecked") List<Object> resultList = query.getResultList();
        List<UUID> consignmentIds = resultList.stream().map(obj -> {
            if (obj instanceof UUID) {
                return (UUID) obj;
            } else if (obj instanceof String) {
                return UUID.fromString((String) obj);
            } else {
                return UUID.fromString(obj.toString());
            }
        }).collect(Collectors.toList());

        // Then fetch the full entities by IDs, maintaining order
        if (consignmentIds.isEmpty()) {
            return List.of();
        }

        // Fetch entities by IDs - need to maintain the order from the query
        List<StockConsignmentViewEntity> allEntities = jpaRepository.findAllById(consignmentIds);

        // Create a map for O(1) lookup
        java.util.Map<UUID, StockConsignmentViewEntity> entityMap = allEntities.stream().collect(Collectors.toMap(StockConsignmentViewEntity::getId, e -> e));

        // Return entities in the same order as the IDs
        List<StockConsignmentViewEntity> entities = consignmentIds.stream().map(entityMap::get).filter(Objects::nonNull).collect(Collectors.toList());

        // Map to views (line items loaded via @OneToMany)
        return entities.stream().map(mapper::toView).collect(Collectors.toList());
    }

    @Override
    public long countByTenantId(TenantId tenantId, Integer expiringWithinDays) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when counting consignment views!");
            throw new IllegalStateException("TenantContext must be set before counting consignment views");
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

        // Calculate expiration date threshold
        LocalDate expirationDateThreshold = LocalDate.now().plusDays(expiringWithinDays);

        // Use EntityManager to execute native query with schema-qualified table names
        // This ensures the query works correctly with schema-per-tenant pattern
        String escapedSchemaName = escapeIdentifier(schemaName);
        String queryString = String.format("SELECT COUNT(DISTINCT c.id) FROM %s.stock_consignments c " + "INNER JOIN %s.consignment_line_items li ON c.id = li.consignment_id "
                        + "WHERE c.tenant_id = :tenantId " + "AND li.expiration_date IS NOT NULL " + "AND li.expiration_date <= :expirationDateThreshold", escapedSchemaName,
                escapedSchemaName);

        jakarta.persistence.Query query = entityManager.createNativeQuery(queryString);
        query.setParameter("tenantId", tenantId.getValue());
        query.setParameter("expirationDateThreshold", expirationDateThreshold);

        Object result = query.getSingleResult();
        if (result instanceof Number) {
            return ((Number) result).longValue();
        } else if (result instanceof String) {
            return Long.parseLong((String) result);
        } else {
            return Long.parseLong(result.toString());
        }
    }
}

