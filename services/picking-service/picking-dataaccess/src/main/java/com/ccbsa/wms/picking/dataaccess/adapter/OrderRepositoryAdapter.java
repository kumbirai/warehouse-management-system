package com.ccbsa.wms.picking.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.picking.application.service.port.repository.OrderRepository;
import com.ccbsa.wms.picking.dataaccess.entity.OrderEntity;
import com.ccbsa.wms.picking.dataaccess.jpa.OrderJpaRepository;
import com.ccbsa.wms.picking.dataaccess.mapper.OrderEntityMapper;
import com.ccbsa.wms.picking.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.picking.domain.core.entity.Order;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: OrderRepositoryAdapter
 * <p>
 * Implements OrderRepository port interface.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {
    private final OrderJpaRepository jpaRepository;
    private final OrderEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(Order order) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext must be set before saving order");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        Optional<OrderEntity> existingEntity = jpaRepository.findById(order.getId().getValue());

        OrderEntity entity;
        if (existingEntity.isPresent()) {
            entity = existingEntity.get();
            updateEntityFromDomain(entity, order);
        } else {
            entity = mapper.toEntity(order);
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

    private void updateEntityFromDomain(OrderEntity entity, Order order) {
        entity.setCustomerCode(order.getCustomerInfo().getCustomerCode());
        entity.setCustomerName(order.getCustomerInfo().getCustomerName());
        entity.setPriority(order.getPriority());
        entity.setStatus(order.getStatus());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setCompletedAt(order.getCompletedAt());

        entity.getLineItems().clear();
        OrderEntity newEntity = mapper.toEntity(order);
        entity.getLineItems().addAll(newEntity.getLineItems());
        entity.getLineItems().forEach(lineItem -> lineItem.setOrder(entity));
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
    public Optional<Order> findById(OrderId id) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext must be set");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        jakarta.persistence.Query query = entityManager.createQuery("SELECT o FROM OrderEntity o " + "LEFT JOIN FETCH o.lineItems " + "WHERE o.id = :id", OrderEntity.class);
        query.setParameter("id", id.getValue());

        @SuppressWarnings("unchecked") List<OrderEntity> results = query.getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapper.toDomain(results.get(0)));
    }

    @Override
    public Optional<Order> findByOrderNumberAndTenantId(OrderNumber orderNumber, TenantId tenantId) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null || !contextTenantId.getValue().equals(tenantId.getValue())) {
            throw new IllegalStateException("TenantContext mismatch");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        return jpaRepository.findByOrderNumberAndLoadTenantId(orderNumber.getValue(), tenantId.getValue()).map(mapper::toDomain);
    }

    @Override
    public List<Order> findByLoadId(LoadId loadId) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext must be set");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        jakarta.persistence.Query query =
                entityManager.createQuery("SELECT o FROM OrderEntity o " + "LEFT JOIN FETCH o.lineItems " + "WHERE o.load.id = :loadId", OrderEntity.class);
        query.setParameter("loadId", loadId.getValue());

        @SuppressWarnings("unchecked") List<OrderEntity> entities = query.getResultList();

        return entities.stream().map(mapper::toDomain).toList();
    }
}
