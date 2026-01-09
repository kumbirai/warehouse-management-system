package com.ccbsa.wms.location.messaging.listener;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.application.context.CorrelationContext;
import com.ccbsa.common.domain.valueobject.MovementReason;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.location.application.service.command.CreateStockMovementCommandHandler;
import com.ccbsa.wms.location.application.service.command.dto.CreateStockMovementCommand;
import com.ccbsa.wms.location.application.service.port.data.LocationViewRepository;
import com.ccbsa.wms.location.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.location.application.service.port.service.StockManagementServicePort;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.MovementType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Listener: PickingTaskCompletedEventListener
 * <p>
 * Listens to PickingTaskCompletedEvent from Picking Service and creates stock movement records.
 * <p>
 * This listener implements event-driven choreography:
 * - Picking task completion triggers stock movement creation
 * - Creates movement from storage location to shipping/picking staging area
 * - Records stock movement for audit and tracking
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PickingTaskCompletedEventListener {
    private static final String PICKING_TASK_COMPLETED_EVENT = "PickingTaskCompletedEvent";

    private final CreateStockMovementCommandHandler createStockMovementCommandHandler;
    private final StockManagementServicePort stockManagementService;
    private final ProductServicePort productServicePort;
    private final LocationViewRepository locationViewRepository;

    @KafkaListener(topics = "picking-events", groupId = "location-management-service", containerFactory = "externalEventKafkaListenerContainerFactory")
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
            String pickedByUserId = extractPickedByUserId(eventData);
            TenantId tenantId = extractTenantId(eventData);

            // Set tenant context
            TenantContext.setTenantId(tenantId);

            log.info("Received PickingTaskCompletedEvent: productCode={}, locationId={}, pickedQuantity={}, tenantId={}", productCode, locationIdString, pickedQuantity,
                    tenantId.getValue());

            // Process event in a separate transactional method
            shouldAcknowledge = processPickingTaskCompletedEvent(productCode, locationIdString, pickedQuantity, pickedByUserId, tenantId);

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

    private String extractPickedByUserId(Map<String, Object> eventData) {
        Object pickedByUserId = eventData.get("pickedByUserId");
        if (pickedByUserId == null) {
            throw new IllegalArgumentException("PickedByUserId not found in event data");
        }
        return pickedByUserId.toString();
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

    @Transactional
    private boolean processPickingTaskCompletedEvent(String productCode, String locationIdString, Integer pickedQuantity, String pickedByUserId, TenantId tenantId) {
        try {
            // 1. Get ProductId from productCode
            com.ccbsa.wms.product.domain.core.valueobject.ProductCode productCodeValue = com.ccbsa.wms.product.domain.core.valueobject.ProductCode.of(productCode);
            ProductServicePort.ProductInfo productInfo =
                    productServicePort.getProductByCode(productCodeValue, tenantId).orElseThrow(() -> new IllegalArgumentException("Product not found for code: " + productCode));
            ProductId productId = ProductId.of(productInfo.getProductId());

            // 2. Find stock item by product and location
            LocationId sourceLocationId = LocationId.of(java.util.UUID.fromString(locationIdString));
            StockManagementServicePort.StockItemQueryResult stockItemResult = stockManagementService.findStockItemByProductAndLocation(productId, sourceLocationId, tenantId);

            if (!stockItemResult.isFound()) {
                log.warn("Stock item not found for product: {} at location: {}. Skipping stock movement creation.", productCode, locationIdString);
                return true; // Acknowledge - stock item might have been moved or doesn't exist
            }

            String stockItemId = stockItemResult.getStockItemId();

            // 3. Determine destination location (shipping/picking staging area)
            // For now, we'll use the source location as destination (represents stock being picked from location)
            // In a full implementation, this would query for a shipping location or use picking list context
            LocationId destinationLocationId = findShippingLocation(tenantId, sourceLocationId);

            // 4. Create stock movement command
            CreateStockMovementCommand command =
                    CreateStockMovementCommand.builder().tenantId(tenantId).stockItemId(stockItemId).productId(productId).sourceLocationId(sourceLocationId)
                            .destinationLocationId(destinationLocationId).quantity(Quantity.of(pickedQuantity)).movementType(MovementType.PICKING_TO_SHIPPING)
                            .reason(MovementReason.PICKING).initiatedBy(UserId.of(pickedByUserId)).build();

            // 5. Create stock movement
            createStockMovementCommandHandler.handle(command);

            log.info("Stock movement created successfully for picking task: productCode={}, locationId={}, quantity={}", productCode, locationIdString, pickedQuantity);
            return true;
        } catch (Exception e) {
            log.error("Error creating stock movement for picking task: productCode={}, locationId={}, error={}", productCode, locationIdString, e.getMessage(), e);
            return false;
        }
    }

    private String extractEventId(Map<String, Object> eventData) {
        Object aggregateId = eventData.get("aggregateId");
        return aggregateId != null ? aggregateId.toString() : "unknown";
    }

    /**
     * Finds a shipping location for the tenant.
     * Falls back to source location if no shipping location is found.
     *
     * @param tenantId         Tenant ID
     * @param sourceLocationId Source location ID (fallback)
     * @return Shipping location ID or source location ID if not found
     */
    private LocationId findShippingLocation(TenantId tenantId, LocationId sourceLocationId) {
        // Try to find a location with type "SHIPPING" or zone "SHIPPING"
        // For now, use source location as fallback (represents stock being picked)
        // In production, this would query for a dedicated shipping location
        try {
            var shippingLocations = locationViewRepository.findByTenantIdAndType(tenantId, "SHIPPING");
            if (!shippingLocations.isEmpty()) {
                return LocationId.of(shippingLocations.get(0).getLocationId().getValue());
            }
        } catch (Exception e) {
            log.debug("No shipping location found, using source location as destination: {}", e.getMessage());
        }
        // Fallback: use source location (represents stock being picked from location)
        return sourceLocationId;
    }
}
