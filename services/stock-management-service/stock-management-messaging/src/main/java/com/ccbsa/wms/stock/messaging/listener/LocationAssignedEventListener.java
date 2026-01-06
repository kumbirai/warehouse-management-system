package com.ccbsa.wms.stock.messaging.listener;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationAssignedEventListener {
    private static final String LOCATION_ASSIGNED_EVENT = "LocationAssignedEvent";

    private final StockItemRepository stockItemRepository;

    @KafkaListener(topics = "location-management-events", groupId = "stock-management-service", containerFactory = "externalEventKafkaListenerContainerFactory")
    public void handle(@Payload Map<String, Object> eventData, @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        boolean shouldAcknowledge = false;
        try {
            extractAndSetCorrelationId(eventData);

            String detectedEventType = detectEventType(eventData, eventType);
            if (!isLocationAssignedEvent(detectedEventType, eventData)) {
                log.debug("Skipping event - not LocationAssignedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                shouldAcknowledge = true;
                return;
            }

            // Extract stock item ID and location ID
            String stockItemIdString = extractStockItemId(eventData);
            String locationIdString = extractLocationId(eventData);

            // Extract tenant ID and set in TenantContext
            TenantId tenantId = extractTenantId(eventData);
            TenantContext.setTenantId(tenantId);

            log.info("Received LocationAssignedEvent: stockItemId={}, locationId={}, tenantId={}", stockItemIdString, locationIdString, tenantId.getValue());

            // Process event in a separate transactional method to ensure proper connection management
            shouldAcknowledge = processLocationAssignedEvent(stockItemIdString, locationIdString, tenantId);

        } catch (Exception e) {
            log.error("Error processing LocationAssignedEvent: eventId={}, error={}", extractEventId(eventData), e.getMessage(), e);
            // For exceptions, acknowledge to prevent infinite reprocessing of malformed events
            shouldAcknowledge = true;
        } finally {
            TenantContext.clear();
            // Acknowledge only after transaction completes and context is cleared
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

    /**
     * Processes the LocationAssignedEvent in a transactional context.
     * This ensures proper connection management and transaction commit/rollback.
     * <p>
     * Implements retry logic to handle optimistic locking failures that occur when
     * multiple concurrent events attempt to update the same StockItem entity.
     * Uses exponential backoff with a maximum of 3 retries.
     *
     * @param stockItemIdString the stock item ID
     * @param locationIdString  the location ID
     * @param tenantId          the tenant ID
     * @return true if the event should be acknowledged, false otherwise
     */
    @Transactional
    private boolean processLocationAssignedEvent(String stockItemIdString, String locationIdString, TenantId tenantId) {
        int maxRetries = 3;
        int retryCount = 0;
        long baseBackoffMs = 50;

        while (retryCount <= maxRetries) {
            try {
                return processLocationAssignedEventInternal(stockItemIdString, locationIdString, tenantId);
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount > maxRetries) {
                    // Max retries exceeded - log error and acknowledge to prevent infinite reprocessing
                    log.error("Optimistic locking failure after {} retries for LocationAssignedEvent: stockItemId={}, locationId={}, tenantId={}. "
                                    + "This indicates high concurrency - acknowledging event to prevent infinite retries.", maxRetries, stockItemIdString, locationIdString,
                            tenantId.getValue(), e);
                    return true;
                }

                // Calculate exponential backoff with jitter
                long backoffMs = baseBackoffMs * (1L << (retryCount - 1));
                long jitter = (long) (Math.random() * backoffMs * 0.1);
                long sleepMs = backoffMs + jitter;

                log.warn("Optimistic locking failure for LocationAssignedEvent (retry {}/{}): stockItemId={}, locationId={}, tenantId={}. Retrying after {}ms", retryCount,
                        maxRetries, stockItemIdString, locationIdString, tenantId.getValue(), sleepMs);

                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Retry interrupted for LocationAssignedEvent: stockItemId={}, locationId={}, tenantId={}. Acknowledging event.", stockItemIdString, locationIdString,
                            tenantId.getValue());
                    return true;
                }
            }
        }

        return true;
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

    /**
     * Internal method that performs the actual event processing logic.
     * Separated to allow retry logic to catch optimistic locking exceptions.
     *
     * @param stockItemIdString the stock item ID
     * @param locationIdString  the location ID
     * @param tenantId          the tenant ID
     * @return true if the event should be acknowledged, false otherwise
     */
    private boolean processLocationAssignedEventInternal(String stockItemIdString, String locationIdString, TenantId tenantId) {
        // Get stock item
        StockItemId stockItemId = StockItemId.of(stockItemIdString);
        Optional<StockItem> stockItemOptional = stockItemRepository.findById(stockItemId, tenantId);

        // Handle case where stock item doesn't exist (race condition or deleted item)
        // This is expected in event-driven systems where events can arrive out of order
        // or after the item has been deleted. We acknowledge the event to prevent infinite reprocessing.
        if (stockItemOptional.isEmpty()) {
            log.debug("Stock item not found for LocationAssignedEvent (expected in event-driven systems - may be race condition or deleted item): "
                    + "stockItemId={}, locationId={}, tenantId={}. Acknowledging event to prevent reprocessing.", stockItemIdString, locationIdString, tenantId.getValue());
            return true;
        }

        StockItem stockItem = stockItemOptional.get();

        // Check if stock is expired - cannot assign location to expired stock
        if (stockItem.getClassification() == StockClassification.EXPIRED) {
            log.warn("Cannot assign location to expired stock: stockItemId={}, locationId={}, tenantId={}. Acknowledging event to prevent reprocessing.", stockItemIdString,
                    locationIdString, tenantId.getValue());
            return true;
        }

        // Check if location is already assigned (idempotency check)
        LocationId locationId = LocationId.of(locationIdString);
        if (stockItem.getLocationId() != null && stockItem.getLocationId().equals(locationId)) {
            log.debug("Location already assigned to stock item: stockItemId={}, locationId={}", stockItemIdString, locationIdString);
            return true;
        }

        // Update stock item with location (if not already assigned)
        // This handles the case where Location Management Service assigns location via FEFO
        // and publishes LocationAssignedEvent. We need to update the stock item to reflect
        // the location assignment for eventual consistency.
        if (stockItem.getLocationId() == null) {
            // Get quantity from stock item (needed for assignLocation method)
            Quantity quantity = stockItem.getQuantity();

            // Validate quantity before assignment
            if (quantity == null || quantity.getValue() <= 0) {
                log.warn("Cannot assign location to stock item with zero quantity: stockItemId={}, locationId={}, tenantId={}. Acknowledging event to prevent reprocessing.",
                        stockItemIdString, locationIdString, tenantId.getValue());
                return true;
            }

            try {
                // Update stock item with location using domain method
                // Note: This will publish LocationAssignedEvent again, but the idempotency check
                // in the Location Management Service will prevent duplicate processing
                stockItem.assignLocation(locationId, quantity);

                // Persist the updated stock item
                stockItemRepository.save(stockItem);

                log.info("Updated stock item with location assignment: stockItemId={}, locationId={}", stockItemIdString, locationIdString);
            } catch (IllegalStateException e) {
                // Handle business rule violations gracefully (e.g., expired stock, zero quantity)
                log.warn(
                        "Cannot assign location to stock item due to business rule violation: stockItemId={}, locationId={}, error={}. Acknowledging event to prevent "
                                + "reprocessing.",
                        stockItemIdString, locationIdString, e.getMessage());
                return true;
            }
        } else {
            log.debug("Stock item already has location assigned: stockItemId={}, existingLocationId={}, newLocationId={}", stockItemIdString,
                    stockItem.getLocationId().getValueAsString(), locationIdString);
        }

        return true;
    }
}

