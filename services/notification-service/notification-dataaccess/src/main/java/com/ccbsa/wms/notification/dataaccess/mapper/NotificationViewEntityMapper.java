package com.ccbsa.wms.notification.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.notification.application.service.port.data.dto.NotificationView;
import com.ccbsa.wms.notification.dataaccess.entity.NotificationViewEntity;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;

/**
 * Entity Mapper: NotificationViewEntityMapper
 * <p>
 * Maps between NotificationViewEntity (JPA) and NotificationView (read model DTO).
 */
@Component
public class NotificationViewEntityMapper {

    /**
     * Converts NotificationViewEntity JPA entity to NotificationView read model DTO.
     *
     * @param entity NotificationViewEntity JPA entity
     * @return NotificationView read model DTO
     * @throws IllegalArgumentException if entity is null
     */
    public NotificationView toView(NotificationViewEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("NotificationViewEntity cannot be null");
        }

        // Build NotificationView
        return NotificationView.builder().notificationId(NotificationId.of(entity.getId())).tenantId(TenantId.of(entity.getTenantId()))
                .recipientUserId(UserId.of(entity.getRecipientUserId())).title(entity.getTitle()).message(entity.getMessage()).type(entity.getType()).status(entity.getStatus())
                .createdAt(entity.getCreatedAt()).lastModifiedAt(entity.getLastModifiedAt()).sentAt(entity.getSentAt()).readAt(entity.getReadAt()).build();
    }
}

