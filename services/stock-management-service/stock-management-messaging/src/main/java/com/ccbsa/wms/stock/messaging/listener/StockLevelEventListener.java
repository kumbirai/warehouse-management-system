package com.ccbsa.wms.stock.messaging.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.BigDecimalQuantity;
import com.ccbsa.common.domain.valueobject.MaximumQuantity;
import com.ccbsa.common.domain.valueobject.MinimumQuantity;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.stock.application.service.command.GenerateRestockRequestCommandHandler;
import com.ccbsa.wms.stock.application.service.command.dto.GenerateRestockRequestCommand;
import com.ccbsa.wms.stock.domain.core.event.StockLevelBelowMinimumEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Listener: StockLevelEventListener
 * <p>
 * Listens to StockLevelBelowMinimumEvent and generates restock requests.
 * <p>
 * This listener implements event-driven choreography:
 * - Stock level threshold breach triggers restock request generation
 * - Only generates if auto-restock is enabled
 * - Deduplication ensures only one active request per product
 * <p>
 * Idempotency: This listener is idempotent - checks for existing active requests before creating.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StockLevelEventListener {

    private final GenerateRestockRequestCommandHandler generateRestockRequestCommandHandler;

    @KafkaListener(topics = "stock-management-events", groupId = "stock-management-service-restock-request-generation", containerFactory =
            "externalEventKafkaListenerContainerFactory")
    public void handleStockLevelBelowMinimum(Object event) {
        if (!(event instanceof StockLevelBelowMinimumEvent)) {
            return; // Skip non-relevant events
        }

        StockLevelBelowMinimumEvent stockLevelEvent = (StockLevelBelowMinimumEvent) event;
        log.info("Received StockLevelBelowMinimumEvent: productId={}, locationId={}, currentQuantity={}, minimumQuantity={}, enableAutoRestock={}",
                stockLevelEvent.getProductId().getValueAsString(), stockLevelEvent.getLocationId() != null ? stockLevelEvent.getLocationId().getValueAsString() : "all locations",
                stockLevelEvent.getCurrentQuantity(), stockLevelEvent.getMinimumQuantity(), stockLevelEvent.isEnableAutoRestock());

        // Only generate restock request if auto-restock is enabled
        if (!stockLevelEvent.isEnableAutoRestock()) {
            log.debug("Auto-restock is disabled for product: {}. Skipping restock request generation.", stockLevelEvent.getProductId().getValueAsString());
            return;
        }

        try {
            // Set tenant context for the command handler
            TenantContext.setTenantId(stockLevelEvent.getTenantId());

            // Convert BigDecimal to BigDecimalQuantity
            BigDecimalQuantity currentQuantity = BigDecimalQuantity.of(stockLevelEvent.getCurrentQuantity());
            MinimumQuantity minimumQuantity = MinimumQuantity.of(stockLevelEvent.getMinimumQuantity());
            // Note: StockLevelBelowMinimumEvent doesn't include maximumQuantity
            // We'll calculate requested quantity without it (uses default: 2x minimum)

            // Create command (maximumQuantity is optional and defaults to null if not set)
            GenerateRestockRequestCommand.GenerateRestockRequestCommandBuilder commandBuilder = GenerateRestockRequestCommand.builder()
                    .tenantId(stockLevelEvent.getTenantId())
                    .productId(stockLevelEvent.getProductId())
                    .locationId(stockLevelEvent.getLocationId())
                    .currentQuantity(currentQuantity)
                    .minimumQuantity(minimumQuantity);
            // maximumQuantity is intentionally not set - will default to null
            GenerateRestockRequestCommand command = commandBuilder.build();

            // Generate restock request
            generateRestockRequestCommandHandler.handle(command);

            log.info("Restock request generated successfully for product: {}", stockLevelEvent.getProductId().getValueAsString());
        } catch (com.ccbsa.wms.stock.domain.core.exception.DuplicateRestockRequestException e) {
            log.debug("Duplicate restock request detected for product: {}. This is expected and handled gracefully.", stockLevelEvent.getProductId().getValueAsString());
        } catch (Exception e) {
            log.error("Error generating restock request for product: {}", stockLevelEvent.getProductId().getValueAsString(), e);
            // Don't throw - allow event processing to continue
        } finally {
            // Clear tenant context
            TenantContext.clear();
        }
    }
}
