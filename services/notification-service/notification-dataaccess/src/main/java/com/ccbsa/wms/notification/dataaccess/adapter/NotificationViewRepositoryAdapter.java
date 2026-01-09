package com.ccbsa.wms.notification.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.notification.application.service.port.data.NotificationViewRepository;
import com.ccbsa.wms.notification.application.service.port.data.dto.NotificationView;
import com.ccbsa.wms.notification.dataaccess.entity.NotificationViewEntity;
import com.ccbsa.wms.notification.dataaccess.jpa.NotificationViewJpaRepository;
import com.ccbsa.wms.notification.dataaccess.mapper.NotificationViewEntityMapper;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository Adapter: NotificationViewRepositoryAdapter
 * <p>
 * Implements NotificationViewRepository data port interface. Provides read model access to notification data.
 * <p>
 * This adapter handles tenant schema resolution and search_path setting for multi-tenant isolation.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class NotificationViewRepositoryAdapter implements NotificationViewRepository {
    private final NotificationViewJpaRepository jpaRepository;
    private final NotificationViewEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<NotificationView> findById(NotificationId notificationId) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying notification view!");
            throw new IllegalStateException("TenantContext must be set before querying notification view");
        }

        // Resolve schema and set search_path
        String schemaName = schemaResolver.resolveSchema();
        log.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        schemaProvisioner.ensureSchemaReady(schemaName);
        validateSchemaName(schemaName);

        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Query view entity - need tenantId for the query
        Optional<NotificationViewEntity> entity = jpaRepository.findByTenantIdAndId(contextTenantId.getValue(), notificationId.getValue());
        return entity.map(mapper::toView);
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
    public List<NotificationView> findByRecipientUserId(TenantId tenantId, UserId recipientUserId) {
        return executeInTenantSchema(tenantId, () -> {
            List<NotificationViewEntity> entities = jpaRepository.findByTenantIdAndRecipientUserId(tenantId.getValue(), recipientUserId.getValue());
            return entities.stream().map(mapper::toView).collect(Collectors.toList());
        });
    }

    private <R> R executeInTenantSchema(TenantId tenantId, java.util.function.Supplier<R> action) {
        // Verify TenantContext is set
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.error("TenantContext is not set when querying notification view!");
            throw new IllegalStateException("TenantContext must be set before querying notification view");
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

    @Override
    public List<NotificationView> findByRecipientUserIdAndStatus(TenantId tenantId, UserId recipientUserId, NotificationStatus status) {
        return executeInTenantSchema(tenantId, () -> {
            List<NotificationViewEntity> entities = jpaRepository.findByTenantIdAndRecipientUserIdAndStatus(tenantId.getValue(), recipientUserId.getValue(), status);
            return entities.stream().map(mapper::toView).collect(Collectors.toList());
        });
    }

    @Override
    public List<NotificationView> findByType(TenantId tenantId, NotificationType type) {
        return executeInTenantSchema(tenantId, () -> {
            List<NotificationViewEntity> entities = jpaRepository.findByTenantIdAndType(tenantId.getValue(), type);
            return entities.stream().map(mapper::toView).collect(Collectors.toList());
        });
    }
}

