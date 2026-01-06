package com.ccbsa.wms.stock.application.service.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.stock.application.service.command.dto.ReleaseStockAllocationCommand;
import com.ccbsa.wms.stock.application.service.command.dto.ReleaseStockAllocationResult;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.StockAllocationRepository;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.exception.StockAllocationNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: ReleaseStockAllocationCommandHandler
 * <p>
 * Handles stock allocation release commands.
 * <p>
 * Responsibilities:
 * - Loads StockAllocation aggregate
 * - Executes business logic via aggregate (release method)
 * - Updates stock item allocated quantity
 * - Persists aggregates
 * - Publishes domain events after transaction commit
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReleaseStockAllocationCommandHandler {
    private final StockAllocationRepository allocationRepository;
    private final StockItemRepository stockItemRepository;
    private final StockManagementEventPublisher eventPublisher;

    @Transactional
    public ReleaseStockAllocationResult handle(ReleaseStockAllocationCommand command) {
        log.debug("Handling ReleaseStockAllocationCommand for allocation: {}", command.getAllocationId());

        // 1. Validate command
        validateCommand(command);

        // 2. Load allocation aggregate
        StockAllocation allocation = allocationRepository.findByIdAndTenantId(command.getAllocationId(), command.getTenantId())
                .orElseThrow(() -> new StockAllocationNotFoundException("Stock allocation not found: " + command.getAllocationId().getValueAsString()));

        // 3. Validate allocation can be released (check status before releasing)
        if (allocation.getStatus() != com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus.ALLOCATED) {
            throw new IllegalStateException(String.format("Cannot release allocation in status: %s. Only ALLOCATED allocations can be released.", allocation.getStatus()));
        }

        // 4. Load stock item
        StockItem stockItem = stockItemRepository.findById(allocation.getStockItemId())
                .orElseThrow(() -> new IllegalStateException("Stock item not found: " + allocation.getStockItemId().getValueAsString()));

        // 5. Execute business logic (release allocation)
        allocation.release();

        // 6. Update stock item allocated quantity
        Quantity currentAllocated = stockItem.getAllocatedQuantity();
        Quantity allocationQuantity = allocation.getQuantity();

        // Subtract allocation quantity from stock item's allocated quantity
        // Ensure we don't go negative (safeguard against data integrity issues)
        int expectedNewAllocated = currentAllocated.getValue() - allocationQuantity.getValue();
        int newAllocatedValue = Math.max(0, expectedNewAllocated);
        Quantity newAllocated = Quantity.of(newAllocatedValue);

        if (expectedNewAllocated < 0) {
            log.warn("Stock item allocated quantity would go negative. Clamping to 0. " + "This may indicate a data integrity issue. AllocationId: {}, StockItemId: {}, "
                            + "Current allocated: {}, Allocation quantity: {}", allocation.getId().getValueAsString(), stockItem.getId().getValueAsString(),
                    currentAllocated.getValue(),
                    allocationQuantity.getValue());
        }

        stockItem.updateAllocatedQuantity(newAllocated);

        // 7. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(allocation.getDomainEvents());

        // 8. Persist aggregates
        StockAllocation savedAllocation = allocationRepository.save(allocation);
        stockItemRepository.save(stockItem);

        // 9. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            allocation.clearDomainEvents();
        }

        // 10. Return result
        return ReleaseStockAllocationResult.builder().allocationId(savedAllocation.getId()).status(savedAllocation.getStatus()).releasedAt(savedAllocation.getReleasedAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(ReleaseStockAllocationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getAllocationId() == null) {
            throw new IllegalArgumentException("AllocationId is required");
        }
    }

    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     *
     * @param domainEvents Domain events to publish
     */
    private void publishEventsAfterCommit(List<DomainEvent<?>> domainEvents) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    log.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    log.error("Failed to publish domain events after transaction commit", e);
                }
            }
        });
    }
}

