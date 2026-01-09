package com.ccbsa.wms.picking.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.Session;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.LoadNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.picking.application.service.port.repository.LoadRepository;
import com.ccbsa.wms.picking.dataaccess.entity.LoadEntity;
import com.ccbsa.wms.picking.dataaccess.entity.OrderEntity;
import com.ccbsa.wms.picking.dataaccess.entity.OrderLineItemEntity;
import com.ccbsa.wms.picking.dataaccess.jpa.LoadJpaRepository;
import com.ccbsa.wms.picking.dataaccess.mapper.LoadEntityMapper;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadStatus;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: LoadRepositoryAdapter
 * <p>
 * Implements LoadRepository port interface.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class LoadRepositoryAdapter implements LoadRepository {
    private final LoadJpaRepository jpaRepository;
    private final LoadEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(Load load) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext must be set before saving load");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        Optional<LoadEntity> existingEntity = jpaRepository.findByTenantIdAndId(load.getTenantId().getValue(), load.getId().getValue());

        LoadEntity entity;
        if (existingEntity.isPresent()) {
            entity = existingEntity.get();
            updateEntityFromDomain(entity, load, session);
        } else {
            entity = mapper.toEntity(load);
            // Check if any orders are already in the session and merge them
            mergeOrdersIfInSession(entity, session);
        }

        jpaRepository.save(entity);
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

    private void updateEntityFromDomain(LoadEntity entity, Load load, Session session) {
        entity.setStatus(load.getStatus());
        entity.setCreatedAt(load.getCreatedAt());
        entity.setPlannedAt(load.getPlannedAt());

        entity.getOrders().clear();
        LoadEntity newEntity = mapper.toEntity(load);
        // Check if orders are already in session and merge them instead of creating new instances
        for (OrderEntity newOrderEntity : newEntity.getOrders()) {
            if (newOrderEntity.getId() == null) {
                log.error("OrderEntity has null ID - cannot check session. This should not happen as IDs are set in mapper.");
                throw new IllegalStateException("OrderEntity ID cannot be null when saving Load");
            }
            OrderEntity existingOrder = session.get(OrderEntity.class, newOrderEntity.getId());
            if (existingOrder != null) {
                // Order already in session - update it and use the existing instance
                updateOrderEntityFromNew(existingOrder, newOrderEntity);
                existingOrder.setLoad(entity);
                entity.getOrders().add(existingOrder);
            } else {
                // Order not in session - use the new instance
                newOrderEntity.setLoad(entity);
                entity.getOrders().add(newOrderEntity);
            }
        }
    }

    private void mergeOrdersIfInSession(LoadEntity entity, Session session) {
        List<OrderEntity> ordersToProcess = new ArrayList<>(entity.getOrders());
        for (OrderEntity orderEntity : ordersToProcess) {
            if (orderEntity.getId() == null) {
                log.error("OrderEntity has null ID - cannot check session. This should not happen as IDs are set in mapper.");
                throw new IllegalStateException("OrderEntity ID cannot be null when merging in session");
            }
            OrderEntity existingOrder = session.get(OrderEntity.class, orderEntity.getId());
            if (existingOrder != null) {
                // Order already in session - update it and use the existing instance
                updateOrderEntityFromNew(existingOrder, orderEntity);
                existingOrder.setLoad(entity);
                // Replace the new instance with the existing one
                entity.getOrders().remove(orderEntity);
                entity.getOrders().add(existingOrder);
            }
        }
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

    private void updateOrderEntityFromNew(OrderEntity existing, OrderEntity newEntity) {
        existing.setOrderNumber(newEntity.getOrderNumber());
        existing.setCustomerCode(newEntity.getCustomerCode());
        existing.setCustomerName(newEntity.getCustomerName());
        existing.setPriority(newEntity.getPriority());
        existing.setStatus(newEntity.getStatus());
        existing.setCreatedAt(newEntity.getCreatedAt());
        existing.setCompletedAt(newEntity.getCompletedAt());
        // Update line items - validate all have IDs before adding (prevents null key errors in Hibernate)
        existing.getLineItems().clear();
        if (newEntity.getLineItems() != null) {
            for (OrderLineItemEntity lineItem : newEntity.getLineItems()) {
                if (lineItem.getId() == null) {
                    log.error("OrderLineItemEntity has null ID in updateOrderEntityFromNew - cannot add to collection");
                    throw new IllegalStateException("OrderLineItemEntity ID cannot be null when updating OrderEntity");
                }
            }
        }
        existing.getLineItems().addAll(newEntity.getLineItems());
        existing.getLineItems().forEach(lineItem -> lineItem.setOrder(existing));
    }

    private String escapeIdentifier(String identifier) {
        return String.format("\"%s\"", identifier.replace("\"", "\"\""));
    }

    @Override
    public Optional<Load> findByIdAndTenantId(LoadId id, TenantId tenantId) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null || !contextTenantId.getValue().equals(tenantId.getValue())) {
            throw new IllegalStateException("TenantContext mismatch");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Step 1: Fetch Load with orders (without lineItems to avoid MultipleBagFetchException)
        jakarta.persistence.Query loadQuery =
                entityManager.createQuery("SELECT l FROM LoadEntity l " + "LEFT JOIN FETCH l.orders " + "WHERE l.tenantId = :tenantId AND l.id = :id", LoadEntity.class);
        loadQuery.setParameter("tenantId", tenantId.getValue());
        loadQuery.setParameter("id", id.getValue());

        @SuppressWarnings("unchecked") List<LoadEntity> results = loadQuery.getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        LoadEntity entity = results.get(0);

        // Step 2: Batch fetch lineItems for all orders to avoid N+1 query problem
        if (entity.getOrders() != null && !entity.getOrders().isEmpty()) {
            List<UUID> orderIds = entity.getOrders().stream().map(OrderEntity::getId).distinct().toList();

            jakarta.persistence.Query lineItemsQuery =
                    entityManager.createQuery("SELECT o FROM OrderEntity o " + "LEFT JOIN FETCH o.lineItems " + "WHERE o.id IN :orderIds", OrderEntity.class);
            lineItemsQuery.setParameter("orderIds", orderIds);

            @SuppressWarnings("unchecked") List<OrderEntity> ordersWithLineItems = lineItemsQuery.getResultList();

            // Associate lineItems with their orders
            ordersWithLineItems.forEach(orderWithLineItems -> {
                OrderEntity order = entity.getOrders().stream().filter(o -> o.getId().equals(orderWithLineItems.getId())).findFirst().orElse(null);
                if (order != null) {
                    order.setLineItems(orderWithLineItems.getLineItems());
                }
            });
        }

        return Optional.of(mapper.toDomain(entity));
    }

    @Override
    public Optional<Load> findByLoadNumberAndTenantId(LoadNumber loadNumber, TenantId tenantId) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null || !contextTenantId.getValue().equals(tenantId.getValue())) {
            throw new IllegalStateException("TenantContext mismatch");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        return jpaRepository.findByTenantIdAndLoadNumber(tenantId.getValue(), loadNumber.getValue()).map(mapper::toDomain);
    }

    @Override
    public List<Load> findByTenantId(TenantId tenantId, LoadStatus status, int page, int size) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null || !contextTenantId.getValue().equals(tenantId.getValue())) {
            throw new IllegalStateException("TenantContext mismatch");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        Pageable pageable = PageRequest.of(page, size);
        List<LoadEntity> entities;
        if (status != null) {
            entities = jpaRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId.getValue(), status, pageable);
        } else {
            entities = jpaRepository.findByTenantIdOrderByCreatedAtDesc(tenantId.getValue(), pageable);
        }

        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<PickingListId> findPickingListIdByLoadId(LoadId loadId, TenantId tenantId) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null || !contextTenantId.getValue().equals(tenantId.getValue())) {
            throw new IllegalStateException("TenantContext mismatch");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        Optional<LoadEntity> loadEntity = jpaRepository.findByTenantIdAndId(tenantId.getValue(), loadId.getValue());
        if (loadEntity.isEmpty() || loadEntity.get().getPickingList() == null) {
            return Optional.empty();
        }

        return Optional.of(PickingListId.of(loadEntity.get().getPickingList().getId()));
    }
}
