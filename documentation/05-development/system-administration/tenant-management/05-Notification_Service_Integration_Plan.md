# Notification Service Integration Plan for Tenant Management

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0
**Date:** 2025-12
**Status:** Draft
**Related Documents:**

- [Tenant Management Implementation Plan](01-Tenant_Management_Implementation_Plan.md)
- [Service Architecture Document](../../../01-architecture/Service_Architecture_Document.md)
- [Event-Driven Architecture Guide](../../../01-architecture/Service_Architecture_Document.md#event-driven-choreography)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Event Subscriptions](#event-subscriptions)
4. [Notification Types](#notification-types)
5. [Implementation Details](#implementation-details)
6. [Template Management](#template-management)
7. [Error Handling](#error-handling)
8. [Testing Strategy](#testing-strategy)

---

## Overview

### Purpose

This document details how the Notification Service integrates with Tenant Management to provide automated notifications for tenant lifecycle events. The integration follows
Event-Driven Choreography principles, ensuring loose coupling between services.

### Scope

The Notification Service consumes tenant domain events and sends appropriate notifications to relevant stakeholders:

- **Tenant Creation** - Notify system admins
- **Tenant Activation** - Notify tenant admins and system admins
- **Tenant Deactivation** - Notify tenant admins
- **Tenant Suspension** - Notify tenant admins with reason
- **Tenant Configuration Updates** - Notify tenant admins

### Key Principles

1. **Event-Driven** - React to domain events via Kafka
2. **Asynchronous** - Non-blocking notification delivery
3. **Resilient** - Retry failed notifications
4. **Template-Based** - Use templates for consistent messaging
5. **Multi-Channel** - Support emailAddress, SMS, in-app notifications (future)

---

## Architecture

### System Architecture

```
Tenant Service
    ↓ Domain Events
Kafka (tenant-events topic)
    ↓ Event Consumption
Notification Service
    ├── Event Listeners
    ├── Notification Handler
    ├── Template Engine
    └── Delivery Service
        ↓ SMTP/SMS/Push
Recipients (Admins, Users)
```

### Component Layers

```
notification-service/
├── notification-domain/
│   ├── notification-domain-core/
│   │   ├── entity/Notification.java
│   │   ├── valueobject/NotificationType.java
│   │   ├── valueobject/NotificationChannel.java
│   │   └── event/NotificationSentEvent.java
│   └── notification-application-service/
│       ├── command/SendNotificationCommandHandler.java
│       ├── port/NotificationTemplatePort.java
│       └── port/NotificationDeliveryPort.java
├── notification-application/
│   └── api/NotificationCommandController.java
├── notification-dataaccess/
│   ├── adapter/NotificationRepositoryAdapter.java
│   └── entity/NotificationEntity.java
├── notification-messaging/
│   ├── listener/TenantEventListener.java
│   ├── adapter/EmailDeliveryAdapter.java
│   └── adapter/TemplateEngineAdapter.java
└── notification-container/
    └── NotificationServiceApplication.java
```

---

## Event Subscriptions

### Kafka Topic: tenant-events

The Notification Service subscribes to the `tenant-events` Kafka topic and handles the following event types:

#### 1. TenantCreatedEvent

**Event Payload:**

```json
{
  "eventId": "evt-123",
  "eventType": "TenantCreatedEvent",
  "aggregateId": "ldp-001",
  "aggregateType": "Tenant",
  "timestamp": "2025-12-04T10:00:00Z",
  "version": 1,
  "payload": {
    "tenantId": "ldp-001",
    "name": "Local Distribution Partner 001",
    "status": "PENDING"
  }
}
```

**Notification Action:**

- Send notification to SYSTEM_ADMIN users
- Subject: "New Tenant Created: {tenantName}"
- Template: `tenant-created-notification`

#### 2. TenantActivatedEvent

**Event Payload:**

```json
{
  "eventId": "evt-124",
  "eventType": "TenantActivatedEvent",
  "aggregateId": "ldp-001",
  "aggregateType": "Tenant",
  "timestamp": "2025-12-04T10:30:00Z",
  "version": 2,
  "payload": {
    "tenantId": "ldp-001",
    "activatedAt": "2025-12-04T10:30:00Z"
  }
}
```

**Notification Action:**

- Send notification to tenant admins (users with TENANT_ADMIN role in tenant group)
- Send notification to SYSTEM_ADMIN users
- Subject: "Tenant Activated: {tenantName}"
- Template: `tenant-activated-notification`

#### 3. TenantDeactivatedEvent

**Event Payload:**

```json
{
  "eventId": "evt-125",
  "eventType": "TenantDeactivatedEvent",
  "aggregateId": "ldp-001",
  "aggregateType": "Tenant",
  "timestamp": "2025-12-04T11:00:00Z",
  "version": 3,
  "payload": {
    "tenantId": "ldp-001",
    "deactivatedAt": "2025-12-04T11:00:00Z"
  }
}
```

**Notification Action:**

- Send notification to tenant admins
- Send notification to SYSTEM_ADMIN users
- Subject: "Tenant Deactivated: {tenantName}"
- Template: `tenant-deactivated-notification`

#### 4. TenantSuspendedEvent

**Event Payload:**

```json
{
  "eventId": "evt-126",
  "eventType": "TenantSuspendedEvent",
  "aggregateId": "ldp-001",
  "aggregateType": "Tenant",
  "timestamp": "2025-12-04T11:30:00Z",
  "version": 4,
  "payload": {
    "tenantId": "ldp-001",
    "suspendedAt": "2025-12-04T11:30:00Z",
    "reason": "Payment overdue"
  }
}
```

**Notification Action:**

- Send notification to tenant admins
- Send notification to SYSTEM_ADMIN users
- Subject: "Tenant Suspended: {tenantName}"
- Template: `tenant-suspended-notification`

#### 5. TenantConfigurationUpdatedEvent

**Event Payload:**

```json
{
  "eventId": "evt-127",
  "eventType": "TenantConfigurationUpdatedEvent",
  "aggregateId": "ldp-001",
  "aggregateType": "Tenant",
  "timestamp": "2025-12-04T12:00:00Z",
  "version": 5,
  "payload": {
    "tenantId": "ldp-001",
    "configuration": {
      "usePerTenantRealm": false,
      "keycloakRealmName": null
    }
  }
}
```

**Notification Action:**

- Send notification to tenant admins
- Subject: "Tenant Configuration Updated: {tenantName}"
- Template: `tenant-configuration-updated-notification`

---

## Notification Types

### Email Notifications

All tenant lifecycle notifications are sent via emailAddress by default.

**Email Configuration:**

- SMTP server configured in `application.yml`
- From address: `noreply@wms.ccbsa.com`
- Reply-to: `support@wms.ccbsa.com`

**Email Template Structure:**

- HTML template with CSS styling
- Plain text alternative
- Consistent branding (logo, colors)
- Actionable links (e.g., "View Tenant", "Login")

### In-App Notifications (Future)

- Notification bell icon in header
- Real-time updates via WebSocket
- Notification history page

### SMS Notifications (Future)

- Critical events only (e.g., tenant suspension)
- SMS gateway integration (Twilio, AWS SNS)

---

## Implementation Details

### Event Listener

```java
// TenantEventListener.java (notification-messaging)
@Component
@Slf4j
public class TenantEventListener {
    private final SendNotificationCommandHandler notificationHandler;
    private final TenantNotificationService tenantNotificationService;

    @KafkaListener(topics = "tenant-events", groupId = "notification-service")
    public void handleTenantEvent(@Payload String eventJson, @Header("eventType") String eventType) {
        log.info("Received tenant event: {}", eventType);

        try {
            switch (eventType) {
                case "TenantCreatedEvent":
                    handleTenantCreated(parseEvent(eventJson, TenantCreatedEvent.class));
                    break;
                case "TenantActivatedEvent":
                    handleTenantActivated(parseEvent(eventJson, TenantActivatedEvent.class));
                    break;
                case "TenantDeactivatedEvent":
                    handleTenantDeactivated(parseEvent(eventJson, TenantDeactivatedEvent.class));
                    break;
                case "TenantSuspendedEvent":
                    handleTenantSuspended(parseEvent(eventJson, TenantSuspendedEvent.class));
                    break;
                case "TenantConfigurationUpdatedEvent":
                    handleTenantConfigurationUpdated(parseEvent(eventJson, TenantConfigurationUpdatedEvent.class));
                    break;
                default:
                    log.warn("Unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process tenant event: {}", eventType, e);
            // Event will be retried based on Kafka consumer configuration
            throw new EventProcessingException("Failed to process event: " + eventType, e);
        }
    }

    private void handleTenantCreated(TenantCreatedEvent event) {
        // Get system admin recipients
        List<String> recipients = tenantNotificationService.getSystemAdminEmails();

        // Send notification
        SendNotificationCommand command = SendNotificationCommand.builder()
            .notificationType(NotificationType.TENANT_CREATED)
            .recipients(recipients)
            .templateName("tenant-created-notification")
            .templateData(Map.of(
                "tenantId", event.getPayload().getTenantId(),
                "tenantName", event.getPayload().getName(),
                "status", event.getPayload().getStatus(),
                "createdAt", event.getTimestamp()
            ))
            .build();

        notificationHandler.handle(command);
    }

    private void handleTenantActivated(TenantActivatedEvent event) {
        // Get tenant admin and system admin recipients
        List<String> recipients = new ArrayList<>();
        recipients.addAll(tenantNotificationService.getTenantAdminEmails(event.getPayload().getTenantId()));
        recipients.addAll(tenantNotificationService.getSystemAdminEmails());

        // Send notification
        SendNotificationCommand command = SendNotificationCommand.builder()
            .notificationType(NotificationType.TENANT_ACTIVATED)
            .recipients(recipients)
            .templateName("tenant-activated-notification")
            .templateData(Map.of(
                "tenantId", event.getPayload().getTenantId(),
                "tenantName", getTenantName(event.getPayload().getTenantId()),
                "activatedAt", event.getPayload().getActivatedAt()
            ))
            .build();

        notificationHandler.handle(command);
    }

    // Similar methods for other events...

    private <T> T parseEvent(String eventJson, Class<T> eventClass) {
        // Parse JSON to event object
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(eventJson, eventClass);
    }

    private String getTenantName(String tenantId) {
        // Call Tenant Service to get tenant name
        // Or maintain a cache of tenant names
        return tenantNotificationService.getTenantName(tenantId);
    }
}
```

### Notification Command Handler

```java
// SendNotificationCommandHandler.java (notification-application-service)
@Component
@Transactional
public class SendNotificationCommandHandler {
    private final NotificationRepository notificationRepository;
    private final NotificationTemplatePort templatePort;
    private final NotificationDeliveryPort deliveryPort;
    private final NotificationEventPublisher eventPublisher;

    public NotificationResult handle(SendNotificationCommand command) {
        // Generate notification ID
        NotificationId notificationId = NotificationId.of(UUID.randomUUID().toString());

        // Render template
        String subject = templatePort.renderSubject(
            command.getTemplateName(),
            command.getTemplateData()
        );
        String body = templatePort.renderBody(
            command.getTemplateName(),
            command.getTemplateData()
        );

        // Create notification entity
        Notification notification = Notification.builder()
            .notificationId(notificationId)
            .type(command.getNotificationType())
            .channel(NotificationChannel.EMAIL)
            .recipients(command.getRecipients())
            .subject(subject)
            .body(body)
            .status(NotificationStatus.PENDING)
            .build();

        // Persist
        notificationRepository.save(notification);

        // Send notification
        try {
            deliveryPort.sendEmail(
                command.getRecipients(),
                subject,
                body
            );

            notification.markAsSent();
            notificationRepository.save(notification);

            // Publish event
            eventPublisher.publish(notification.getDomainEvents());
            notification.clearDomainEvents();

        } catch (NotificationDeliveryException e) {
            notification.markAsFailed(e.getMessage());
            notificationRepository.save(notification);
            throw e;
        }

        return new NotificationResult(notificationId.getValue(), true, "Notification sent successfully");
    }
}
```

---

## Template Management

### Template Structure

Templates are stored in `notification-service/src/main/resources/templates/`

**Email Template Example: tenant-activated-notification.html**

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #007bff; color: white; padding: 20px; text-align: center; }
        .content { padding: 20px; background-color: #f9f9f9; }
        .button { background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; display: inline-block; margin: 10px 0; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Tenant Activated</h1>
        </div>
        <div class="content">
            <p>Hello,</p>
            <p>The tenant <strong>${tenantName}</strong> (ID: ${tenantId}) has been activated.</p>
            <p><strong>Activation Date:</strong> ${activatedAt}</p>
            <p>You can now create users and begin using the warehouse management system.</p>
            <a href="${loginUrl}" class="button">Login to System</a>
            <p>If you have any questions, please contact our support team.</p>
            <p>Best regards,<br>WMS Team</p>
        </div>
    </div>
</body>
</html>
```

### Template Variables

Each template supports the following variables:

- `tenantId` - Tenant identifier
- `tenantName` - Tenant name
- `timestamp` - Event timestamp
- `loginUrl` - Login URL
- `supportEmail` - Support emailAddress address
- Event-specific variables (e.g., `activatedAt`, `suspendedReason`)

---

## Error Handling

### Retry Strategy

1. **Kafka Consumer Retry**
    - Kafka consumer retries failed events
    - Retry delay: 1s, 5s, 30s, 5m
    - Max retries: 5

2. **SMTP Retry**
    - Retry failed SMTP sends
    - Exponential backoff: 1s, 2s, 4s, 8s, 16s
    - Max retries: 5

3. **Dead Letter Queue**
    - Failed events sent to `tenant-events-dlq`
    - Manual review and retry

### Error Scenarios

1. **Template Not Found**
    - Log error
    - Send generic notification
    - Alert admin

2. **SMTP Connection Failed**
    - Retry with backoff
    - Mark notification as failed after max retries
    - Store for manual retry

3. **Invalid Recipients**
    - Log error
    - Skip invalid recipients
    - Send to valid recipients

4. **Event Parsing Failed**
    - Log error with full event payload
    - Send to dead letter queue
    - Alert admin

---

## Testing Strategy

### Unit Tests

1. **Event Listener Tests**
    - Test event parsing
    - Test event routing
    - Test error handling

2. **Command Handler Tests**
    - Test notification creation
    - Test template rendering
    - Test delivery

3. **Template Tests**
    - Test template rendering
    - Test variable substitution
    - Test HTML/plain text generation

### Integration Tests

1. **Kafka Integration**
    - Publish test events
    - Verify consumption
    - Verify notification creation

2. **SMTP Integration**
    - Test emailAddress sending
    - Verify emailAddress content
    - Test retry logic

3. **Template Rendering**
    - Test with real templates
    - Verify output format
    - Test with various data

### E2E Tests

1. **Tenant Creation Flow**
    - Create tenant
    - Verify notification sent
    - Verify recipient received emailAddress

2. **Tenant Activation Flow**
    - Activate tenant
    - Verify notifications sent to multiple recipients
    - Verify emailAddress content

3. **Error Scenarios**
    - Test SMTP failure
    - Test template missing
    - Verify retry behavior

---

## Configuration

### Application Configuration

```yaml
# application.yml
spring:
  kafka:
    consumer:
      group-id: notification-service
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        max.poll.records: 10
        session.timeout.ms: 30000

  mail:
    host: ${SMTP_HOST:smtp.gmail.com}
    port: ${SMTP_PORT:587}
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

notification:
  from-emailAddress: noreply@wms.ccbsa.com
  reply-to-emailAddress: support@wms.ccbsa.com
  base-url: ${APP_BASE_URL:http://localhost:3000}

  templates:
    base-path: classpath:/templates/

  retry:
    max-attempts: 5
    initial-interval: 1000
    multiplier: 2.0
    max-interval: 300000
```

---

## References

- [Tenant Management Implementation Plan](01-Tenant_Management_Implementation_Plan.md)
- [Service Architecture Document](../../../01-architecture/Service_Architecture_Document.md)
- [Event-Driven Architecture Guide](../../../01-architecture/Service_Architecture_Document.md#event-driven-choreography)
- [Mandated Implementation Template Guide](../../../guide/mandated-Implementation-template-guide.md)

---

**Document Status:** Draft
**Last Updated:** 2025-12
**Next Review:** 2026-01
