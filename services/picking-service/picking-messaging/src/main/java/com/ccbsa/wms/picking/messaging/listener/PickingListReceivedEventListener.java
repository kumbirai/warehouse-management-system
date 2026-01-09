package com.ccbsa.wms.picking.messaging.listener;

import java.util.List;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.picking.application.service.command.PlanPickingLocationsCommandHandler;
import com.ccbsa.wms.picking.application.service.command.dto.PlanPickingLocationsCommand;
import com.ccbsa.wms.picking.application.service.exception.StockManagementServiceException;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Listener: PickingListReceivedEventListener
 * <p>
 * Listens to PickingListReceivedEvent and triggers picking location planning.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PickingListReceivedEventListener {
    private final PlanPickingLocationsCommandHandler planPickingLocationsCommandHandler;

    @KafkaListener(topics = "picking-events", groupId = "picking-service", containerFactory = "internalEventKafkaListenerContainerFactory")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(@org.springframework.messaging.handler.annotation.Payload Map<String, Object> eventData,
                       @org.springframework.messaging.handler.annotation.Header(value = "__TypeId__", required = false) String eventType,
                       @org.springframework.messaging.handler.annotation.Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        try {
            extractAndSetCorrelationId(eventData);

            String detectedEventType = detectEventType(eventData, eventType);
            if (!isPickingListReceivedEvent(detectedEventType)) {
                log.debug("Skipping event - not PickingListReceivedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            String aggregateId = extractAggregateId(eventData);
            TenantId tenantId = extractTenantId(eventData);
            TenantContext.setTenantId(tenantId);

            @SuppressWarnings("unchecked") List<String> loadIds = (List<String>) eventData.get("loadIds");

            log.info("Received PickingListReceivedEvent: pickingListId={}, tenantId={}, loadIds={}", aggregateId, tenantId.getValue(), loadIds);

            // Plan picking locations for each load
            PickingListId pickingListId = PickingListId.of(aggregateId);
            for (String loadIdStr : loadIds) {
                LoadId loadId = LoadId.of(loadIdStr);
                PlanPickingLocationsCommand command = PlanPickingLocationsCommand.builder().tenantId(tenantId).pickingListId(pickingListId).loadId(loadId).build();
                planPickingLocationsCommandHandler.handle(command);
            }

            acknowledgment.acknowledge();
        } catch (StockManagementServiceException e) {
            // Service unavailable errors are retryable - let error handler manage retries and DLQ
            log.error("Stock management service unavailable while processing PickingListReceivedEvent: eventData={}, error={}", eventData, e.getMessage(), e);
            throw e;
        } catch (IllegalArgumentException e) {
            // Validation errors are not retryable - should not happen in production
            log.error("Invalid event data for PickingListReceivedEvent: eventData={}, error={}", eventData, e.getMessage(), e);
            throw new RuntimeException("Invalid PickingListReceivedEvent data", e);
        } catch (Exception e) {
            // Other errors - preserve exception type for proper error handling
            log.error("Failed to process PickingListReceivedEvent: eventData={}, error={}", eventData, e.getMessage(), e);
            throw e;
        } finally {
            CorrelationContext.clear();
            TenantContext.clear();
        }
    }

    private void extractAndSetCorrelationId(Map<String, Object> eventData) {
        try {
            Object metadataObj = eventData.get("metadata");
            if (metadataObj instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> metadata = (Map<String, Object>) metadataObj;
                Object correlationIdObj = metadata.get("correlationId");
                if (correlationIdObj != null) {
                    CorrelationContext.setCorrelationId(correlationIdObj.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract correlation ID: {}", e.getMessage());
        }
    }

    private String detectEventType(Map<String, Object> eventData, String headerType) {
        if (headerType != null) {
            return headerType;
        }
        Object classField = eventData.get("@class");
        if (classField != null) {
            return classField.toString();
        }
        // Check for event-specific fields
        if (eventData.containsKey("loadIds")) {
            return "PickingListReceivedEvent";
        }
        return "Unknown";
    }

    private boolean isPickingListReceivedEvent(String eventType) {
        return eventType != null && eventType.contains("PickingListReceivedEvent");
    }

    private String extractAggregateId(Map<String, Object> eventData) {
        Object aggregateId = eventData.get("aggregateId");
        if (aggregateId == null) {
            throw new IllegalArgumentException("Event missing aggregateId");
        }
        return aggregateId.toString();
    }

    private TenantId extractTenantId(Map<String, Object> eventData) {
        Object tenantIdObj = eventData.get("tenantId");
        if (tenantIdObj == null) {
            throw new IllegalArgumentException("Event missing tenantId");
        }
        if (tenantIdObj instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> tenantIdMap = (Map<String, Object>) tenantIdObj;
            Object value = tenantIdMap.get("value");
            return TenantId.of(value != null ? value.toString() : null);
        }
        return TenantId.of(tenantIdObj.toString());
    }
}
