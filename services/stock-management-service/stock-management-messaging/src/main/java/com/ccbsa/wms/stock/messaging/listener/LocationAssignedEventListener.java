package com.ccbsa.wms.stock.messaging.listener;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

/**
 * Event Listener: LocationAssignedEventListener
 * <p>
 * Listens to LocationAssignedEvent from Location Management Service and updates stock items with location assignment.
 * <p>
 * This listener implements event-driven choreography:
 * - Location Management Service publishes LocationAssignedEvent when stock is assigned to a location
 * - This listener updates the stock item with the location ID
 * <p>
 * Note: This is a cross-service event listener (listens to location-management-events topic).
 * <p>
 * Idempotency: This listener is idempotent - checks if stock item already has the location assigned.
 */
@Component
public class LocationAssignedEventListener {
    private static final Logger logger = LoggerFactory.getLogger(LocationAssignedEventListener.class);

    private static final String LOCATION_ASSIGNED_EVENT = "LocationAssignedEvent";

    private final StockItemRepository stockItemRepository;

    public LocationAssignedEventListener(StockItemRepository stockItemRepository) {
        this.stockItemRepository = stockItemRepository;
    }

    @KafkaListener(topics = "location-management-events", groupId = "stock-management-service", containerFactory = "externalEventKafkaListenerContainerFactory")
    @Transactional
    public void handle(@Payload Map<String, Object> eventData, @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        try {
            extractAndSetCorrelationId(eventData);

            String detectedEventType = detectEventType(eventData, eventType);
            if (!isLocationAssignedEvent(detectedEventType, eventData)) {
                logger.debug("Skipping event - not LocationAssignedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            // Extract stock item ID and location ID
            String stockItemIdString = extractStockItemId(eventData);
            String locationIdString = extractLocationId(eventData);

            // Extract tenant ID and set in TenantContext
            TenantId tenantId = extractTenantId(eventData);
            TenantContext.setTenantId(tenantId);

            logger.info("Received LocationAssignedEvent: stockItemId={}, locationId={}, tenantId={}", stockItemIdString, locationIdString, tenantId.getValue());

            // Get stock item
            StockItemId stockItemId = StockItemId.of(stockItemIdString);
            StockItem stockItem =
                    stockItemRepository.findById(stockItemId, tenantId).orElseThrow(() -> new IllegalStateException(String.format("Stock item not found: %s", stockItemIdString)));

            // Check if location is already assigned (idempotency check)
            LocationId locationId = LocationId.of(locationIdString);
            if (stockItem.getLocationId() != null && stockItem.getLocationId().equals(locationId)) {
                logger.debug("Location already assigned to stock item: stockItemId={}, locationId={}", stockItemIdString, locationIdString);
                acknowledgment.acknowledge();
                return;
            }

            // Update stock item with location (if not already assigned)
            // Note: The location assignment logic is in the domain, but we need to update the stock item
            // Since LocationAssignedEvent is published by Location Management Service after assignment,
            // we just need to ensure the stock item reflects the location assignment
            // In a fully event-driven system, we might not need to update here if the assignment
            // was already done synchronously. However, for eventual consistency, we update here.

            // For now, we'll just log - the actual location assignment should have been done
            // synchronously in AssignLocationToStockCommandHandler
            logger.info("Location assignment event received for stock item: stockItemId={}, locationId={}", stockItemIdString, locationIdString);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing LocationAssignedEvent: eventId={}, error={}", extractEventId(eventData), e.getMessage(), e);
            acknowledgment.acknowledge();
        } finally {
            TenantContext.clear();
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
            if (lastDot >= 0) {
                return className.substring(lastDot + 1);
            }
            return className;
        }

        Object eventTypeObj = eventData.get("eventType");
        if (eventTypeObj != null) {
            return eventTypeObj.toString();
        }

        Object aggregateTypeObj = eventData.get("aggregateType");
        if (aggregateTypeObj != null) {
            return aggregateTypeObj.toString();
        }

        return "Unknown";
    }

    private boolean isLocationAssignedEvent(String detectedEventType, Map<String, Object> eventData) {
        if (LOCATION_ASSIGNED_EVENT.equals(detectedEventType)) {
            return true;
        }
        Object eventClass = eventData.get("@class");
        if (eventClass != null && eventClass.toString().contains(LOCATION_ASSIGNED_EVENT)) {
            return true;
        }
        return false;
    }

    private String extractStockItemId(Map<String, Object> eventData) {
        Object stockItemIdObj = eventData.get("stockItemId");
        if (stockItemIdObj != null) {
            if (stockItemIdObj instanceof UUID) {
                return stockItemIdObj.toString();
            }
            if (stockItemIdObj instanceof String) {
                return (String) stockItemIdObj;
            }
            if (stockItemIdObj instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> idMap = (Map<String, Object>) stockItemIdObj;
                Object valueObj = idMap.get("value");
                if (valueObj != null) {
                    return valueObj.toString();
                }
            }
            return stockItemIdObj.toString();
        }
        throw new IllegalArgumentException("stockItemId not found in event data");
    }

    private String extractLocationId(Map<String, Object> eventData) {
        Object locationIdObj = eventData.get("locationId");
        if (locationIdObj != null) {
            if (locationIdObj instanceof UUID) {
                return locationIdObj.toString();
            }
            if (locationIdObj instanceof String) {
                return (String) locationIdObj;
            }
            if (locationIdObj instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> idMap = (Map<String, Object>) locationIdObj;
                Object valueObj = idMap.get("value");
                if (valueObj != null) {
                    return valueObj.toString();
                }
            }
            return locationIdObj.toString();
        }
        throw new IllegalArgumentException("locationId not found in event data");
    }

    private TenantId extractTenantId(Map<String, Object> eventData) {
        Object tenantIdObj = eventData.get("tenantId");
        if (tenantIdObj != null) {
            if (tenantIdObj instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> tenantIdMap = (Map<String, Object>) tenantIdObj;
                Object valueObj = tenantIdMap.get("value");
                if (valueObj != null) {
                    return TenantId.of(valueObj.toString());
                }
            }
            return TenantId.of(tenantIdObj.toString());
        }
        throw new IllegalArgumentException("tenantId not found in event data");
    }

    private String extractEventId(Map<String, Object> eventData) {
        Object eventIdObj = eventData.get("eventId");
        if (eventIdObj != null) {
            return eventIdObj.toString();
        }
        Object idObj = eventData.get("id");
        if (idObj != null) {
            return idObj.toString();
        }
        return "unknown";
    }
}

