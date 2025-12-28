package com.ccbsa.wms.stock.application.service.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.AllocationType;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.command.dto.AllocateStockCommand;
import com.ccbsa.wms.stock.application.service.command.dto.AllocateStockResult;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.StockAllocationRepository;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.Notes;
import com.ccbsa.wms.stock.domain.core.valueobject.ReferenceId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: AllocateStockCommandHandler
 * <p>
 * Handles stock allocation commands with FEFO (First-Expired, First-Out) support.
 * <p>
 * Responsibilities:
 * - Validates command
 * - Finds stock items using FEFO algorithm (earliest expiration first)
 * - Validates sufficient available stock
 * - Creates StockAllocation aggregate
 * - Updates stock item allocated quantity
 * - Persists aggregates
 * - Publishes domain events after transaction commit
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AllocateStockCommandHandler {
    private final StockAllocationRepository allocationRepository;
    private final StockItemRepository stockItemRepository;
    private final StockManagementEventPublisher eventPublisher;

    @Transactional
    public AllocateStockResult handle(AllocateStockCommand command) {
        log.debug("Handling AllocateStockCommand for product: {}, quantity: {}", command.getProductId(), command.getQuantity());

        // 1. Validate command
        validateCommand(command);

        // 2. Find stock items for allocation (FEFO if location not specified)
        List<StockItem> stockItems = findStockItemsForAllocation(command.getTenantId(), command.getProductId(), command.getLocationId());

        log.debug("Found {} stock item(s) for allocation: productId={}, locationId={}", stockItems.size(), command.getProductId().getValueAsString(),
                command.getLocationId() != null ? command.getLocationId().getValueAsString() : "all locations");

        // 3. Validate sufficient available stock
        int totalAvailable = calculateTotalAvailable(stockItems);
        if (totalAvailable < command.getQuantity().getValue()) {
            log.warn("Insufficient available stock for allocation: productId={}, locationId={}, required={}, available={}", command.getProductId().getValueAsString(),
                    command.getLocationId() != null ? command.getLocationId().getValueAsString() : "all locations", command.getQuantity().getValue(), totalAvailable);
            throw new IllegalStateException(String.format("Insufficient available stock. Required: %d, Available: %d", command.getQuantity().getValue(), totalAvailable));
        }

        // 4. Select stock item for allocation (FEFO: earliest expiration first)
        StockItem selectedStockItem = selectStockItemForAllocation(stockItems, command.getQuantity().getValue());

        // 5. Create allocation aggregate using builder
        StockAllocation.Builder allocationBuilder =
                StockAllocation.builder().stockAllocationId(StockAllocationId.generate()).tenantId(command.getTenantId()).productId(command.getProductId())
                        .locationId(command.getLocationId()).stockItemId(selectedStockItem.getId()).quantity(command.getQuantity()).allocationType(command.getAllocationType())
                        .allocatedBy(command.getUserId());

        // Set referenceId if provided
        if (command.getReferenceId() != null && !command.getReferenceId().trim().isEmpty()) {
            allocationBuilder.referenceId(ReferenceId.of(command.getReferenceId()));
        }

        // Set notes if provided
        if (command.getNotes() != null) {
            allocationBuilder.notes(Notes.ofNullable(command.getNotes()));
        }

        StockAllocation allocation = allocationBuilder.build();

        // 6. Allocate (publishes domain event)
        allocation.allocate();

        // 7. Update stock item allocated quantity
        Quantity currentAllocated = selectedStockItem.getAllocatedQuantity();
        Quantity newAllocated = currentAllocated.add(command.getQuantity());
        selectedStockItem.updateAllocatedQuantity(newAllocated);

        // 8. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(allocation.getDomainEvents());

        // 9. Persist aggregates
        StockAllocation savedAllocation = allocationRepository.save(allocation);
        stockItemRepository.save(selectedStockItem);

        // 10. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            allocation.clearDomainEvents();
        }

        // 11. Return result
        return AllocateStockResult.builder().allocationId(savedAllocation.getId()).productId(savedAllocation.getProductId()).locationId(savedAllocation.getLocationId())
                .stockItemId(savedAllocation.getStockItemId()).quantity(savedAllocation.getQuantity()).allocationType(savedAllocation.getAllocationType())
                .referenceId(savedAllocation.getReferenceId() != null ? savedAllocation.getReferenceId().getValue() : null).status(savedAllocation.getStatus())
                .allocatedAt(savedAllocation.getAllocatedAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(AllocateStockCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getProductId() == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (command.getQuantity() == null || command.getQuantity().getValue() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (command.getAllocationType() == null) {
            throw new IllegalArgumentException("AllocationType is required");
        }
        if (command.getAllocationType() == AllocationType.PICKING_ORDER && (command.getReferenceId() == null || command.getReferenceId().trim().isEmpty())) {
            throw new IllegalArgumentException("ReferenceId is required for PICKING_ORDER allocation");
        }
        if (command.getUserId() == null) {
            throw new IllegalArgumentException("UserId is required");
        }
    }

    /**
     * Finds stock items for allocation using FEFO principles.
     * <p>
     * FEFO Algorithm:
     * 1. Query stock items by product (and location if specified)
     * 2. Filter by available quantity (total - allocated)
     * 3. Sort by expiration date (earliest first)
     * 4. Return items with sufficient available stock
     *
     * @param tenantId   Tenant ID
     * @param productId  Product ID
     * @param locationId Location ID (optional, null for FEFO across all locations)
     * @return List of stock items sorted by FEFO
     */
    private List<StockItem> findStockItemsForAllocation(com.ccbsa.common.domain.valueobject.TenantId tenantId, ProductId productId, LocationId locationId) {
        List<StockItem> stockItems;

        if (locationId != null) {
            // Specific location requested
            stockItems = stockItemRepository.findByTenantIdAndProductIdAndLocationId(tenantId, productId, locationId);

            // If no stock items found at the specified location, also check for stock items without location assignment
            // (stock items created from consignments but not yet assigned via FEFO)
            if (stockItems.isEmpty()) {
                log.debug("No stock items found at location: {} for product: {}. " + "Checking for stock items without location assignment.", locationId.getValueAsString(),
                        productId.getValueAsString());

                List<StockItem> stockItemsWithoutLocation =
                        stockItemRepository.findByTenantIdAndProductId(tenantId, productId).stream().filter(item -> item.getLocationId() == null).collect(Collectors.toList());

                if (!stockItemsWithoutLocation.isEmpty()) {
                    log.debug("Found {} stock item(s) without location assignment for product: {}. " + "These will be considered for allocation.", stockItemsWithoutLocation.size(),
                            productId.getValueAsString());
                    stockItems = stockItemsWithoutLocation;
                }
            }
        } else {
            // FEFO: Find all locations with stock, sort by expiration
            stockItems = stockItemRepository.findByTenantIdAndProductId(tenantId, productId);
        }

        // Filter by available quantity and sort by expiration (FEFO)
        return stockItems.stream().filter(item -> {
            // Calculate available quantity (total - allocated)
            Quantity available = item.getAvailableQuantity();
            return available.getValue() > 0;
        }).sorted(createFEFOComparator()).collect(Collectors.toList());
    }

    /**
     * Calculates total available quantity across all stock items.
     *
     * @param stockItems List of stock items
     * @return Total available quantity
     */
    private int calculateTotalAvailable(List<StockItem> stockItems) {
        return stockItems.stream().mapToInt(item -> item.getAvailableQuantity().getValue()).sum();
    }

    /**
     * Selects stock item for allocation using FEFO.
     * Prefers earliest expiring stock that has sufficient available quantity.
     *
     * @param stockItems       List of stock items (already sorted by FEFO)
     * @param requiredQuantity Required quantity
     * @return Selected stock item
     * @throws IllegalStateException if no stock item found with sufficient available quantity
     */
    private StockItem selectStockItemForAllocation(List<StockItem> stockItems, int requiredQuantity) {
        for (StockItem item : stockItems) {
            Quantity available = item.getAvailableQuantity();
            if (available.getValue() >= requiredQuantity) {
                return item;
            }
        }
        throw new IllegalStateException("No stock item found with sufficient available quantity");
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

    /**
     * Creates a comparator for FEFO sorting (earliest expiration first).
     * <p>
     * Items with null expiration dates are sorted last.
     *
     * @return FEFO comparator
     */
    private Comparator<StockItem> createFEFOComparator() {
        return (a, b) -> {
            // Both have expiration dates
            if (a.getExpirationDate() != null && b.getExpirationDate() != null) {
                return a.getExpirationDate().getValue().compareTo(b.getExpirationDate().getValue());
            }
            // Only a has expiration date (a comes first)
            if (a.getExpirationDate() != null) {
                return -1;
            }
            // Only b has expiration date (b comes first)
            if (b.getExpirationDate() != null) {
                return 1;
            }
            // Neither has expiration date (maintain order)
            return 0;
        };
    }
}

