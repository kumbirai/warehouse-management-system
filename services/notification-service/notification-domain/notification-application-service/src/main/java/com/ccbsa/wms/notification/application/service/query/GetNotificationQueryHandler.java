package com.ccbsa.wms.notification.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.notification.application.service.exception.NotificationNotFoundException;
import com.ccbsa.wms.notification.application.service.port.data.NotificationViewRepository;
import com.ccbsa.wms.notification.application.service.port.data.dto.NotificationView;
import com.ccbsa.wms.notification.application.service.query.dto.GetNotificationQuery;
import com.ccbsa.wms.notification.application.service.query.dto.GetNotificationQueryResult;

/**
 * Query Handler: GetNotificationQueryHandler
 * <p>
 * Handles query for Notification read model by ID.
 * <p>
 * Uses data port (NotificationViewRepository) instead of repository port for CQRS compliance.
 */
@Component
public class GetNotificationQueryHandler {

    private final NotificationViewRepository viewRepository;

    public GetNotificationQueryHandler(NotificationViewRepository viewRepository) {
        this.viewRepository = viewRepository;
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

        // 2. Load read model (view) from data port
        var notificationView = viewRepository.findById(query.getNotificationId())
                .orElseThrow(() -> new NotificationNotFoundException(query.getNotificationId().getValueAsString(), "Notification not found"));

        // 3. Map view to query result
        return toQueryResult(notificationView);
    }

    private void validateQuery(GetNotificationQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.getNotificationId() == null) {
            throw new IllegalArgumentException("NotificationId is required");
        }
    }

    private GetNotificationQueryResult toQueryResult(NotificationView view) {
        return GetNotificationQueryResult.builder().notificationId(view.getNotificationId()).tenantId(view.getTenantId()).recipientUserId(view.getRecipientUserId())
                .title(view.getTitle()).message(view.getMessage()).type(view.getType()).status(view.getStatus()).createdAt(view.getCreatedAt())
                .lastModifiedAt(view.getLastModifiedAt()).sentAt(view.getSentAt()).readAt(view.getReadAt()).build();
    }
}

