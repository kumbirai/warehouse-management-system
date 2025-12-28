package com.ccbsa.wms.stock.application.service.command;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.stock.application.service.command.dto.AssignLocationToStockCommand;
import com.ccbsa.wms.stock.application.service.command.dto.AssignLocationToStockResult;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.application.service.port.service.LocationServicePort;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.exception.StockItemNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: AssignLocationToStockCommandHandler
 * <p>
 * Handles assignment of location to stock items.
 * <p>
 * Responsibilities:
 * - Validate stock item exists and is in valid state
 * - Query Location Management Service (synchronous) for location availability
 * - Validate location availability and capacity
 * - Assign location to stock item
 * - Publish LocationAssignedEvent
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AssignLocationToStockCommandHandler {
    private final StockItemRepository stockItemRepository;
    private final LocationServicePort locationServicePort;
    private final StockManagementEventPublisher eventPublisher;

    @Transactional
    public AssignLocationToStockResult handle(AssignLocationToStockCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Get stock item
        StockItem stockItem = stockItemRepository.findById(command.getStockItemId(), command.getTenantId())
                .orElseThrow(() -> new StockItemNotFoundException(String.format("Stock item not found: %s", command.getStockItemId().getValueAsString())));

        // 3. Validate location availability and capacity (synchronous call)
        LocationServicePort.LocationAvailability locationAvailability =
                locationServicePort.checkLocationAvailability(command.getLocationId(), command.getQuantity(), command.getTenantId());

        if (!locationAvailability.isAvailable()) {
            throw new IllegalStateException(String.format("Location %s is not available: %s", command.getLocationId().getValueAsString(), locationAvailability.getReason()));
        }

        if (!locationAvailability.hasCapacity()) {
            throw new IllegalStateException(String.format("Location %s does not have sufficient capacity. Required: %s, Available: %s", command.getLocationId().getValueAsString(),
                    command.getQuantity().getValue(), locationAvailability.getAvailableCapacity() != null ? locationAvailability.getAvailableCapacity().getValue() : "unlimited"));
        }

        // 4. Assign location to stock item
        stockItem.assignLocation(command.getLocationId(), command.getQuantity());

        // 5. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = List.copyOf(stockItem.getDomainEvents());

        // 6. Persist stock item
        StockItem savedStockItem = stockItemRepository.save(stockItem);

        // 7. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            savedStockItem.clearDomainEvents();
        }

        // 8. Return result
        return AssignLocationToStockResult.builder().stockItemId(savedStockItem.getId()).locationId(savedStockItem.getLocationId()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(AssignLocationToStockCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getStockItemId() == null) {
            throw new IllegalArgumentException("StockItemId is required");
        }
        if (command.getLocationId() == null) {
            throw new IllegalArgumentException("LocationId is required");
        }
        if (command.getQuantity() == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
    }

    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     * <p>
     * Events are published using TransactionSynchronizationManager to ensure they are only published after the database transaction has successfully committed. This prevents race
     * conditions where event listeners consume events before the aggregate is visible in the database.
     *
     * @param domainEvents Domain events to publish
     */
    private void publishEventsAfterCommit(List<DomainEvent<?>> domainEvents) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // No active transaction - publish immediately
            log.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        // Register synchronization to publish events after transaction commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    log.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    log.error("Failed to publish domain events after transaction commit", e);
                    // Don't throw - transaction already committed, event publishing failure
                    // should be handled by retry mechanisms or dead letter queue
                }
            }
        });
    }
}

