package com.ccbsa.wms.notification.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.notification.application.service.port.data.NotificationViewRepository;
import com.ccbsa.wms.notification.application.service.port.data.dto.NotificationView;
import com.ccbsa.wms.notification.application.service.query.dto.GetNotificationQueryResult;
import com.ccbsa.wms.notification.application.service.query.dto.ListNotificationsQuery;
import com.ccbsa.wms.notification.application.service.query.dto.ListNotificationsQueryResult;

/**
 * Query Handler: ListNotificationsQueryHandler
 * <p>
 * Handles query for list of notification read models with filtering.
 * <p>
 * Uses data port (NotificationViewRepository) instead of repository port for CQRS compliance.
 */
@Component
public class ListNotificationsQueryHandler {

    private final NotificationViewRepository viewRepository;

    public ListNotificationsQueryHandler(NotificationViewRepository viewRepository) {
        this.viewRepository = viewRepository;
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

        // 2. Load read models (views) from data port based on filters
        List<NotificationView> notificationViews;
        if (query.getRecipientUserId() != null && query.getStatus() != null) {
            notificationViews = viewRepository.findByRecipientUserIdAndStatus(query.getTenantId(), query.getRecipientUserId(), query.getStatus());
        } else if (query.getRecipientUserId() != null) {
            notificationViews = viewRepository.findByRecipientUserId(query.getTenantId(), query.getRecipientUserId());
        } else if (query.getType() != null) {
            notificationViews = viewRepository.findByType(query.getTenantId(), query.getType());
        } else {
            // For MVP, if no filters, return empty (can be extended later)
            notificationViews = List.of();
        }

        // 3. Map views to query results
        List<GetNotificationQueryResult> results = notificationViews.stream().map(this::toQueryResult).collect(Collectors.toList());

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

    private GetNotificationQueryResult toQueryResult(NotificationView view) {
        return GetNotificationQueryResult.builder().notificationId(view.getNotificationId()).tenantId(view.getTenantId()).recipientUserId(view.getRecipientUserId())
                .title(view.getTitle()).message(view.getMessage()).type(view.getType()).status(view.getStatus()).createdAt(view.getCreatedAt())
                .lastModifiedAt(view.getLastModifiedAt()).sentAt(view.getSentAt()).readAt(view.getReadAt()).build();
    }
}

