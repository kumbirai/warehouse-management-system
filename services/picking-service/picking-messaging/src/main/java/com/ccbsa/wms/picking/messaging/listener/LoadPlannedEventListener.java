package com.ccbsa.wms.picking.messaging.listener;

import java.util.List;
import java.util.Map;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.picking.application.service.port.repository.LoadRepository;
import com.ccbsa.wms.picking.application.service.port.repository.PickingListRepository;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadStatus;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Listener: LoadPlannedEventListener
 * <p>
 * Listens to LoadPlannedEvent and checks if all loads for a picking list are planned.
 * When all loads are planned, marks the picking list as PLANNED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoadPlannedEventListener {
    private final PickingListRepository pickingListRepository;
    private final LoadRepository loadRepository;

    @KafkaListener(topics = "picking-events", groupId = "picking-service-load-planned", containerFactory = "internalEventKafkaListenerContainerFactory")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(@org.springframework.messaging.handler.annotation.Payload Map<String, Object> eventData,
                       @org.springframework.messaging.handler.annotation.Header(value = "__TypeId__", required = false) String eventType,
                       @org.springframework.messaging.handler.annotation.Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        try {
            extractAndSetCorrelationId(eventData);

            String detectedEventType = detectEventType(eventData, eventType);
            if (!isLoadPlannedEvent(detectedEventType)) {
                log.debug("Skipping event - not LoadPlannedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            String aggregateId = extractAggregateId(eventData);
            TenantId tenantId = extractTenantId(eventData);
            TenantContext.setTenantId(tenantId);

            String pickingListIdStr = (String) eventData.get("pickingListId");

            log.info("Received LoadPlannedEvent: loadId={}, pickingListId={}, tenantId={}, pickingTaskIds={}", aggregateId, pickingListIdStr, tenantId.getValue(),
                    eventData.get("pickingTaskIds"));

            // Check if all loads for the picking list are planned
            // Only process if pickingListId is present (loads without picking lists are standalone)
            if (pickingListIdStr != null && !pickingListIdStr.isEmpty()) {
                checkAndUpdatePickingListStatus(PickingListId.of(pickingListIdStr), tenantId);
            } else {
                log.debug("LoadPlannedEvent has no pickingListId - skipping picking list status update (standalone load): loadId={}", aggregateId);
            }

            acknowledgment.acknowledge();
        } catch (IllegalArgumentException e) {
            // Validation errors are not retryable - should not happen in production
            log.error("Invalid event data for LoadPlannedEvent: eventData={}, error={}", eventData, e.getMessage(), e);
            acknowledgment.acknowledge(); // Acknowledge to avoid infinite retries
        } catch (Exception e) {
            // Other errors - preserve exception type for proper error handling
            log.error("Failed to process LoadPlannedEvent: eventData={}, error={}", eventData, e.getMessage(), e);
            throw e;
        } finally {
            CorrelationContext.clear();
            TenantContext.clear();
        }
    }

    @Transactional
    private void checkAndUpdatePickingListStatus(PickingListId pickingListId, TenantId tenantId) {
        // Load picking list with fresh data from database
        PickingList pickingList = pickingListRepository.findByIdAndTenantId(pickingListId, tenantId)
                .orElse(null);

        if (pickingList == null) {
            log.warn("Picking list not found: {} for tenant: {}", pickingListId.getValueAsString(), tenantId.getValue());
            return;
        }

        // Check if picking list is already PLANNED or COMPLETED
        if (pickingList.getStatus() == PickingListStatus.PLANNED || pickingList.getStatus() == PickingListStatus.COMPLETED) {
            log.debug("Picking list already in final status: {}", pickingList.getStatus());
            return;
        }

        // Query loads directly from database to ensure fresh statuses (bypasses cache)
        // This is critical because cached picking lists may have stale load statuses
        // Retry logic to handle eventual consistency - the load status might not be immediately visible
        List<com.ccbsa.wms.picking.domain.core.entity.Load> loads = null;
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            loads = loadRepository.findByPickingListIdAndTenantId(pickingListId, tenantId);
            if (loads != null && !loads.isEmpty()) {
                break;
            }
            retryCount++;
            if (retryCount < maxRetries) {
                try {
                    Thread.sleep(100); // Small delay to allow database to catch up
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while waiting for loads to be available");
                    return;
                }
            }
        }

        if (loads == null || loads.isEmpty()) {
            log.warn("Picking list has no loads after {} retries: {}", maxRetries, pickingListId.getValueAsString());
            return;
        }

        // Check if all loads are planned (using fresh data from database)
        boolean allLoadsPlanned = loads.stream()
                .allMatch(load -> load.getStatus() == LoadStatus.PLANNED);

        if (allLoadsPlanned) {
            log.info("All loads are planned for picking list: {}. Current status: {}", pickingListId.getValueAsString(), pickingList.getStatus());

            // Perform status transitions in memory first, then save once to avoid optimistic locking issues
            boolean statusChanged = false;

            // Mark picking list as PROCESSING first (if it's RECEIVED)
            if (pickingList.getStatus() == PickingListStatus.RECEIVED) {
                pickingList.markAsProcessed();
                log.info("Transitioning picking list to PROCESSING: {}", pickingListId.getValueAsString());
                statusChanged = true;
            }

            // Mark picking list as PLANNED (if it's PROCESSING or was just marked as PROCESSING)
            if (pickingList.getStatus() == PickingListStatus.PROCESSING) {
                pickingList.markAsPlanned();
                log.info("Transitioning picking list to PLANNED: {}", pickingListId.getValueAsString());
                statusChanged = true;
            } else if (pickingList.getStatus() != PickingListStatus.PLANNED && pickingList.getStatus() != PickingListStatus.COMPLETED) {
                log.warn("Picking list status is {} after all loads are planned, expected PROCESSING. Picking list ID: {}",
                        pickingList.getStatus(), pickingListId.getValueAsString());
            }

            // Save only once after all status transitions to avoid optimistic locking conflict
            // Add retry logic to handle concurrent updates from multiple LoadPlannedEvents
            if (statusChanged) {
                savePickingListWithRetry(pickingList, pickingListId);
            }
        } else {
            log.debug("Not all loads are planned yet for picking list: {}. Planned: {}/{}, Status: {}", 
                    pickingListId.getValueAsString(),
                    loads.stream().filter(load -> load.getStatus() == LoadStatus.PLANNED).count(),
                    loads.size(),
                    pickingList.getStatus());
        }
    }

    /**
     * Saves picking list with retry logic to handle optimistic locking failures.
     * <p>
     * When multiple LoadPlannedEvents are processed concurrently for the same picking list,
     * optimistic locking conflicts can occur. This method implements exponential backoff
     * retry to handle such conflicts gracefully.
     *
     * @param pickingList  Picking list to save
     * @param pickingListId Picking list ID for logging
     */
    private void savePickingListWithRetry(PickingList pickingList, PickingListId pickingListId) {
        int maxRetries = 3;
        int retryCount = 0;
        long baseBackoffMs = 50;

        while (retryCount <= maxRetries) {
            try {
                // Reload picking list to get fresh version for optimistic locking
                PickingList freshPickingList = pickingListRepository.findByIdAndTenantId(pickingListId, pickingList.getTenantId())
                        .orElse(null);
                
                if (freshPickingList == null) {
                    log.warn("Picking list not found during retry: {}", pickingListId.getValueAsString());
                    return;
                }

                // Re-apply status transitions on fresh instance
                boolean needsSave = false;
                if (freshPickingList.getStatus() == PickingListStatus.RECEIVED) {
                    freshPickingList.markAsProcessed();
                    needsSave = true;
                }
                if (freshPickingList.getStatus() == PickingListStatus.PROCESSING) {
                    freshPickingList.markAsPlanned();
                    needsSave = true;
                }

                if (needsSave) {
                    pickingListRepository.save(freshPickingList);
                    log.info("Picking list status updated to PLANNED: {}", pickingListId.getValueAsString());
                    // Note: Cache is automatically updated by CachedPickingListRepositoryAdapter.save()
                    // The cache will be updated with the new status after the database save
                } else {
                    // Status already updated by another concurrent event - this is fine
                    log.debug("Picking list status already updated by concurrent event: {}", pickingListId.getValueAsString());
                }
                return; // Success
            } catch (OptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount > maxRetries) {
                    // Max retries exceeded - log error but don't throw (event will be retried by Kafka)
                    log.error("Optimistic locking failure after {} retries for LoadPlannedEvent: pickingListId={}. "
                            + "This indicates high concurrency. Event will be retried by Kafka.", maxRetries, pickingListId.getValueAsString(), e);
                    throw e; // Re-throw to trigger Kafka retry
                }

                // Calculate exponential backoff with jitter
                long backoffMs = baseBackoffMs * (1L << (retryCount - 1));
                long jitter = (long) (Math.random() * backoffMs * 0.1);
                long sleepMs = backoffMs + jitter;

                log.warn("Optimistic locking failure for LoadPlannedEvent (retry {}/{}): pickingListId={}. Retrying after {}ms", retryCount,
                        maxRetries, pickingListId.getValueAsString(), sleepMs);

                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Retry interrupted for LoadPlannedEvent: pickingListId={}. Re-throwing to trigger Kafka retry.", pickingListId.getValueAsString());
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
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
        if (eventData.containsKey("pickingTaskIds")) {
            return "LoadPlannedEvent";
        }
        return "Unknown";
    }

    private boolean isLoadPlannedEvent(String eventType) {
        return eventType != null && eventType.contains("LoadPlannedEvent");
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
