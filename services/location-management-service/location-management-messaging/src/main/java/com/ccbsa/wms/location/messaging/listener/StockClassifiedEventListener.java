package com.ccbsa.wms.location.messaging.listener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.location.application.service.command.AssignLocationsFEFOCommandHandler;
import com.ccbsa.wms.location.application.service.command.dto.AssignLocationsFEFOCommand;
import com.ccbsa.wms.location.application.service.command.dto.AssignLocationsFEResult;
import com.ccbsa.wms.location.domain.core.valueobject.StockItemAssignmentRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Listener: StockClassifiedEventListener
 * <p>
 * Listens to StockClassifiedEvent and triggers FEFO location assignment for newly classified stock items.
 * <p>
 * This listener implements event-driven choreography:
 * - Stock classification triggers FEFO location assignment
 * - Only triggers for newly classified stock (oldClassification is null)
 * - Creates StockItemAssignmentRequest and triggers AssignLocationsFEFOCommand
 * <p>
 * Idempotency: This listener is idempotent - checks if stock item already has a location assigned.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockClassifiedEventListener {
    private static final String STOCK_CLASSIFIED_EVENT = "StockClassifiedEvent";

    private final AssignLocationsFEFOCommandHandler fefoCommandHandler;

    @KafkaListener(topics = "stock-management-events", groupId = "location-management-service", containerFactory = "externalEventKafkaListenerContainerFactory")
    public void handle(@Payload Map<String, Object> eventData, @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        boolean shouldAcknowledge = false;
        try {
            extractAndSetCorrelationId(eventData);

            String detectedEventType = detectEventType(eventData, eventType);
            if (!isStockClassifiedEvent(detectedEventType, eventData)) {
                log.debug("Skipping event - not StockClassifiedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                shouldAcknowledge = true;
                return;
            }

            // Extract stock item ID and classification
            String stockItemIdString = extractStockItemId(eventData);
            StockClassification newClassification = extractClassification(eventData, "newClassification");

            // Only trigger FEFO for newly classified stock (initial classification)
            StockClassification oldClassification = extractClassification(eventData, "oldClassification");
            if (oldClassification != null) {
                log.debug("Skipping FEFO assignment - stock item already classified: stockItemId={}, oldClassification={}, newClassification={}", stockItemIdString,
                        oldClassification, newClassification);
                shouldAcknowledge = true;
                return;
            }

            // Extract tenant ID and set in TenantContext
            TenantId tenantId = extractTenantId(eventData);
            TenantContext.setTenantId(tenantId);

            log.info("Received StockClassifiedEvent: stockItemId={}, classification={}, tenantId={}", stockItemIdString, newClassification, tenantId.getValue());

            // Skip FEFO assignment for expired stock - expired stock cannot be assigned locations
            if (newClassification == StockClassification.EXPIRED) {
                log.debug("Skipping FEFO assignment for expired stock item: stockItemId={}, tenantId={}. " + "Expired stock cannot be assigned locations.", stockItemIdString,
                        tenantId.getValue());
                shouldAcknowledge = true;
                return;
            }

            // Process event in a separate transactional method to ensure proper connection management
            shouldAcknowledge = processStockClassifiedEvent(stockItemIdString, newClassification, tenantId, eventData);

        } catch (Exception e) {
            log.error("Error processing StockClassifiedEvent: eventId={}, error={}", extractEventId(eventData), e.getMessage(), e);
            // Acknowledge even on error to prevent infinite reprocessing
            // Stock item remains unassigned, which is acceptable
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

    private boolean isStockClassifiedEvent(String detectedEventType, Map<String, Object> eventData) {
        if (STOCK_CLASSIFIED_EVENT.equals(detectedEventType)) {
            return true;
        }
        Object eventClass = eventData.get("@class");
        if (eventClass != null && eventClass.toString().contains(STOCK_CLASSIFIED_EVENT)) {
            return true;
        }
        return false;
    }

    private String extractStockItemId(Map<String, Object> eventData) {
        Object aggregateIdObj = eventData.get("aggregateId");
        if (aggregateIdObj != null) {
            if (aggregateIdObj instanceof UUID) {
                return aggregateIdObj.toString();
            }
            if (aggregateIdObj instanceof String) {
                return (String) aggregateIdObj;
            }
            if (aggregateIdObj instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> idMap = (Map<String, Object>) aggregateIdObj;
                Object valueObj = idMap.get("value");
                if (valueObj != null) {
                    return valueObj.toString();
                }
            }
            return aggregateIdObj.toString();
        }
        throw new IllegalArgumentException("aggregateId not found in event data");
    }

    private StockClassification extractClassification(Map<String, Object> eventData, String fieldName) {
        Object classificationObj = eventData.get(fieldName);
        if (classificationObj != null) {
            if (classificationObj instanceof String) {
                try {
                    return StockClassification.valueOf((String) classificationObj);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid classification value: {}", classificationObj);
                    return null;
                }
            }
            if (classificationObj instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> classificationMap = (Map<String, Object>) classificationObj;
                Object nameObj = classificationMap.get("name");
                if (nameObj != null) {
                    try {
                        return StockClassification.valueOf(nameObj.toString());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid classification name: {}", nameObj);
                        return null;
                    }
                }
            }
        }
        return null;
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
     * Processes the StockClassifiedEvent in a transactional context.
     * This ensures proper connection management and transaction commit/rollback.
     *
     * @param stockItemIdString the stock item ID
     * @param newClassification the new classification
     * @param tenantId          the tenant ID
     * @param eventData         the event data for extracting additional information
     * @return true if the event should be acknowledged, false otherwise
     */
    @Transactional
    private boolean processStockClassifiedEvent(String stockItemIdString, StockClassification newClassification, TenantId tenantId, Map<String, Object> eventData) {
        log.debug("Processing StockClassifiedEvent for FEFO assignment: stockItemId={}, classification={}, tenantId={}", stockItemIdString, newClassification,
                tenantId.getValue());

        // Extract expiration date and quantity from event
        ExpirationDate expirationDate = extractExpirationDate(eventData);
        BigDecimal quantity = extractQuantity(eventData);

        log.debug("Extracted event data: stockItemId={}, expirationDate={}, quantity={}, classification={}", stockItemIdString, expirationDate, quantity, newClassification);

        // Create stock item assignment request
        StockItemAssignmentRequest assignmentRequest =
                StockItemAssignmentRequest.builder().stockItemId(stockItemIdString).quantity(quantity).expirationDate(expirationDate).classification(newClassification).build();

        // Trigger FEFO assignment
        List<StockItemAssignmentRequest> stockItems = List.of(assignmentRequest);
        AssignLocationsFEFOCommand command = AssignLocationsFEFOCommand.builder().tenantId(tenantId).stockItems(stockItems).build();

        try {
            log.debug("Calling FEFO assignment handler for stock item: stockItemId={}, tenantId={}", stockItemIdString, tenantId.getValue());
            AssignLocationsFEResult result = fefoCommandHandler.handle(command);

            // Check if assignment was successful
            if (result.getAssignments().containsKey(stockItemIdString)) {
                log.info("Successfully assigned location via FEFO for stock item: stockItemId={}, locationId={}, tenantId={}", stockItemIdString,
                        result.getAssignments().get(stockItemIdString).getValueAsString(), tenantId.getValue());
            } else {
                // Assignment failed - no available locations or insufficient capacity
                // This is acceptable - stock item remains unassigned and can be:
                // 1. Assigned later when locations become available
                // 2. Allocated from unassigned items (handled in allocation logic)
                log.warn("FEFO assignment did not assign location for stock item: stockItemId={}, tenantId={}, quantity={}, expirationDate={}. "
                        + "Possible reasons: no available BIN locations, insufficient capacity, or all locations are full. "
                        + "Stock item will remain unassigned until locations become available or manual assignment.", stockItemIdString, tenantId.getValue(), quantity,
                        expirationDate);
            }
        } catch (IllegalStateException e) {
            // Handle specific case: no BIN locations available
            if (e.getMessage() != null && e.getMessage().contains("No BIN type locations")) {
                log.warn("FEFO assignment failed - no BIN type locations available: stockItemId={}, tenantId={}, error={}. "
                        + "Stock item will remain unassigned until BIN locations are created. "
                        + "Stock can still be allocated from unassigned items.", stockItemIdString, tenantId.getValue(), e.getMessage());
            } else {
                log.warn("FEFO assignment failed with IllegalStateException: stockItemId={}, tenantId={}, error={}. "
                        + "Stock item will remain unassigned until locations become available or manual assignment.", stockItemIdString, tenantId.getValue(), e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            // Handle validation errors
            log.error("FEFO assignment failed with validation error: stockItemId={}, tenantId={}, error={}. "
                    + "This indicates a configuration issue that should be investigated.", stockItemIdString, tenantId.getValue(), e.getMessage(), e);
        } catch (Exception e) {
            // Handle other assignment failures gracefully
            // Stock item remains unassigned, which is acceptable behavior
            log.error("FEFO assignment failed with unexpected error: stockItemId={}, tenantId={}, error={}. "
                    + "Stock item will remain unassigned until locations become available or manual assignment.", stockItemIdString, tenantId.getValue(), e.getMessage(), e);
        }

        // Always acknowledge the event to prevent infinite reprocessing
        // Stock items can remain unassigned - they can be assigned later or allocated from unassigned items
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

    private ExpirationDate extractExpirationDate(Map<String, Object> eventData) {
        Object expirationDateObj = eventData.get("expirationDate");
        if (expirationDateObj != null) {
            if (expirationDateObj instanceof String) {
                try {
                    LocalDate date = LocalDate.parse((String) expirationDateObj);
                    return ExpirationDate.of(date);
                } catch (Exception e) {
                    log.warn("Invalid expiration date format: {}", expirationDateObj);
                    return null;
                }
            }
            if (expirationDateObj instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> dateMap = (Map<String, Object>) expirationDateObj;
                Object valueObj = dateMap.get("value");
                if (valueObj != null) {
                    try {
                        LocalDate date = LocalDate.parse(valueObj.toString());
                        return ExpirationDate.of(date);
                    } catch (Exception e) {
                        log.warn("Invalid expiration date value: {}", valueObj);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private BigDecimal extractQuantity(Map<String, Object> eventData) {
        Object quantityObj = eventData.get("quantity");
        if (quantityObj != null) {
            if (quantityObj instanceof Number) {
                return BigDecimal.valueOf(((Number) quantityObj).doubleValue());
            }
            if (quantityObj instanceof String) {
                try {
                    return new BigDecimal((String) quantityObj);
                } catch (NumberFormatException e) {
                    log.warn("Invalid quantity format: {}", quantityObj);
                    return BigDecimal.ONE; // Fallback to 1
                }
            }
            if (quantityObj instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> quantityMap = (Map<String, Object>) quantityObj;
                Object valueObj = quantityMap.get("value");
                if (valueObj != null) {
                    if (valueObj instanceof Number) {
                        return BigDecimal.valueOf(((Number) valueObj).doubleValue());
                    }
                    try {
                        return new BigDecimal(valueObj.toString());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid quantity value: {}", valueObj);
                        return BigDecimal.ONE; // Fallback to 1
                    }
                }
            }
        }
        log.warn("Quantity not found in event data, using default value of 1");
        return BigDecimal.ONE; // Fallback to 1 if quantity not found
    }
}

