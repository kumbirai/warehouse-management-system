package com.ccbsa.wms.stock.messaging.listener;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.command.DecreaseStockQuantityCommandHandler;
import com.ccbsa.wms.stock.application.service.command.dto.DecreaseStockQuantityCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Listener: PickingTaskCompletedEventListener
 * <p>
 * Listens to PickingTaskCompletedEvent from Picking Service and updates stock levels.
 * <p>
 * This listener implements event-driven choreography:
 * - Picking task completion triggers stock quantity decrease
 * - Stock levels are updated to reflect picked quantities
 * - Stock level thresholds are checked after update
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PickingTaskCompletedEventListener {
    private static final String PICKING_TASK_COMPLETED_EVENT = "PickingTaskCompletedEvent";

    private final DecreaseStockQuantityCommandHandler decreaseStockQuantityCommandHandler;

    @KafkaListener(topics = "picking-events", groupId = "stock-management-service", containerFactory = "externalEventKafkaListenerContainerFactory")
    public void handle(@Payload Map<String, Object> eventData, @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        boolean shouldAcknowledge = false;
        try {
            extractAndSetCorrelationId(eventData);

            String detectedEventType = detectEventType(eventData, eventType);
            if (!isPickingTaskCompletedEvent(detectedEventType, eventData)) {
                log.debug("Skipping event - not PickingTaskCompletedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                shouldAcknowledge = true;
                return;
            }

            // Extract event fields
            String productCode = extractProductCode(eventData);
            String locationIdString = extractLocationId(eventData);
            Integer pickedQuantity = extractPickedQuantity(eventData);
            TenantId tenantId = extractTenantId(eventData);

            // Set tenant context
            TenantContext.setTenantId(tenantId);

            log.info("Received PickingTaskCompletedEvent: productCode={}, locationId={}, pickedQuantity={}, tenantId={}", productCode, locationIdString, pickedQuantity,
                    tenantId.getValue());

            // Process event in a separate transactional method
            shouldAcknowledge = processPickingTaskCompletedEvent(productCode, locationIdString, pickedQuantity, tenantId);

        } catch (Exception e) {
            log.error("Error processing PickingTaskCompletedEvent: eventId={}, error={}", extractEventId(eventData), e.getMessage(), e);
            shouldAcknowledge = true;
        } finally {
            TenantContext.clear();
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

    private boolean isPickingTaskCompletedEvent(String detectedEventType, Map<String, Object> eventData) {
        if (PICKING_TASK_COMPLETED_EVENT.equals(detectedEventType)) {
            return true;
        }
        Object eventClass = eventData.get("@class");
        return eventClass != null && eventClass.toString().contains(PICKING_TASK_COMPLETED_EVENT);
    }

    private String extractProductCode(Map<String, Object> eventData) {
        Object productCode = eventData.get("productCode");
        if (productCode == null) {
            throw new IllegalArgumentException("ProductCode not found in event data");
        }
        // Handle value object serialization (may be a Map with "value" key)
        if (productCode instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> productCodeMap = (Map<String, Object>) productCode;
            Object value = productCodeMap.get("value");
            if (value != null) {
                return value.toString();
            }
        }
        return productCode.toString();
    }

    private String extractLocationId(Map<String, Object> eventData) {
        Object locationId = eventData.get("locationId");
        if (locationId == null) {
            throw new IllegalArgumentException("LocationId not found in event data");
        }
        // Handle value object serialization (may be a Map with "value" key or UUID string)
        if (locationId instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> locationIdMap = (Map<String, Object>) locationId;
            Object value = locationIdMap.get("value");
            if (value != null) {
                return value.toString();
            }
        }
        return locationId.toString();
    }

    private Integer extractPickedQuantity(Map<String, Object> eventData) {
        Object pickedQuantity = eventData.get("pickedQuantity");
        if (pickedQuantity == null) {
            throw new IllegalArgumentException("PickedQuantity not found in event data");
        }
        if (pickedQuantity instanceof Number) {
            return ((Number) pickedQuantity).intValue();
        }
        return Integer.parseInt(pickedQuantity.toString());
    }

    private TenantId extractTenantId(Map<String, Object> eventData) {
        Object tenantIdObj = eventData.get("tenantId");
        if (tenantIdObj == null) {
            // Try to get from metadata
            @SuppressWarnings("unchecked") Map<String, Object> metadata = (Map<String, Object>) eventData.get("metadata");
            if (metadata != null) {
                tenantIdObj = metadata.get("tenantId");
            }
        }
        if (tenantIdObj == null) {
            throw new IllegalArgumentException("TenantId not found in event data or metadata");
        }
        // Handle value object serialization (may be a Map with "value" key)
        if (tenantIdObj instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> tenantIdMap = (Map<String, Object>) tenantIdObj;
            Object value = tenantIdMap.get("value");
            if (value != null) {
                return TenantId.of(value.toString());
            }
        }
        return TenantId.of(tenantIdObj.toString());
    }

    @Transactional
    private boolean processPickingTaskCompletedEvent(String productCode, String locationIdString, Integer pickedQuantity, TenantId tenantId) {
        try {
            // Note: We need ProductId and LocationId, but we have productCode and locationIdString
            // We'll need to convert productCode to ProductId via ProductServicePort
            // For now, we'll use a simplified approach - the command handler should handle this

            LocationId locationId = LocationId.of(java.util.UUID.fromString(locationIdString));

            // Create command to decrease stock quantity
            DecreaseStockQuantityCommand command =
                    DecreaseStockQuantityCommand.builder().tenantId(tenantId).productCode(productCode).locationId(locationId).quantity(pickedQuantity).build();

            decreaseStockQuantityCommandHandler.handle(command);

            log.info("Stock quantity decreased successfully for product: {} at location: {}", productCode, locationIdString);
            return true;
        } catch (Exception e) {
            log.error("Error processing PickingTaskCompletedEvent: productCode={}, locationId={}, error={}", productCode, locationIdString, e.getMessage(), e);
            return false;
        }
    }

    private String extractEventId(Map<String, Object> eventData) {
        Object aggregateId = eventData.get("aggregateId");
        return aggregateId != null ? aggregateId.toString() : "unknown";
    }
}
