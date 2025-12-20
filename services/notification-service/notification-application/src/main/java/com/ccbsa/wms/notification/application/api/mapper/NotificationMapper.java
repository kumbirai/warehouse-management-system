package com.ccbsa.wms.notification.application.api.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.notification.application.api.dto.NotificationResponse;
import com.ccbsa.wms.notification.application.service.query.dto.GetNotificationQuery;
import com.ccbsa.wms.notification.application.service.query.dto.GetNotificationQueryResult;
import com.ccbsa.wms.notification.application.service.query.dto.ListNotificationsQuery;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

/**
 * Mapper: NotificationMapper
 * <p>
 * Maps between API DTOs and application service commands/queries.
 */
@Component
public class NotificationMapper {

    /**
     * Converts list of GetNotificationQueryResult to list of NotificationResponse.
     *
     * @param results Query results
     * @return List of notification responses
     */
    public List<NotificationResponse> toNotificationResponseList(List<GetNotificationQueryResult> results) {
        return results.stream().map(this::toNotificationResponse).collect(Collectors.toList());
    }

    /**
     * Converts GetNotificationQueryResult to NotificationResponse.
     *
     * @param result Query result
     * @return Notification response
     */
    public NotificationResponse toNotificationResponse(GetNotificationQueryResult result) {
        return new NotificationResponse(result.getNotificationId().getValueAsString(), result.getTenantId().getValue(), result.getRecipientUserId().getValue(), result.getTitle(),
                result.getMessage(), result.getType(), result.getStatus(), result.getCreatedAt(), result.getLastModifiedAt(), result.getSentAt(), result.getReadAt());
    }

    /**
     * Converts notification ID string to GetNotificationQuery.
     *
     * @param notificationId Notification ID string
     * @return GetNotificationQuery
     */
    public GetNotificationQuery toGetNotificationQuery(String notificationId) {
        return new GetNotificationQuery(NotificationId.of(notificationId));
    }

    /**
     * Converts parameters to ListNotificationsQuery.
     *
     * @param tenantId        Tenant ID string
     * @param recipientUserId Recipient user ID string (optional)
     * @param status          Status string (optional)
     * @param type            Type string (optional)
     * @param page            Page number (optional)
     * @param size            Page size (optional)
     * @return ListNotificationsQuery
     */
    public ListNotificationsQuery toListNotificationsQuery(String tenantId, String recipientUserId, String status, String type, Integer page, Integer size) {
        ListNotificationsQuery.Builder builder = ListNotificationsQuery.builder().tenantId(TenantId.of(tenantId));

        if (recipientUserId != null) {
            builder.recipientUserId(UserId.of(recipientUserId));
        }
        if (status != null) {
            builder.status(NotificationStatus.valueOf(status));
        }
        if (type != null) {
            builder.type(NotificationType.valueOf(type));
        }
        if (page != null) {
            builder.page(page);
        }
        if (size != null) {
            builder.size(size);
        }

        return builder.build();
    }
}

