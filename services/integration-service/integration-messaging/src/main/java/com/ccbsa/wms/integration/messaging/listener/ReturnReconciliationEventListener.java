package com.ccbsa.wms.integration.messaging.listener;

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
import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.integration.application.service.D365ReturnReconciliationService;
import com.ccbsa.wms.integration.application.service.port.messaging.IntegrationEventPublisher;
import com.ccbsa.wms.integration.domain.core.event.ReturnReconciledEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Listener: ReturnReconciliationEventListener
 * <p>
 * Listens to ReturnProcessedEvent from Returns Service and triggers D365 reconciliation.
 * <p>
 * This listener implements event-driven choreography:
 * - ReturnProcessedEvent triggers D365 reconciliation
 * - Only triggers for returns in PROCESSED or LOCATION_ASSIGNED status
 * - Publishes ReturnReconciledEvent after successful reconciliation
 * <p>
 * Idempotency: This listener is idempotent - checks return status before reconciliation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnReconciliationEventListener {
    private static final String RETURN_PROCESSED_EVENT = "ReturnProcessedEvent";

    private final D365ReturnReconciliationService reconciliationService;
    private final IntegrationEventPublisher eventPublisher;

    @KafkaListener(topics = "returns-events", groupId = "integration-service-return-reconciliation", containerFactory = "externalEventKafkaListenerContainerFactory")
    public void handle(@Payload Map<String, Object> eventData, @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        boolean shouldAcknowledge = false;
        try {
            extractAndSetCorrelationId(eventData);

            String detectedEventType = detectEventType(eventData, eventType);
            if (!isReturnProcessedEvent(detectedEventType, eventData)) {
                log.debug("Skipping event - not ReturnProcessedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                shouldAcknowledge = true;
                return;
            }

            // Extract return ID
            String returnIdString = extractReturnId(eventData);
            ReturnId returnId = ReturnId.of(returnIdString);

            // Extract tenant ID and set in TenantContext
            TenantId tenantId = extractTenantId(eventData);
            TenantContext.setTenantId(tenantId);

            log.info("Received ReturnProcessedEvent: returnId={}, tenantId={}", returnId.getValueAsString(), tenantId.getValue());

            // Process event in a separate transactional method
            shouldAcknowledge = processReturnProcessedEvent(returnId, tenantId);

        } catch (Exception e) {
            log.error("Error processing ReturnProcessedEvent: eventId={}, error={}", extractEventId(eventData), e.getMessage(), e);
            // Acknowledge to prevent infinite retries for business logic errors
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
            log.debug("Event type detected from header: {}", headerType);
            return headerType;
        }

        Object classObj = eventData.get("@class");
        if (classObj != null) {
            String className = classObj.toString();
            log.debug("Event type detected from @class property: {}", className);
            int lastDot = className.lastIndexOf('.');
            if (lastDot >= 0) {
                return className.substring(lastDot + 1);
            }
            return className;
        }

        Object eventTypeObj = eventData.get("eventType");
        if (eventTypeObj != null) {
            log.debug("Event type detected from eventType property: {}", eventTypeObj);
            return eventTypeObj.toString();
        }

        Object aggregateTypeObj = eventData.get("aggregateType");
        if (aggregateTypeObj != null) {
            log.debug("Event type detected from aggregateType property: {}", aggregateTypeObj);
            return aggregateTypeObj.toString();
        }

        log.warn("Could not detect event type from event data. Header: {}, EventData keys: {}", headerType, eventData.keySet());
        return "Unknown";
    }

    private boolean isReturnProcessedEvent(String detectedEventType, Map<String, Object> eventData) {
        if (RETURN_PROCESSED_EVENT.equals(detectedEventType)) {
            return true;
        }
        Object eventClass = eventData.get("@class");
        if (eventClass != null && eventClass.toString().contains(RETURN_PROCESSED_EVENT)) {
            return true;
        }
        return false;
    }

    private String extractReturnId(Map<String, Object> eventData) {
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
     * Processes the ReturnProcessedEvent in a transactional context.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID
     * @return true if the event should be acknowledged, false otherwise
     */
    @Transactional
    private boolean processReturnProcessedEvent(ReturnId returnId, TenantId tenantId) {
        try {
            // Trigger D365 reconciliation
            D365ReturnReconciliationService.ReconciliationResult result = reconciliationService.reconcileReturn(returnId, tenantId);

            // Publish ReturnReconciledEvent
            ReturnReconciledEvent event = new ReturnReconciledEvent(returnId, tenantId, result.getD365ReturnOrderId(), result.isInventoryAdjusted(), result.getCreditNoteId(),
                    result.isWriteOffProcessed());
            eventPublisher.publish(event);

            log.info("Successfully reconciled return with D365: returnId={}, d365ReturnOrderId={}, tenantId={}", returnId.getValueAsString(), result.getD365ReturnOrderId(),
                    tenantId.getValue());
        } catch (Exception e) {
            // Handle reconciliation failures gracefully
            // Returns can remain without D365 reconciliation - they can be retried later
            log.warn("Failed to reconcile return with D365: returnId={}, tenantId={}, error={}. " + "Return will remain without D365 reconciliation until retry.",
                    returnId.getValueAsString(), tenantId.getValue(), e.getMessage());
        }

        // Always acknowledge the event to prevent infinite reprocessing
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
}
