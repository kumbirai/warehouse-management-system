package com.ccbsa.wms.notification.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.notification.application.service.port.repository.NotificationRepository;
import com.ccbsa.wms.notification.application.service.query.dto.GetNotificationQueryResult;
import com.ccbsa.wms.notification.application.service.query.dto.ListNotificationsQuery;
import com.ccbsa.wms.notification.application.service.query.dto.ListNotificationsQueryResult;

/**
 * Query Handler: ListNotificationsQueryHandler
 * <p>
 * Handles query for list of notifications with filtering. Uses repository port for MVP (read model can be added later).
 */
@Component
public class ListNotificationsQueryHandler {

    private final NotificationRepository repository;

    public ListNotificationsQueryHandler(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Handles the ListNotificationsQuery.
     * <p>
     * Read-only transaction for query optimization.
     *
     * @param query Query to execute
     * @return Query result
     */
    @Transactional(readOnly = true)
    public ListNotificationsQueryResult handle(ListNotificationsQuery query) {
        // 1. Validate query
        validateQuery(query);

        // 2. Load from repository based on filters
        List<com.ccbsa.wms.notification.domain.core.entity.Notification> notifications;
        if (query.getRecipientUserId() != null && query.getStatus() != null) {
            notifications = repository.findByRecipientUserIdAndStatus(query.getTenantId(), query.getRecipientUserId(), query.getStatus());
        } else if (query.getRecipientUserId() != null) {
            notifications = repository.findByRecipientUserId(query.getTenantId(), query.getRecipientUserId());
        } else if (query.getType() != null) {
            notifications = repository.findByType(query.getTenantId(), query.getType());
        } else {
            // For MVP, if no filters, return empty (can be extended later)
            notifications = List.of();
        }

        // 3. Map to query results
        List<GetNotificationQueryResult> results = notifications.stream().map(this::toQueryResult).collect(Collectors.toList());

        // 4. Return result
        return ListNotificationsQueryResult.builder().items(results).totalCount(results.size()).build();
    }

    private void validateQuery(ListNotificationsQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
    }

    private GetNotificationQueryResult toQueryResult(com.ccbsa.wms.notification.domain.core.entity.Notification notification) {
        return GetNotificationQueryResult.builder().notificationId(notification.getId()).tenantId(notification.getTenantId()).recipientUserId(notification.getRecipientUserId())
                .title(notification.getTitle().getValue()).message(notification.getMessage().getValue()).type(notification.getType()).status(notification.getStatus())
                .createdAt(notification.getCreatedAt()).lastModifiedAt(notification.getLastModifiedAt()).sentAt(notification.getSentAt()).readAt(notification.getReadAt()).build();
    }
}

