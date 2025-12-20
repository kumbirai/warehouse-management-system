package com.ccbsa.wms.notification.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.notification.application.service.exception.NotificationNotFoundException;
import com.ccbsa.wms.notification.application.service.port.repository.NotificationRepository;
import com.ccbsa.wms.notification.application.service.query.dto.GetNotificationQuery;
import com.ccbsa.wms.notification.application.service.query.dto.GetNotificationQueryResult;

/**
 * Query Handler: GetNotificationQueryHandler
 * <p>
 * Handles query for Notification by ID. Uses repository port for MVP (read model can be added later).
 */
@Component
public class GetNotificationQueryHandler {

    private final NotificationRepository repository;

    public GetNotificationQueryHandler(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Handles the GetNotificationQuery.
     * <p>
     * Read-only transaction for query optimization.
     *
     * @param query Query to execute
     * @return Query result
     * @throws NotificationNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public GetNotificationQueryResult handle(GetNotificationQuery query) {
        // 1. Validate query
        validateQuery(query);

        // 2. Load from repository
        return repository.findById(query.getNotificationId()).map(this::toQueryResult)
                .orElseThrow(() -> new NotificationNotFoundException(query.getNotificationId().getValueAsString(), "Notification not found"));
    }

    private void validateQuery(GetNotificationQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.getNotificationId() == null) {
            throw new IllegalArgumentException("NotificationId is required");
        }
    }

    private GetNotificationQueryResult toQueryResult(com.ccbsa.wms.notification.domain.core.entity.Notification notification) {
        return GetNotificationQueryResult.builder().notificationId(notification.getId()).tenantId(notification.getTenantId()).recipientUserId(notification.getRecipientUserId())
                .title(notification.getTitle().getValue()).message(notification.getMessage().getValue()).type(notification.getType()).status(notification.getStatus())
                .createdAt(notification.getCreatedAt()).lastModifiedAt(notification.getLastModifiedAt()).sentAt(notification.getSentAt()).readAt(notification.getReadAt()).build();
    }
}

