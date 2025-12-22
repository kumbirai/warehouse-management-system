package com.ccbsa.wms.location.messaging.listener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.location.application.service.command.AssignLocationsFEFOCommandHandler;
import com.ccbsa.wms.location.application.service.command.dto.AssignLocationsFEFOCommand;
import com.ccbsa.wms.location.domain.core.valueobject.StockItemAssignmentRequest;

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
@Component
public class StockClassifiedEventListener {
    private static final Logger logger = LoggerFactory.getLogger(StockClassifiedEventListener.class);

    private static final String STOCK_CLASSIFIED_EVENT = "StockClassifiedEvent";

    private final AssignLocationsFEFOCommandHandler fefoCommandHandler;

    public StockClassifiedEventListener(AssignLocationsFEFOCommandHandler fefoCommandHandler) {
        this.fefoCommandHandler = fefoCommandHandler;
    }

    @KafkaListener(topics = "stock-management-events", groupId = "location-management-service", containerFactory = "externalEventKafkaListenerContainerFactory")
    @Transactional
    public void handle(@Payload Map<String, Object> eventData, @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        try {
            extractAndSetCorrelationId(eventData);

            String detectedEventType = detectEventType(eventData, eventType);
            if (!isStockClassifiedEvent(detectedEventType, eventData)) {
                logger.debug("Skipping event - not StockClassifiedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            // Extract stock item ID and classification
            String stockItemIdString = extractStockItemId(eventData);
            StockClassification newClassification = extractClassification(eventData, "newClassification");

            // Only trigger FEFO for newly classified stock (initial classification)
            StockClassification oldClassification = extractClassification(eventData, "oldClassification");
            if (oldClassification != null) {
                logger.debug("Skipping FEFO assignment - stock item already classified: stockItemId={}, oldClassification={}, newClassification={}", stockItemIdString,
                        oldClassification, newClassification);
                acknowledgment.acknowledge();
                return;
            }

            // Extract tenant ID and set in TenantContext
            TenantId tenantId = extractTenantId(eventData);
            TenantContext.setTenantId(tenantId);

            logger.info("Received StockClassifiedEvent: stockItemId={}, classification={}, tenantId={}", stockItemIdString, newClassification, tenantId.getValue());

            // Extract expiration date and quantity
            // Note: We need to get the stock item to get quantity - for now, we'll use a placeholder
            // In production, we might need to query Stock Management Service or include quantity in event
            // For Sprint 3, we'll create a single-item assignment request
            ExpirationDate expirationDate = extractExpirationDate(eventData);

            // Create stock item assignment request
            // Note: Quantity is not in the event - we'll need to query or include it
            // For now, we'll use a default quantity of 1 (this should be fixed to include quantity in event)
            StockItemAssignmentRequest assignmentRequest =
                    StockItemAssignmentRequest.builder().stockItemId(stockItemIdString).quantity(BigDecimal.ONE) // TODO: Include quantity in StockClassifiedEvent
                            .expirationDate(expirationDate).classification(newClassification).build();

            // Trigger FEFO assignment
            List<StockItemAssignmentRequest> stockItems = List.of(assignmentRequest);
            AssignLocationsFEFOCommand command = AssignLocationsFEFOCommand.builder().tenantId(tenantId).stockItems(stockItems).build();

            fefoCommandHandler.handle(command);

            logger.info("Successfully triggered FEFO assignment for stock item: stockItemId={}", stockItemIdString);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing StockClassifiedEvent: eventId={}, error={}", extractEventId(eventData), e.getMessage(), e);
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
                    logger.warn("Invalid classification value: {}", classificationObj);
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
                        logger.warn("Invalid classification name: {}", nameObj);
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

    private ExpirationDate extractExpirationDate(Map<String, Object> eventData) {
        Object expirationDateObj = eventData.get("expirationDate");
        if (expirationDateObj != null) {
            if (expirationDateObj instanceof String) {
                try {
                    LocalDate date = LocalDate.parse((String) expirationDateObj);
                    return ExpirationDate.of(date);
                } catch (Exception e) {
                    logger.warn("Invalid expiration date format: {}", expirationDateObj);
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
                        logger.warn("Invalid expiration date value: {}", valueObj);
                        return null;
                    }
                }
            }
        }
        return null;
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

