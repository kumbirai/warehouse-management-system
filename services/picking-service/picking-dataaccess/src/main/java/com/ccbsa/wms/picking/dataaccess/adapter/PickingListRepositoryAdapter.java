package com.ccbsa.wms.picking.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.Session;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.picking.application.service.port.repository.PickingListRepository;
import com.ccbsa.wms.picking.dataaccess.entity.LoadEntity;
import com.ccbsa.wms.picking.dataaccess.entity.OrderEntity;
import com.ccbsa.wms.picking.dataaccess.entity.OrderLineItemEntity;
import com.ccbsa.wms.picking.dataaccess.entity.PickingListEntity;
import com.ccbsa.wms.picking.dataaccess.jpa.PickingListJpaRepository;
import com.ccbsa.wms.picking.dataaccess.mapper.PickingListEntityMapper;
import com.ccbsa.wms.picking.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: PickingListRepositoryAdapter
 * <p>
 * Implements PickingListRepository port interface. Adapts between domain PickingList aggregate and JPA PickingListEntity.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class PickingListRepositoryAdapter implements PickingListRepository {
    private final PickingListJpaRepository jpaRepository;
    private final PickingListEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(PickingList pickingList) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.error("TenantContext is not set when saving picking list! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before saving picking list");
        }

        if (!tenantId.getValue().equals(pickingList.getTenantId().getValue())) {
            log.error("TenantContext mismatch! Context: {}, PickingList: {}", tenantId.getValue(), pickingList.getTenantId().getValue());
            throw new IllegalStateException("TenantContext tenantId does not match picking list tenantId");
        }

        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        Optional<PickingListEntity> existingEntity = jpaRepository.findByTenantIdAndId(pickingList.getTenantId().getValue(), pickingList.getId().getValue());

        PickingListEntity entity;
        if (existingEntity.isPresent()) {
            entity = existingEntity.get();
            updateEntityFromDomain(entity, pickingList, session);
        } else {
            entity = mapper.toEntity(pickingList);
            // Validate all entity IDs are set before saving (prevents null key errors in Hibernate's internal Maps)
            validateEntityIds(entity);
        }

        jpaRepository.save(entity);
        log.debug("Picking list saved successfully to schema: '{}'", schemaName);
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

    private void updateEntityFromDomain(PickingListEntity entity, PickingList pickingList, Session session) {
        entity.setStatus(pickingList.getStatus());
        entity.setReceivedAt(pickingList.getReceivedAt());
        entity.setProcessedAt(pickingList.getProcessedAt());
        entity.setNotes(pickingList.getNotes() != null ? pickingList.getNotes().getValue() : null);

        // Update loads - merge with existing loads in session to avoid NonUniqueObjectException
        entity.getLoads().clear();
        PickingListEntity newEntity = mapper.toEntity(pickingList);

        // Merge loads: check if any loads are already in session
        for (LoadEntity newLoadEntity : newEntity.getLoads()) {
            if (newLoadEntity.getId() == null) {
                log.error("LoadEntity has null ID - cannot check session. This should not happen as IDs are set in mapper.");
                throw new IllegalStateException("LoadEntity ID cannot be null when saving PickingList");
            }
            LoadEntity existingLoad = session.get(LoadEntity.class, newLoadEntity.getId());
            if (existingLoad != null) {
                // Load already in session - update it and use the existing instance
                updateLoadEntityFromNew(existingLoad, newLoadEntity);
                existingLoad.setPickingList(entity);
                entity.getLoads().add(existingLoad);
            } else {
                // Load not in session - use the new instance
                newLoadEntity.setPickingList(entity);
                entity.getLoads().add(newLoadEntity);
            }
        }
    }

    /**
     * Validates that all entity IDs are set before saving.
     * This prevents null key errors in Hibernate's internal Map operations.
     */
    private void validateEntityIds(PickingListEntity entity) {
        if (entity.getId() == null) {
            throw new IllegalStateException("PickingListEntity ID cannot be null");
        }
        if (entity.getLoads() != null) {
            for (LoadEntity load : entity.getLoads()) {
                if (load.getId() == null) {
                    throw new IllegalStateException("LoadEntity ID cannot be null when saving PickingList");
                }
                if (load.getOrders() != null) {
                    for (OrderEntity order : load.getOrders()) {
                        if (order.getId() == null) {
                            throw new IllegalStateException("OrderEntity ID cannot be null when saving PickingList");
                        }
                        if (order.getLineItems() != null) {
                            for (OrderLineItemEntity lineItem : order.getLineItems()) {
                                if (lineItem.getId() == null) {
                                    throw new IllegalStateException("OrderLineItemEntity ID cannot be null when saving PickingList");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated against expected patterns")
    private void executeSetSearchPath(Connection connection, String schemaName) {
        try (Statement stmt = connection.createStatement()) {
            String setSchemaSql = String.format("SET search_path TO %s", escapeIdentifier(schemaName));
            log.debug("Setting search_path to: {}", schemaName);
            stmt.execute(setSchemaSql);
        } catch (SQLException e) {
            log.error("Failed to set search_path to schema '{}': {}", schemaName, e.getMessage(), e);
            throw new RuntimeException("Failed to set database schema", e);
        }
    }

    private void updateLoadEntityFromNew(LoadEntity existing, LoadEntity newEntity) {
        existing.setLoadNumber(newEntity.getLoadNumber());
        existing.setStatus(newEntity.getStatus());
        existing.setCreatedAt(newEntity.getCreatedAt());
        existing.setPlannedAt(newEntity.getPlannedAt());
        // Update orders - clear and rebuild
        existing.getOrders().clear();
        // Validate all orders have IDs before adding (prevents null key errors in Hibernate)
        for (OrderEntity order : newEntity.getOrders()) {
            if (order.getId() == null) {
                log.error("OrderEntity has null ID in updateLoadEntityFromNew - cannot add to collection");
                throw new IllegalStateException("OrderEntity ID cannot be null when updating LoadEntity");
            }
            // Validate line items have IDs
            if (order.getLineItems() != null) {
                for (OrderLineItemEntity lineItem : order.getLineItems()) {
                    if (lineItem.getId() == null) {
                        log.error("OrderLineItemEntity has null ID in updateLoadEntityFromNew - cannot add to collection");
                        throw new IllegalStateException("OrderLineItemEntity ID cannot be null when updating LoadEntity");
                    }
                }
            }
        }
        existing.getOrders().addAll(newEntity.getOrders());
        existing.getOrders().forEach(order -> order.setLoad(existing));
    }

    private String escapeIdentifier(String identifier) {
        return String.format("\"%s\"", identifier.replace("\"", "\"\""));
    }

    @Override
    public Optional<PickingList> findByIdAndTenantId(PickingListId id, TenantId tenantId) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying picking list! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying picking list");
        }

        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            log.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Step 1: Fetch picking list only (without loads to avoid MultipleBagFetchException)
        // We cannot fetch p.loads and l.orders simultaneously as they are both List collections (bags)
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

        // Create a map for quick load lookup
        Map<java.util.UUID, LoadEntity> loadMap = new HashMap<>();
        for (LoadEntity load : loads) {
            if (load.getId() == null) {
                log.error("LoadEntity has null ID in findByIdAndTenantId - cannot add to map");
                throw new IllegalStateException("LoadEntity ID cannot be null");
            }
            loadMap.put(load.getId(), load);
        }

        // Step 3: Batch fetch orders for all loads (without lineItems)
        // Use JOIN FETCH to load the load reference, but don't fetch load.orders to avoid multiple bags
        if (!loads.isEmpty()) {
            List<java.util.UUID> loadIds = loads.stream().map(LoadEntity::getId).distinct().toList();

            jakarta.persistence.Query ordersQuery =
                    entityManager.createQuery("SELECT o FROM OrderEntity o " + "JOIN FETCH o.load l " + "WHERE l.id IN :loadIds", OrderEntity.class);
            ordersQuery.setParameter("loadIds", loadIds);

            @SuppressWarnings("unchecked") List<OrderEntity> orders = ordersQuery.getResultList();

            // Associate orders with their loads using the map to ensure we use the correct load instance
            orders.forEach(order -> {
                LoadEntity fetchedLoad = order.getLoad();
                if (fetchedLoad != null) {
                    LoadEntity correctLoad = loadMap.get(fetchedLoad.getId());
                    if (correctLoad != null) {
                        // Use the load from our map (same instance as in entity.getLoads())
                        order.setLoad(correctLoad);
                        if (!correctLoad.getOrders().contains(order)) {
                            correctLoad.getOrders().add(order);
                        }
                    }
                }
            });

            // Step 4: Batch fetch lineItems for all orders to avoid N+1 query problem
            if (!orders.isEmpty()) {
                List<java.util.UUID> orderIds = orders.stream().map(OrderEntity::getId).distinct().toList();

                jakarta.persistence.Query lineItemsQuery =
                        entityManager.createQuery("SELECT o FROM OrderEntity o " + "LEFT JOIN FETCH o.lineItems " + "WHERE o.id IN :orderIds", OrderEntity.class);
                lineItemsQuery.setParameter("orderIds", orderIds);

                @SuppressWarnings("unchecked") List<OrderEntity> ordersWithLineItems = lineItemsQuery.getResultList();

                // Hibernate will merge the loaded lineItems with the already-loaded orders
                // The query execution loads lineItems into persistence context
                ordersWithLineItems.size(); // Force execution and initialization
            }
        }

        return Optional.of(mapper.toDomain(entity));
    }

    @Override
    public List<PickingList> findByTenantId(TenantId tenantId, PickingListStatus status, int page, int size) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            throw new IllegalStateException("TenantContext must be set before querying picking lists");
        }

        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        Pageable pageable = PageRequest.of(page, size);
        List<PickingListEntity> entities;
        if (status != null) {
            entities = jpaRepository.findByTenantIdAndStatusOrderByReceivedAtDesc(tenantId.getValue(), status, pageable);
        } else {
            entities = jpaRepository.findByTenantIdOrderByReceivedAtDesc(tenantId.getValue(), pageable);
        }

        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByTenantId(TenantId tenantId, PickingListStatus status) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            throw new IllegalStateException("TenantContext must be set before counting picking lists");
        }

        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        if (status != null) {
            return jpaRepository.countByTenantIdAndStatus(tenantId.getValue(), status);
        } else {
            return jpaRepository.countByTenantId(tenantId.getValue());
        }
    }
}
