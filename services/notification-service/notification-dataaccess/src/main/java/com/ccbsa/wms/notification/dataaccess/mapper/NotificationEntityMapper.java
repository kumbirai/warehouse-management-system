package com.ccbsa.wms.notification.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.Message;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.Title;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.notification.dataaccess.entity.NotificationEntity;
import com.ccbsa.wms.notification.domain.core.entity.Notification;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;

/**
 * Mapper: NotificationEntityMapper
 * <p>
 * Maps between Notification domain aggregate and NotificationEntity JPA entity.
 * Handles conversion between domain value objects and JPA entity fields.
 */
@Component
public class NotificationEntityMapper {

    /**
     * Converts Notification domain entity to NotificationEntity JPA entity.
     * <p>
     * For new entities (version == 0), version is set to null to let Hibernate manage it.
     * For existing entities (version > 0), version is set to enable optimistic locking.
     *
     * @param notification Notification domain entity
     * @return NotificationEntity JPA entity
     * @throws IllegalArgumentException if notification is null
     */
    public NotificationEntity toEntity(Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification cannot be null");
        }

        NotificationEntity entity = new NotificationEntity();
        entity.setId(notification.getId().getValue());
        entity.setTenantId(notification.getTenantId().getValue());
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

        // For new entities, version will be set by Hibernate when persisting
        // For existing entities loaded from DB, version is already set
        // We only set version when mapping from domain if it's > 0 (existing entity)
        int domainVersion = notification.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }
        // For new entities (version == 0), don't set version - let Hibernate manage it

        return entity;
    }

    /**
     * Converts NotificationEntity JPA entity to Notification domain entity.
     *
     * @param entity NotificationEntity JPA entity
     * @return Notification domain entity
     * @throws IllegalArgumentException if entity is null
     */
    public Notification toDomain(NotificationEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("NotificationEntity cannot be null");
        }

        Notification.Builder builder = Notification.builder()
                .notificationId(NotificationId.of(entity.getId()))
                .tenantId(TenantId.of(entity.getTenantId()))
                .recipientUserId(UserId.of(entity.getRecipientUserId()))
                .title(Title.of(entity.getTitle()))
                .message(Message.of(entity.getMessage()))
                .type(entity.getType())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .lastModifiedAt(entity.getLastModifiedAt())
                .sentAt(entity.getSentAt())
                .readAt(entity.getReadAt())
                .version(entity.getVersion());

        // Set recipient email if available (nullable for backward compatibility)
        if (entity.getRecipientEmail() != null && !entity.getRecipientEmail().isEmpty()) {
            builder.recipientEmail(EmailAddress.of(entity.getRecipientEmail()));
        }

        return builder.buildWithoutEvents();
    }
}

