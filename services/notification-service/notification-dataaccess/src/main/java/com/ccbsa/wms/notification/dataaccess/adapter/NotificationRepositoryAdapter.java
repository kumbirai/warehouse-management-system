package com.ccbsa.wms.notification.dataaccess.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.common.dataaccess.TenantSchemaResolver;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.notification.application.service.port.repository.NotificationRepository;
import com.ccbsa.wms.notification.dataaccess.entity.NotificationEntity;
import com.ccbsa.wms.notification.dataaccess.jpa.NotificationJpaRepository;
import com.ccbsa.wms.notification.dataaccess.mapper.NotificationEntityMapper;
import com.ccbsa.wms.notification.dataaccess.schema.TenantSchemaProvisioner;
import com.ccbsa.wms.notification.domain.core.entity.Notification;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Repository Adapter: NotificationRepositoryAdapter
 * <p>
 * Implements NotificationRepository port interface. Adapts between domain Notification aggregate and JPA NotificationEntity.
 */
@Repository
public class NotificationRepositoryAdapter implements NotificationRepository {
    private static final Logger logger = LoggerFactory.getLogger(NotificationRepositoryAdapter.class);

    private final NotificationJpaRepository jpaRepository;
    private final NotificationEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpaRepository, NotificationEntityMapper mapper, TenantSchemaResolver schemaResolver,
                                         TenantSchemaProvisioner schemaProvisioner) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.schemaResolver = schemaResolver;
        this.schemaProvisioner = schemaProvisioner;
    }

    @Override
    public Notification save(Notification notification) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            logger.error("TenantContext is not set when saving notification! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before saving notification");
        }

        // Verify tenantId matches
        if (!tenantId.getValue().equals(notification.getTenantId().getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Notification: {}", tenantId.getValue(), notification.getTenantId().getValue());
            throw new IllegalStateException("TenantContext tenantId does not match notification tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Check if entity already exists to handle version correctly
        Optional<NotificationEntity> existingEntity = jpaRepository.findByTenantIdAndId(notification.getTenantId().getValue(), notification.getId().getValue());

        NotificationEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            updateEntityFromDomain(entity, notification);
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity(notification);
        }

        NotificationEntity savedEntity = jpaRepository.save(entity);
        Notification savedNotification = mapper.toDomain(savedEntity);
        logger.debug("Notification saved successfully to schema: '{}'", schemaName);

        // Domain events are preserved by the command handler before calling save()
        // The command handler gets domain events from the original notification before save()
        // and publishes them after transaction commit. We return the saved notification
        // which may not have events, but that's OK since events are already captured.
        return savedNotification;
    }

    /**
     * Validates that a schema name matches expected patterns to prevent SQL injection.
     * <p>
     * Schema names must match one of these patterns:
     * - 'public' (for legacy data)
     * - 'tenant_*_schema' (for tenant-specific schemas)
     *
     * @param schemaName The schema name to validate
     * @throws IllegalArgumentException if schema name does not match expected patterns
     */
    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }

        // Allow 'public' schema
        if ("public".equals(schemaName)) {
            return;
        }

        // Allow tenant schemas matching pattern: tenant_*_schema
        if (schemaName.matches("^tenant_[a-zA-Z0-9_]+_schema$")) {
            return;
        }

        throw new IllegalArgumentException(String.format("Invalid schema name format: '%s'. Expected 'public' or 'tenant_*_schema' pattern", schemaName));
    }

    /**
     * Sets the PostgreSQL search_path for the current database connection.
     * <p>
     * This method sets the search_path to the specified schema name.
     * The schema name must be validated before calling this method.
     *
     * @param session    Hibernate session
     * @param schemaName Validated schema name
     */
    private void setSearchPath(Session session, String schemaName) {
        session.doWork(connection -> executeSetSearchPath(connection, schemaName));
    }

    /**
     * Updates an existing entity with values from the domain model. Preserves JPA managed state and version for optimistic locking.
     *
     * @param entity       Existing JPA entity
     * @param notification Domain notification aggregate
     */
    private void updateEntityFromDomain(NotificationEntity entity, Notification notification) {
        entity.setRecipientUserId(notification.getRecipientUserId().getValue());
        entity.setRecipientEmail(notification.getRecipientEmail() != null ? notification.getRecipientEmail().getValue() : null);
        entity.setTitle(notification.getTitle().getValue());
        entity.setMessage(notification.getMessage().getValue());
        entity.setType(notification.getType());
        entity.setStatus(notification.getStatus());
        entity.setCreatedAt(notification.getCreatedAt());
        entity.setLastModifiedAt(notification.getLastModifiedAt());
        entity.setSentAt(notification.getSentAt());
        entity.setReadAt(notification.getReadAt());
        // Version is managed by JPA - don't update it manually
    }

    /**
     * Executes the SET search_path SQL command.
     * <p>
     * This method is separated to allow proper SpotBugs annotation placement.
     *
     * @param connection Database connection
     * @param schemaName Validated and escaped schema name
     */
    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Schema name is validated against expected patterns (tenant_*_schema or public) "
            + "and properly escaped using escapeIdentifier() method. " + "PostgreSQL SET search_path command does not support parameterized queries.")
    private void executeSetSearchPath(Connection connection, String schemaName) {
        try (Statement stmt = connection.createStatement()) {
            String setSchemaSql = String.format("SET search_path TO %s", escapeIdentifier(schemaName));
            logger.debug("Setting search_path to: {}", schemaName);
            stmt.execute(setSchemaSql);
        } catch (SQLException e) {
            logger.error("Failed to set search_path to schema '{}': {}", schemaName, e.getMessage(), e);
            throw new RuntimeException("Failed to set database schema", e);
        }
    }

    /**
     * Escapes a PostgreSQL identifier to prevent SQL injection.
     * <p>
     * PostgreSQL identifiers are case-sensitive when quoted. This method wraps
     * the identifier in double quotes and escapes any internal quotes.
     *
     * @param identifier The identifier to escape
     * @return Escaped identifier safe for use in SQL
     */
    private String escapeIdentifier(String identifier) {
        // PostgreSQL identifiers: wrap in double quotes and escape internal quotes
        return String.format("\"%s\"", identifier.replace("\"", "\"\""));
    }

    @Override
    public Optional<Notification> findById(NotificationId notificationId) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            logger.error("TenantContext is not set when querying notification! Cannot resolve schema.");
            throw new IllegalStateException("Tenant context not set. Cannot find notification without tenant ID.");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, tenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), notificationId.getValue()).map(mapper::toDomain);
    }

    @Override
    public List<Notification> findByRecipientUserId(TenantId tenantId, UserId recipientUserId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying notifications by recipient! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying notifications by recipient");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.findByTenantIdAndRecipientUserId(tenantId.getValue(), recipientUserId.getValue()).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Notification> findByRecipientUserIdAndStatus(TenantId tenantId, UserId recipientUserId, NotificationStatus status) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying notifications by recipient and status! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying notifications by recipient and status");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.findByTenantIdAndRecipientUserIdAndStatus(tenantId.getValue(), recipientUserId.getValue(), status).stream().map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findByType(TenantId tenantId, NotificationType type) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when querying notifications by type! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before querying notifications by type");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.findByTenantIdAndType(tenantId.getValue(), type).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countUnreadByRecipientUserId(TenantId tenantId, UserId recipientUserId) {
        // Verify TenantContext is set (critical for schema resolution)
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            logger.error("TenantContext is not set when counting unread notifications! Cannot resolve schema.");
            throw new IllegalStateException("TenantContext must be set before counting unread notifications");
        }

        // Verify tenantId matches TenantContext
        if (!contextTenantId.getValue().equals(tenantId.getValue())) {
            logger.error("TenantContext mismatch! Context: {}, Requested: {}", contextTenantId.getValue(), tenantId.getValue());
            throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
        }

        // Get the actual schema name from TenantSchemaResolver
        String schemaName = schemaResolver.resolveSchema();
        logger.debug("Resolved schema name: '{}' for tenantId: '{}'", schemaName, contextTenantId.getValue());

        // On-demand safety: ensure schema exists and migrations are applied
        schemaProvisioner.ensureSchemaReady(schemaName);

        // Validate schema name format before use
        validateSchemaName(schemaName);

        // Set the search_path explicitly on the database connection
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // Now query using JPA repository (will use the schema set in search_path)
        return jpaRepository.countUnreadByTenantIdAndRecipientUserId(tenantId.getValue(), recipientUserId.getValue());
    }
}

