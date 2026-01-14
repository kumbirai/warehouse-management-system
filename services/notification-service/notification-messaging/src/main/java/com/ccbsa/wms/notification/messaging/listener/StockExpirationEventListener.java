package com.ccbsa.wms.notification.messaging.listener;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.valueobject.Message;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.Title;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.notification.application.service.command.CreateNotificationCommandHandler;
import com.ccbsa.wms.notification.application.service.command.dto.CreateNotificationCommand;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Listener: StockExpirationEventListener
 * <p>
 * Listens to StockExpiringAlertEvent and StockExpiredEvent from Stock Management Service and creates notifications.
 * <p>
 * This listener implements event-driven choreography:
 * - Stock expiration alerts trigger notifications for warehouse managers
 * - Stock expired events trigger critical notifications
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StockExpirationEventListener {
    private static final String STOCK_EXPIRING_ALERT_EVENT = "StockExpiringAlertEvent";
    private static final String STOCK_EXPIRED_EVENT = "StockExpiredEvent";

    private final CreateNotificationCommandHandler createNotificationCommandHandler;

    @KafkaListener(topics = "stock-management-events", groupId = "notification-service-stock-expiration", containerFactory = "externalEventKafkaListenerContainerFactory")
    public void handle(@Payload Map<String, Object> eventData, @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        boolean shouldAcknowledge = false;
        try {
            extractAndSetCorrelationId(eventData);

            String detectedEventType = detectEventType(eventData, eventType);
            if (!isStockExpirationEvent(detectedEventType, eventData)) {
                log.debug("Skipping event - not StockExpiringAlertEvent or StockExpiredEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                shouldAcknowledge = true;
                return;
            }

            TenantId tenantId = extractTenantId(eventData);
            TenantContext.setTenantId(tenantId);

            if (STOCK_EXPIRING_ALERT_EVENT.equals(detectedEventType) || isStockExpiringAlertEvent(detectedEventType, eventData)) {
                log.info("Received StockExpiringAlertEvent: tenantId={}", tenantId.getValue());
                shouldAcknowledge = processStockExpiringAlertEvent(eventData, tenantId);
            } else if (STOCK_EXPIRED_EVENT.equals(detectedEventType) || isStockExpiredEvent(detectedEventType, eventData)) {
                log.info("Received StockExpiredEvent: tenantId={}", tenantId.getValue());
                shouldAcknowledge = processStockExpiredEvent(eventData, tenantId);
            }

        } catch (Exception e) {
            log.error("Error processing stock expiration event: eventId={}, error={}", extractEventId(eventData), e.getMessage(), e);
            shouldAcknowledge = true;
        } finally {
            TenantContext.clear();
            CorrelationContext.clear();
            if (shouldAcknowledge) {
                acknowledgment.acknowledge();
            }
        }
    }

    private void extractAndSetCorrelationId(Map<String, Object> eventData) {
        @SuppressWarnings("unchecked") Map<String, Object> metadata = (Map<String, Object>) eventData.get("metadata");
        if (metadata != null) {
            Object correlationIdObj = metadata.get("correlationId");
            if (correlationIdObj != null) {
                CorrelationContext.setCorrelationId(correlationIdObj.toString());
            }
        }
    }

    private String detectEventType(Map<String, Object> eventData, String headerType) {
        if (headerType != null && !headerType.isEmpty()) {
            return headerType;
        }
        Object classObj = eventData.get("@class");
        if (classObj != null) {
            String className = classObj.toString();
            int lastDot = className.lastIndexOf('.');
            return lastDot >= 0 ? className.substring(lastDot + 1) : className;
        }
        return "Unknown";
    }

    private boolean isStockExpirationEvent(String detectedEventType, Map<String, Object> eventData) {
        return isStockExpiringAlertEvent(detectedEventType, eventData) || isStockExpiredEvent(detectedEventType, eventData);
    }

    private TenantId extractTenantId(Map<String, Object> eventData) {
        Object tenantIdObj = eventData.get("tenantId");
        if (tenantIdObj == null) {
            @SuppressWarnings("unchecked") Map<String, Object> metadata = (Map<String, Object>) eventData.get("metadata");
            if (metadata != null) {
                tenantIdObj = metadata.get("tenantId");
            }
        }
        if (tenantIdObj == null) {
            throw new IllegalArgumentException("TenantId not found in event data or metadata");
        }
        if (tenantIdObj instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> tenantIdMap = (Map<String, Object>) tenantIdObj;
            Object value = tenantIdMap.get("value");
            if (value != null) {
                return TenantId.of(value.toString());
            }
        }
        return TenantId.of(tenantIdObj.toString());
    }

    private boolean isStockExpiringAlertEvent(String detectedEventType, Map<String, Object> eventData) {
        if (STOCK_EXPIRING_ALERT_EVENT.equals(detectedEventType)) {
            return true;
        }
        Object eventClass = eventData.get("@class");
        return eventClass != null && eventClass.toString().contains(STOCK_EXPIRING_ALERT_EVENT);
    }

    private boolean processStockExpiringAlertEvent(Map<String, Object> eventData, TenantId tenantId) {
        try {
            String stockItemId = extractStockItemId(eventData);
            String productId = extractProductId(eventData);
            LocalDate expirationDate = extractExpirationDate(eventData);
            Integer daysUntilExpiry = extractDaysUntilExpiry(eventData);

            String message = String.format("Stock item %s (Product: %s) is expiring soon. Expiration date: %s, Days until expiry: %d. Please review and take appropriate action.",
                    stockItemId, productId, expirationDate, daysUntilExpiry != null ? daysUntilExpiry : 0);

            // Create notification for warehouse managers
            // Note: For now, we'll create a system notification without specific recipient
            // In production, this would query for users with WAREHOUSE_MANAGER or STOCK_MANAGER roles
            CreateNotificationCommand command =
                    CreateNotificationCommand.builder().tenantId(tenantId).recipientUserId(UserId.of("system")) // System notification - can be enhanced to query for managers
                            .title(Title.of("Stock Expiring Alert")).message(Message.of(message)).type(NotificationType.STOCK_EXPIRING).build();

            createNotificationCommandHandler.handle(command);

            log.info("Created stock expiring alert notification: stockItemId={}, productId={}, expirationDate={}", stockItemId, productId, expirationDate);
            return true;
        } catch (Exception e) {
            log.error("Error creating stock expiring alert notification: error={}", e.getMessage(), e);
            return false;
        }
    }

    private boolean isStockExpiredEvent(String detectedEventType, Map<String, Object> eventData) {
        if (STOCK_EXPIRED_EVENT.equals(detectedEventType)) {
            return true;
        }
        Object eventClass = eventData.get("@class");
        return eventClass != null && eventClass.toString().contains(STOCK_EXPIRED_EVENT);
    }

    private boolean processStockExpiredEvent(Map<String, Object> eventData, TenantId tenantId) {
        try {
            String stockItemId = extractStockItemId(eventData);
            String productId = extractProductId(eventData);
            LocalDate expirationDate = extractExpirationDate(eventData);

            String message =
                    String.format("Stock item %s (Product: %s) has expired. Expiration date: %s. Please remove from inventory and take appropriate action.", stockItemId, productId,
                            expirationDate);

            // Create critical notification for warehouse managers
            CreateNotificationCommand command =
                    CreateNotificationCommand.builder().tenantId(tenantId).recipientUserId(UserId.of("system")) // System notification - can be enhanced to query for managers
                            .title(Title.of("Stock Expired - Action Required")).message(Message.of(message)).type(NotificationType.STOCK_EXPIRED).build();

            createNotificationCommandHandler.handle(command);

            log.info("Created stock expired notification: stockItemId={}, productId={}, expirationDate={}", stockItemId, productId, expirationDate);
            return true;
        } catch (Exception e) {
            log.error("Error creating stock expired notification: error={}", e.getMessage(), e);
            return false;
        }
    }

    private String extractEventId(Map<String, Object> eventData) {
        Object aggregateId = eventData.get("aggregateId");
        return aggregateId != null ? aggregateId.toString() : "unknown";
    }

    private String extractStockItemId(Map<String, Object> eventData) {
        Object stockItemId = eventData.get("stockItemId");
        if (stockItemId == null) {
            // Try aggregateId as fallback
            stockItemId = eventData.get("aggregateId");
        }
        if (stockItemId == null) {
            throw new IllegalArgumentException("StockItemId not found in event data");
        }
        if (stockItemId instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> idMap = (Map<String, Object>) stockItemId;
            Object value = idMap.get("value");
            if (value != null) {
                return value.toString();
            }
        }
        return stockItemId.toString();
    }

    private String extractProductId(Map<String, Object> eventData) {
        Object productId = eventData.get("productId");
        if (productId == null) {
            throw new IllegalArgumentException("ProductId not found in event data");
        }
        if (productId instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> productIdMap = (Map<String, Object>) productId;
            Object value = productIdMap.get("value");
            if (value != null) {
                return value.toString();
            }
        }
        return productId.toString();
    }

    private LocalDate extractExpirationDate(Map<String, Object> eventData) {
        Object expirationDate = eventData.get("expirationDate");
        if (expirationDate == null) {
            throw new IllegalArgumentException("ExpirationDate not found in event data");
        }
        if (expirationDate instanceof String) {
            return LocalDate.parse((String) expirationDate);
        }
        if (expirationDate instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> dateMap = (Map<String, Object>) expirationDate;
            Object value = dateMap.get("value");
            if (value != null) {
                return LocalDate.parse(value.toString());
            }
        }
        throw new IllegalArgumentException("Invalid expiration date format in event data");
    }

    private Integer extractDaysUntilExpiry(Map<String, Object> eventData) {
        Object daysUntilExpiry = eventData.get("daysUntilExpiry");
        if (daysUntilExpiry == null) {
            return null; // Optional field
        }
        if (daysUntilExpiry instanceof Number) {
            return ((Number) daysUntilExpiry).intValue();
        }
        return Integer.parseInt(daysUntilExpiry.toString());
    }
}
