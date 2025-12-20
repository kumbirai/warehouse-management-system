package com.ccbsa.wms.notification.application.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.notification.application.api.dto.NotificationResponse;
import com.ccbsa.wms.notification.application.api.mapper.NotificationMapper;
import com.ccbsa.wms.notification.application.service.query.GetNotificationQueryHandler;
import com.ccbsa.wms.notification.application.service.query.ListNotificationsQueryHandler;
import com.ccbsa.wms.notification.application.service.query.dto.GetNotificationQueryResult;
import com.ccbsa.wms.notification.application.service.query.dto.ListNotificationsQueryResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller: NotificationQueryController
 * <p>
 * Handles notification query operations (read operations).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification Queries", description = "Notification query operations")
public class NotificationQueryController {
    private final GetNotificationQueryHandler getNotificationQueryHandler;
    private final ListNotificationsQueryHandler listNotificationsQueryHandler;
    private final NotificationMapper mapper;

    public NotificationQueryController(GetNotificationQueryHandler getNotificationQueryHandler, ListNotificationsQueryHandler listNotificationsQueryHandler,
                                       NotificationMapper mapper) {
        this.getNotificationQueryHandler = getNotificationQueryHandler;
        this.listNotificationsQueryHandler = listNotificationsQueryHandler;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Notification by ID", description = "Retrieves a notification by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(@PathVariable String id) {
        GetNotificationQueryResult result = getNotificationQueryHandler.handle(mapper.toGetNotificationQuery(id));
        NotificationResponse response = mapper.toNotificationResponse(result);
        return ApiResponseBuilder.ok(response);
    }

    @GetMapping
    @Operation(summary = "List Notifications", description = "Lists notifications for the authenticated user with optional filtering")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listNotifications(@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
                                                                                     @RequestParam(required = false) String recipientUserId,
                                                                                     @RequestParam(required = false) String status, @RequestParam(required = false) String type,
                                                                                     @RequestParam(required = false, defaultValue = "0") Integer page,
                                                                                     @RequestParam(required = false, defaultValue = "20") Integer size) {
        ListNotificationsQueryResult result = listNotificationsQueryHandler.handle(mapper.toListNotificationsQuery(tenantId, recipientUserId, status, type, page, size));
        List<NotificationResponse> responses = mapper.toNotificationResponseList(result.getItems());
        return ApiResponseBuilder.ok(responses);
    }
}

