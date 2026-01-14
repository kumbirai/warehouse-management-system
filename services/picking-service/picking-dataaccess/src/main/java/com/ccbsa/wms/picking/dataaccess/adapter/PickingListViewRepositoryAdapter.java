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

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.picking.application.service.port.data.PickingListViewRepository;
import com.ccbsa.wms.picking.application.service.query.dto.PickingListView;
import com.ccbsa.wms.picking.dataaccess.entity.LoadEntity;
import com.ccbsa.wms.picking.dataaccess.entity.OrderEntity;
import com.ccbsa.wms.picking.dataaccess.entity.PickingListEntity;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListReference;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: PickingListViewRepositoryAdapter
 * <p>
 * Implements PickingListViewRepository data port interface. Provides read model access to picking list data.
 * <p>
 * This adapter handles tenant schema resolution and search_path setting for multi-tenant isolation.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class PickingListViewRepositoryAdapter implements PickingListViewRepository {
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<PickingListView> findByIdAndTenantId(PickingListId id, TenantId tenantId) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying picking list view!");
            throw new IllegalStateException("TenantContext must be set before querying picking list view");
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

        // Step 1: Fetch picking list only (without loads to avoid MultipleBagFetchException)
        jakarta.persistence.Query query = entityManager.createQuery("SELECT p FROM PickingListEntity p " + "WHERE p.tenantId = :tenantId AND p.id = :id", PickingListEntity.class);
        query.setParameter("tenantId", tenantId.getValue());
        query.setParameter("id", id.getValue());

        @SuppressWarnings("unchecked") List<PickingListEntity> results = query.getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        PickingListEntity entity = results.get(0);

        // Step 2: Batch fetch loads for the picking list (without orders)
        jakarta.persistence.Query loadsQuery =
                entityManager.createQuery("SELECT l FROM LoadEntity l " + "WHERE l.pickingList.id = :pickingListId AND l.tenantId = :tenantId", LoadEntity.class);
        loadsQuery.setParameter("pickingListId", id.getValue());
        loadsQuery.setParameter("tenantId", tenantId.getValue());

        @SuppressWarnings("unchecked") List<LoadEntity> loads = loadsQuery.getResultList();

        // Associate loads with picking list
        entity.getLoads().clear();
        entity.getLoads().addAll(loads);
        loads.forEach(load -> load.setPickingList(entity));

        // Step 3: Batch fetch orders for all loads to initialize collections for counting
        if (!loads.isEmpty()) {
            List<java.util.UUID> loadIds = loads.stream().map(LoadEntity::getId).distinct().toList();

            jakarta.persistence.Query ordersQuery =
                    entityManager.createQuery("SELECT o FROM OrderEntity o " + "JOIN FETCH o.load l " + "WHERE l.id IN :loadIds", OrderEntity.class);
            ordersQuery.setParameter("loadIds", loadIds);

            @SuppressWarnings("unchecked") List<OrderEntity> orders = ordersQuery.getResultList();

            // Associate orders with their loads
            orders.forEach(order -> {
                LoadEntity fetchedLoad = order.getLoad();
                if (fetchedLoad != null) {
                    LoadEntity correctLoad = entity.getLoads().stream().filter(l -> l.getId().equals(fetchedLoad.getId())).findFirst().orElse(null);
                    if (correctLoad != null) {
                        order.setLoad(correctLoad);
                        if (!correctLoad.getOrders().contains(order)) {
                            correctLoad.getOrders().add(order);
                        }
                    }
                }
            });
        }

        // Map to view DTO
        return Optional.of(toView(entity));
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
     * Maps PickingListEntity to PickingListView DTO.
     *
     * @param entity PickingListEntity to map
     * @return PickingListView DTO
     */
    private PickingListView toView(PickingListEntity entity) {
        // Calculate load count
        int loadCount = entity.getLoads() != null ? entity.getLoads().size() : 0;

        // Calculate total order count across all loads
        int totalOrderCount = 0;
        if (entity.getLoads() != null) {
            totalOrderCount = entity.getLoads().stream().mapToInt(load -> load.getOrders() != null ? load.getOrders().size() : 0).sum();
        }

        PickingListView.PickingListViewBuilder builder =
                PickingListView.builder().id(PickingListId.of(entity.getId())).status(entity.getStatus()).receivedAt(entity.getReceivedAt()).processedAt(entity.getProcessedAt())
                        .loadCount(loadCount).totalOrderCount(totalOrderCount).notes(entity.getNotes());

        if (entity.getPickingListReference() != null) {
            builder.pickingListReference(PickingListReference.of(entity.getPickingListReference()));
        }

        return builder.build();
    }

    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated")
    private void executeSetSearchPath(Connection connection, String schemaName) {
        try (Statement stmt = connection.createStatement()) {
            String setSchemaSql = String.format("SET search_path TO %s", escapeIdentifier(schemaName));
            stmt.execute(setSchemaSql);
        } catch (SQLException e) {
            String errorMessage = String.format("Failed to set database schema: %s. SQL State: %s, Error Code: %d. Root cause: %s", schemaName, e.getSQLState(), e.getErrorCode(),
                    e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    private String escapeIdentifier(String identifier) {
        return String.format("\"%s\"", identifier.replace("\"", "\"\""));
    }

    @Override
    public List<PickingListView> findByTenantId(TenantId tenantId, PickingListStatus status, int page, int size) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying picking list views!");
            throw new IllegalStateException("TenantContext must be set before querying picking list views");
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

        // Query entities with pagination using EntityManager to ensure search_path is respected
        Pageable pageable = PageRequest.of(page, size);
        List<PickingListEntity> entities;

        try {
            if (status != null) {
                jakarta.persistence.Query query =
                        entityManager.createQuery("SELECT p FROM PickingListEntity p " + "WHERE p.tenantId = :tenantId AND p.status = :status " + "ORDER BY p.receivedAt DESC",
                                PickingListEntity.class);
                query.setParameter("tenantId", tenantId.getValue());
                query.setParameter("status", status);
                query.setFirstResult((int) pageable.getOffset());
                query.setMaxResults(pageable.getPageSize());

                @SuppressWarnings("unchecked") List<PickingListEntity> resultList = query.getResultList();
                entities = resultList;
            } else {
                jakarta.persistence.Query query =
                        entityManager.createQuery("SELECT p FROM PickingListEntity p " + "WHERE p.tenantId = :tenantId " + "ORDER BY p.receivedAt DESC", PickingListEntity.class);
                query.setParameter("tenantId", tenantId.getValue());
                query.setFirstResult((int) pageable.getOffset());
                query.setMaxResults(pageable.getPageSize());

                @SuppressWarnings("unchecked") List<PickingListEntity> resultList = query.getResultList();
                entities = resultList;
            }
        } catch (Exception e) {
            String errorMessage =
                    String.format("Failed to query picking list entities for tenant: %s, status: %s, page: %d, size: %d, schema: %s. Root cause: %s", tenantId.getValue(),
                            status != null ? status.name() : "null", page, size, schemaName, e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }

        // Initialize loads collection to avoid lazy loading issues
        entities.forEach(entity -> {
            if (entity.getLoads() != null) {
                @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "size() call is intentional to force initialization of lazy-loaded "
                        + "collections")
                int loadsSize = entity.getLoads().size(); // Initialize collection
                entity.getLoads().forEach(load -> {
                    if (load.getOrders() != null) {
                        @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "size() call is intentional to force initialization of lazy-loaded "
                                + "collections")
                        int ordersSize = load.getOrders().size(); // Initialize orders collection
                        // Sizes are intentionally not used - collection initialization is the side effect
                    }
                });
            }
        });

        // Map to views
        return entities.stream().map(this::toView).collect(Collectors.toList());
    }

    @Override
    public long countByTenantId(TenantId tenantId, PickingListStatus status) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when counting picking list views!");
            throw new IllegalStateException("TenantContext must be set before counting picking list views");
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

        // Count entities using EntityManager to ensure search_path is respected
        try {
            if (status != null) {
                jakarta.persistence.Query query =
                        entityManager.createQuery("SELECT COUNT(p) FROM PickingListEntity p " + "WHERE p.tenantId = :tenantId AND p.status = :status", Long.class);
                query.setParameter("tenantId", tenantId.getValue());
                query.setParameter("status", status);
                Long count = (Long) query.getSingleResult();
                return count != null ? count : 0L;
            } else {
                jakarta.persistence.Query query = entityManager.createQuery("SELECT COUNT(p) FROM PickingListEntity p " + "WHERE p.tenantId = :tenantId", Long.class);
                query.setParameter("tenantId", tenantId.getValue());
                Long count = (Long) query.getSingleResult();
                return count != null ? count : 0L;
            }
        } catch (Exception e) {
            String errorMessage = String.format("Failed to count picking list entities for tenant: %s, status: %s, schema: %s. Root cause: %s", tenantId.getValue(),
                    status != null ? status.name() : "null", schemaName, e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
}
