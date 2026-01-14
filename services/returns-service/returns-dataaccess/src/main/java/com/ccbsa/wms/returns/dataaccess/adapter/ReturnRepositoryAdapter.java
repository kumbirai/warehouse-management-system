package com.ccbsa.wms.returns.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.returns.application.service.port.repository.ReturnRepository;
import com.ccbsa.wms.returns.dataaccess.entity.ReturnEntity;
import com.ccbsa.wms.returns.dataaccess.jpa.ReturnJpaRepository;
import com.ccbsa.wms.returns.dataaccess.mapper.ReturnEntityMapper;
import com.ccbsa.wms.returns.domain.core.entity.Return;
import com.ccbsa.common.domain.valueobject.ReturnId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: ReturnRepositoryAdapter
 * <p>
 * Implements ReturnRepository port interface. Adapts between domain Return aggregate and JPA ReturnEntity.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class ReturnRepositoryAdapter implements ReturnRepository {
    private final ReturnJpaRepository jpaRepository;
    private final ReturnEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Return save(Return returnAggregate) {
        // Verify TenantContext is set
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.error("TenantContext is not set when saving return! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before saving return");
        }

        if (!tenantId.getValue().equals(returnAggregate.getTenantId().getValue())) {
            log.error("TenantContext mismatch! Context: {}, Return: {}", tenantId.getValue(), returnAggregate.getTenantId().getValue());
            throw new IllegalStateException("TenantContext tenantId does not match return tenantId");
        }

        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        Optional<ReturnEntity> existingEntity = jpaRepository.findByTenantIdAndReturnId(returnAggregate.getTenantId().getValue(), returnAggregate.getId().getValue());

        ReturnEntity entity;
        if (existingEntity.isPresent()) {
            entity = existingEntity.get();
            updateEntityFromDomain(entity, returnAggregate);
        } else {
            entity = mapper.toEntity(returnAggregate);
        }

        jpaRepository.save(entity);
        log.debug("Return saved successfully to schema: '{}'", schemaName);

        // Return domain entity with updated version
        return mapper.toDomain(entity);
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }
        if ("public".equals(schemaName)) {
            return;
        }
        if (schemaName.matches("^tenant_[a-zA-Z0-9_]+_schema$")) {
            return;
        }
        throw new IllegalArgumentException(String.format("Invalid schema name format: '%s'", schemaName));
    }

    private void setSearchPath(Session session, String schemaName) {
        session.doWork(connection -> executeSetSearchPath(connection, schemaName));
    }

    private void updateEntityFromDomain(ReturnEntity entity, Return returnAggregate) {
        entity.setOrderNumber(returnAggregate.getOrderNumber().getValue());
        entity.setReturnType(returnAggregate.getReturnType());
        entity.setReturnStatus(returnAggregate.getStatus());
        entity.setReturnedAt(returnAggregate.getReturnedAt());
        entity.setLastModifiedAt(returnAggregate.getLastModifiedAt());

        if (returnAggregate.getCustomerSignature() != null) {
            entity.setCustomerSignature(returnAggregate.getCustomerSignature().getSignatureData());
            entity.setSignatureTimestamp(returnAggregate.getCustomerSignature().getTimestamp());
        }

        if (returnAggregate.getPrimaryReturnReason() != null) {
            entity.setPrimaryReturnReason(returnAggregate.getPrimaryReturnReason().name());
        }

        entity.setReturnNotes(returnAggregate.getReturnNotes() != null ? returnAggregate.getReturnNotes().getValue() : null);

        entity.getLineItems().clear();
        ReturnEntity newEntity = mapper.toEntity(returnAggregate);
        entity.getLineItems().addAll(newEntity.getLineItems());
        entity.getLineItems().forEach(lineItem -> lineItem.setReturnEntity(entity));
    }

    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated and properly escaped")
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

    private String escapeIdentifier(String identifier) {
        return String.format("\"%s\"", identifier.replace("\"", "\"\""));
    }

    @Override
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "Calling size() to force initialization of lazy-loaded JPA collection")
    public Optional<Return> findByIdAndTenantId(ReturnId id, TenantId tenantId) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying return! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying return");
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

        jakarta.persistence.Query query =
                entityManager.createQuery("SELECT r FROM ReturnEntity r LEFT JOIN FETCH r.lineItems WHERE r.tenantId = :tenantId AND r.returnId = :returnId", ReturnEntity.class);
        query.setParameter("tenantId", tenantId.getValue());
        query.setParameter("returnId", id.getValue());

        @SuppressWarnings("unchecked") List<ReturnEntity> results = query.getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        ReturnEntity entity = results.get(0);
        if (entity.getLineItems() != null) {
            entity.getLineItems().size();
        }

        return Optional.of(mapper.toDomain(entity));
    }

    @Override
    public List<Return> findByStatusAndTenantId(ReturnStatus status, TenantId tenantId) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            throw new IllegalStateException("TenantContext must be set before querying returns");
        }

        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        return jpaRepository.findByTenantIdAndReturnStatus(tenantId.getValue(), status).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Return> findByOrderNumberAndTenantId(OrderNumber orderNumber, TenantId tenantId) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            throw new IllegalStateException("TenantContext must be set before querying returns");
        }

        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        return jpaRepository.findByTenantIdAndOrderNumber(tenantId.getValue(), orderNumber.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}
