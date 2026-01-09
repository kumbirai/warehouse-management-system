package com.ccbsa.wms.stock.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.repository.RestockRequestRepository;
import com.ccbsa.wms.stock.dataaccess.entity.RestockRequestEntity;
import com.ccbsa.wms.stock.dataaccess.jpa.RestockRequestJpaRepository;
import com.ccbsa.wms.stock.dataaccess.mapper.RestockRequestEntityMapper;
import com.ccbsa.wms.stock.domain.core.entity.RestockRequest;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestId;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestStatus;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: RestockRequestRepositoryAdapter
 * <p>
 * Implements RestockRequestRepository port interface. Adapts between domain RestockRequest aggregate and JPA RestockRequestEntity.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class RestockRequestRepositoryAdapter implements RestockRequestRepository {
    private final RestockRequestJpaRepository jpaRepository;
    private final RestockRequestEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(RestockRequest restockRequest) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.error("TenantContext is not set when saving restock request! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before saving restock request");
        }

        if (!tenantId.getValue().equals(restockRequest.getTenantId().getValue())) {
            log.error("TenantContext mismatch! Context: {}, RestockRequest: {}", tenantId.getValue(), restockRequest.getTenantId().getValue());
            throw new IllegalStateException("TenantContext tenantId does not match restock request tenantId");
        }

        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        Optional<RestockRequestEntity> existingEntity = jpaRepository.findByTenantIdAndId(restockRequest.getTenantId().getValue(), restockRequest.getId().getValue());

        RestockRequestEntity entity;
        if (existingEntity.isPresent()) {
            entity = existingEntity.get();
            updateEntityFromDomain(entity, restockRequest);
        } else {
            entity = mapper.toEntity(restockRequest);
        }

        jpaRepository.save(entity);
        log.debug("Restock request saved successfully to schema: '{}'", schemaName);
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

    @SuppressFBWarnings(value = "SQL_INJECTION_HIBERNATE", justification = "Schema name is validated before use")
    private void setSearchPath(Session session, String schemaName) {
        session.doWork(connection -> executeSetSearchPath(connection, schemaName));
    }

    private void updateEntityFromDomain(RestockRequestEntity entity, RestockRequest restockRequest) {
        entity.setCurrentQuantity(restockRequest.getCurrentQuantity().getValue());
        entity.setMinimumQuantity(restockRequest.getMinimumQuantity().getValue());
        entity.setMaximumQuantity(restockRequest.getMaximumQuantity() != null ? restockRequest.getMaximumQuantity().getValue() : null);
        entity.setRequestedQuantity(restockRequest.getRequestedQuantity().getValue());
        entity.setPriority(restockRequest.getPriority());
        entity.setStatus(restockRequest.getStatus());
        entity.setSentToD365At(restockRequest.getSentToD365At());
        entity.setD365OrderReference(restockRequest.getD365OrderReference());
    }

    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
            justification = "Schema name validated against expected patterns and escaped")
    private void executeSetSearchPath(Connection connection, String schemaName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(String.format("SET search_path TO %s, public", schemaName));
        }
    }

    @Override
    public Optional<RestockRequest> findById(RestockRequestId restockRequestId, TenantId tenantId) {
        prepareSchema(tenantId);
        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), restockRequestId.getValue()).map(mapper::toDomain);
    }

    private void prepareSchema(TenantId tenantId) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null || !contextTenantId.getValue().equals(tenantId.getValue())) {
            throw new IllegalStateException("TenantContext mismatch");
        }

        String schemaName = schemaResolver.resolveSchema();
        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);
    }

    @Override
    public Optional<RestockRequest> findActiveByProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId) {
        prepareSchema(tenantId);
        List<RestockRequestStatus> activeStatuses = List.of(RestockRequestStatus.PENDING, RestockRequestStatus.SENT_TO_D365);
        UUID locationIdValue = locationId != null ? locationId.getValue() : null;
        return jpaRepository.findByTenantIdAndProductIdAndLocationIdAndStatusIn(tenantId.getValue(), productId.getValue(), locationIdValue, activeStatuses).map(mapper::toDomain);
    }

    @Override
    public List<RestockRequest> findByTenantId(TenantId tenantId, RestockRequestStatus status) {
        prepareSchema(tenantId);
        if (status != null) {
            return jpaRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId.getValue(), status).stream().map(mapper::toDomain).collect(Collectors.toList());
        } else {
            return jpaRepository.findByTenantIdOrderByCreatedAtDesc(tenantId.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList());
        }
    }
}
