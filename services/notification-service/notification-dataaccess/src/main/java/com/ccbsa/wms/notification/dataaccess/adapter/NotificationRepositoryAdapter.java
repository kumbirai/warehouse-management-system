package com.ccbsa.wms.notification.dataaccess.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.notification.application.service.port.repository.NotificationRepository;
import com.ccbsa.wms.notification.dataaccess.entity.NotificationEntity;
import com.ccbsa.wms.notification.dataaccess.jpa.NotificationJpaRepository;
import com.ccbsa.wms.notification.dataaccess.mapper.NotificationEntityMapper;
import com.ccbsa.wms.notification.domain.core.entity.Notification;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

/**
 * Repository Adapter: NotificationRepositoryAdapter
 * <p>
 * Implements NotificationRepository port interface. Adapts between domain Notification aggregate and JPA NotificationEntity.
 */
@Repository
public class NotificationRepositoryAdapter
        implements NotificationRepository {
    private final NotificationJpaRepository jpaRepository;
    private final NotificationEntityMapper mapper;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpaRepository, NotificationEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Notification save(Notification notification) {
        // Check if entity already exists to handle version correctly
        Optional<NotificationEntity> existingEntity = jpaRepository.findByTenantIdAndId(notification.getTenantId()
                .getValue(), notification.getId()
                .getValue());

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

        // Domain events are preserved by the command handler before calling save()
        // The command handler gets domain events from the original notification before save()
        // and publishes them after transaction commit. We return the saved notification
        // which may not have events, but that's OK since events are already captured.
        return savedNotification;
    }

    /**
     * Updates an existing entity with values from the domain model. Preserves JPA managed state and version for optimistic locking.
     *
     * @param entity       Existing JPA entity
     * @param notification Domain notification aggregate
     */
    private void updateEntityFromDomain(NotificationEntity entity, Notification notification) {
        entity.setRecipientUserId(notification.getRecipientUserId()
                .getValue());
        entity.setRecipientEmail(notification.getRecipientEmail() != null ? notification.getRecipientEmail()
                .getValue() : null);
        entity.setTitle(notification.getTitle()
                .getValue());
        entity.setMessage(notification.getMessage()
                .getValue());
        entity.setType(notification.getType());
        entity.setStatus(notification.getStatus());
        entity.setCreatedAt(notification.getCreatedAt());
        entity.setLastModifiedAt(notification.getLastModifiedAt());
        entity.setSentAt(notification.getSentAt());
        entity.setReadAt(notification.getReadAt());
        // Version is managed by JPA - don't update it manually
    }

    @Override
    public Optional<Notification> findById(NotificationId notificationId) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set. Cannot find notification without tenant ID.");
        }
        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), notificationId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<Notification> findByRecipientUserId(TenantId tenantId, UserId recipientUserId) {
        return jpaRepository.findByTenantIdAndRecipientUserId(tenantId.getValue(), recipientUserId.getValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findByRecipientUserIdAndStatus(TenantId tenantId, UserId recipientUserId, NotificationStatus status) {
        return jpaRepository.findByTenantIdAndRecipientUserIdAndStatus(tenantId.getValue(), recipientUserId.getValue(), status)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findByType(TenantId tenantId, NotificationType type) {
        return jpaRepository.findByTenantIdAndType(tenantId.getValue(), type)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countUnreadByRecipientUserId(TenantId tenantId, UserId recipientUserId) {
        return jpaRepository.countUnreadByTenantIdAndRecipientUserId(tenantId.getValue(), recipientUserId.getValue());
    }
}

