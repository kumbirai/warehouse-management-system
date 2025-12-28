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

        // 3. Load stock item
        StockItem stockItem = stockItemRepository.findById(allocation.getStockItemId())
                .orElseThrow(() -> new IllegalStateException("Stock item not found: " + allocation.getStockItemId().getValueAsString()));

        // 4. Execute business logic (release allocation)
        allocation.release();

        // 5. Update stock item allocated quantity
        Quantity currentAllocated = stockItem.getAllocatedQuantity();
        Quantity newAllocated = currentAllocated.subtract(allocation.getQuantity());
        stockItem.updateAllocatedQuantity(newAllocated);

        // 6. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(allocation.getDomainEvents());

        // 7. Persist aggregates
        StockAllocation savedAllocation = allocationRepository.save(allocation);
        stockItemRepository.save(stockItem);

        // 8. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            allocation.clearDomainEvents();
        }

        // 9. Return result
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

