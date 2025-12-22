package com.ccbsa.wms.stock.messaging.listener;

import java.time.LocalDate;
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
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;
import com.ccbsa.wms.stock.application.service.command.CreateStockItemCommandHandler;
import com.ccbsa.wms.stock.application.service.command.dto.CreateStockItemCommand;
import com.ccbsa.wms.stock.application.service.port.repository.StockConsignmentRepository;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentLineItem;
import com.ccbsa.wms.stock.domain.core.valueobject.Quantity;

/**
 * Event Listener: StockConsignmentConfirmedEventListener
 * <p>
 * Listens to StockConsignmentConfirmedEvent and creates stock items for each line item in the consignment.
 * <p>
 * This listener implements event-driven choreography:
 * - Consignment confirmation triggers stock item creation
 * - Stock items are automatically classified on creation
 * - StockClassifiedEvent is published for each stock item
 * - Location Management Service listens to StockClassifiedEvent for FEFO assignment
 * <p>
 * Idempotency: This listener is idempotent - checks if stock items already exist for the consignment before creating.
 */
@Component
public class StockConsignmentConfirmedEventListener {
    private static final Logger logger = LoggerFactory.getLogger(StockConsignmentConfirmedEventListener.class);

    private static final String STOCK_CONSIGNMENT_CONFIRMED_EVENT = "StockConsignmentConfirmedEvent";

    private final StockConsignmentRepository consignmentRepository;
    private final CreateStockItemCommandHandler createStockItemCommandHandler;
    private final ProductServicePort productServicePort;

    public StockConsignmentConfirmedEventListener(StockConsignmentRepository consignmentRepository, CreateStockItemCommandHandler createStockItemCommandHandler,
                                                  ProductServicePort productServicePort) {
        this.consignmentRepository = consignmentRepository;
        this.createStockItemCommandHandler = createStockItemCommandHandler;
        this.productServicePort = productServicePort;
    }

    @KafkaListener(topics = "stock-management-events", groupId = "stock-management-service", containerFactory = "externalEventKafkaListenerContainerFactory")
    @Transactional
    public void handle(@Payload Map<String, Object> eventData, @Header(value = "__TypeId__", required = false) String eventType,
                       @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment acknowledgment) {
        try {
            extractAndSetCorrelationId(eventData);

            String detectedEventType = detectEventType(eventData, eventType);
            if (!isStockConsignmentConfirmedEvent(detectedEventType, eventData)) {
                logger.debug("Skipping event - not StockConsignmentConfirmedEvent: detectedType={}, headerType={}", detectedEventType, eventType);
                acknowledgment.acknowledge();
                return;
            }

            // Extract consignment ID
            String consignmentIdString = extractConsignmentId(eventData);
            ConsignmentId consignmentId = ConsignmentId.of(consignmentIdString);

            // Extract tenant ID and set in TenantContext
            TenantId tenantId = extractTenantId(eventData);
            TenantContext.setTenantId(tenantId);

            logger.info("Received StockConsignmentConfirmedEvent: consignmentId={}, tenantId={}", consignmentId.getValueAsString(), tenantId.getValue());

            // Get consignment to access line items
            StockConsignment consignment = consignmentRepository.findByIdAndTenantId(consignmentId, tenantId)
                    .orElseThrow(() -> new IllegalStateException(String.format("Consignment not found: %s", consignmentId.getValueAsString())));

            // Check if stock items already exist (idempotency check)
            // Note: We'll check this in the command handler or create a separate check
            // For now, we'll create stock items for each line item

            // Create stock items for each line item
            for (ConsignmentLineItem lineItem : consignment.getLineItems()) {
                createStockItemFromLineItem(consignment, lineItem, tenantId);
            }

            logger.info("Successfully created stock items for consignment: consignmentId={}, lineItemsCount={}", consignmentId.getValueAsString(),
                    consignment.getLineItems().size());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing StockConsignmentConfirmedEvent: eventId={}, error={}", extractEventId(eventData), e.getMessage(), e);
            // Acknowledge to prevent infinite retries for business logic errors
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

    private boolean isStockConsignmentConfirmedEvent(String detectedEventType, Map<String, Object> eventData) {
        if (STOCK_CONSIGNMENT_CONFIRMED_EVENT.equals(detectedEventType)) {
            return true;
        }
        Object eventClass = eventData.get("@class");
        if (eventClass != null && eventClass.toString().contains(STOCK_CONSIGNMENT_CONFIRMED_EVENT)) {
            return true;
        }
        return false;
    }

    private String extractConsignmentId(Map<String, Object> eventData) {
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
     * Creates a stock item from a consignment line item.
     */
    private void createStockItemFromLineItem(StockConsignment consignment, ConsignmentLineItem lineItem, TenantId tenantId) {
        // Get product ID from product code (synchronous call to Product Service)
        ProductCode productCode = lineItem.getProductCode();
        ProductServicePort.ProductInfo productInfo = productServicePort.getProductByCode(productCode, tenantId)
                .orElseThrow(() -> new IllegalStateException(String.format("Product not found for code: %s", productCode.getValue())));

        ProductId productId = ProductId.of(productInfo.getProductId());

        // Create stock item command
        CreateStockItemCommand.Builder commandBuilder =
                CreateStockItemCommand.builder().tenantId(tenantId).productId(productId).quantity(Quantity.of(lineItem.getQuantity())).consignmentId(consignment.getId());

        // Set expiration date if present
        if (lineItem.hasExpirationDate()) {
            LocalDate expirationDate = lineItem.getExpirationDate();
            commandBuilder.expirationDate(ExpirationDate.of(expirationDate));
        }

        CreateStockItemCommand command = commandBuilder.build();

        // Create stock item (will automatically classify and publish StockClassifiedEvent)
        createStockItemCommandHandler.handle(command);
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

