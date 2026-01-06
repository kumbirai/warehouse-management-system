package com.ccbsa.wms.stock.application.service.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.AllocationType;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockClassification;
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
        List<StockItem> stockItems = findStockItemsForAllocation(command.getTenantId(), command.getProductId(), command.getLocationId(), command.getQuantity().getValue());

        log.debug("Found {} stock item(s) for allocation: productId={}, locationId={}", stockItems.size(), command.getProductId().getValueAsString(),
                command.getLocationId() != null ? command.getLocationId().getValueAsString() : "all locations");

        // 3. Validate sufficient available stock
        // When a specific location is requested, validate against total available (location + unassigned if included)
        // This ensures we can use unassigned stock when location stock is insufficient, but validation is still strict
        int totalAvailable = calculateTotalAvailable(stockItems);

        if (totalAvailable < command.getQuantity().getValue()) {
            // Calculate available at location for better error message
            int availableAtLocation = 0;
            if (command.getLocationId() != null) {
                availableAtLocation = stockItems.stream().filter(item -> item.getLocationId() != null && item.getLocationId().equals(command.getLocationId()))
                        .filter(item -> item.getClassification() != StockClassification.EXPIRED).mapToInt(item -> item.getAvailableQuantity().getValue()).sum();
            }

            log.warn("Insufficient available stock for allocation: productId={}, locationId={}, required={}, available={}, availableAtLocation={}",
                    command.getProductId().getValueAsString(), command.getLocationId() != null ? command.getLocationId().getValueAsString() : "all locations",
                    command.getQuantity().getValue(), totalAvailable, availableAtLocation);
            throw new IllegalStateException(String.format("Insufficient available stock. Required: %d, Available: %d", command.getQuantity().getValue(), totalAvailable));
        }

        // 4. Allocate from stock items (FEFO: earliest expiration first)
        // May need to allocate from multiple stock items if single item doesn't have enough
        AllocationResult allocationResult = allocateFromStockItems(stockItems, command);
        List<StockAllocation> allocations = allocationResult.getAllocations();
        Set<StockItem> updatedStockItems = allocationResult.getUpdatedStockItems();

        // Validate allocations list is not empty (should never happen due to validation, but defensive check)
        if (allocations == null || allocations.isEmpty()) {
            log.error("Allocations list is empty after allocation attempt. This should not happen. productId={}, quantity={}", command.getProductId().getValueAsString(),
                    command.getQuantity().getValue());
            throw new IllegalStateException("Failed to create any allocations. This indicates a bug in the allocation logic.");
        }

        // 5. Collect all domain events BEFORE saving
        List<DomainEvent<?>> allDomainEvents = new ArrayList<>();
        for (StockAllocation allocation : allocations) {
            allDomainEvents.addAll(allocation.getDomainEvents());
        }

        // 6. Persist all aggregates
        StockAllocation firstAllocation = allocations.get(0);
        for (StockAllocation allocation : allocations) {
            allocationRepository.save(allocation);
        }
        // Save all updated stock items (avoid duplicates using a set)
        for (StockItem stockItem : updatedStockItems) {
            stockItemRepository.save(stockItem);
        }

        // 7. Publish events after transaction commit
        if (!allDomainEvents.isEmpty()) {
            publishEventsAfterCommit(allDomainEvents);
            for (StockAllocation allocation : allocations) {
                allocation.clearDomainEvents();
            }
        }

        // 8. Return result (first allocation as primary, but total quantity matches request)
        return AllocateStockResult.builder().allocationId(firstAllocation.getId()).productId(firstAllocation.getProductId()).locationId(firstAllocation.getLocationId())
                .stockItemId(firstAllocation.getStockItemId()).quantity(command.getQuantity()).allocationType(firstAllocation.getAllocationType())
                .referenceId(firstAllocation.getReferenceId() != null ? firstAllocation.getReferenceId().getValue() : null).status(firstAllocation.getStatus())
                .allocatedAt(firstAllocation.getAllocatedAt()).build();
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
     * @param tenantId          Tenant ID
     * @param productId         Product ID
     * @param locationId        Location ID (optional, null for FEFO across all locations)
     * @param requestedQuantity Requested quantity (used to determine if unassigned stock should be included)
     * @return List of stock items sorted by FEFO
     */
    private List<StockItem> findStockItemsForAllocation(com.ccbsa.common.domain.valueobject.TenantId tenantId, ProductId productId, LocationId locationId, int requestedQuantity) {
        List<StockItem> stockItems;

        if (locationId != null) {
            // Specific location requested - find stock items at this location
            List<StockItem> stockItemsAtLocation = stockItemRepository.findByTenantIdAndProductIdAndLocationId(tenantId, productId, locationId);

            // Calculate available quantity at the location (only from items at this location)
            int availableAtLocation =
                    stockItemsAtLocation.stream().filter(item -> item.getClassification() != StockClassification.EXPIRED).mapToInt(item -> item.getAvailableQuantity().getValue())
                            .sum();

            // IMPORTANT: Filter out expired stock items immediately - they cannot be allocated
            stockItems = stockItemsAtLocation.stream().filter(item -> item.getClassification() != StockClassification.EXPIRED).collect(Collectors.toList());

            // Only include unassigned stock if available stock at location is insufficient for the requested quantity
            // This handles the case where stock items are created from consignments but haven't been assigned locations yet
            // (locations are assigned asynchronously via FEFO in the location management service)
            // NOTE: We only include unassigned stock if we can't fulfill from the location alone
            // This ensures validation is strict - if location has insufficient stock, we check unassigned stock
            // but the validation will still fail if total available (location + unassigned) is insufficient
            if (availableAtLocation < requestedQuantity) {
                // Insufficient stock at location - check for unassigned stock items
                List<StockItem> stockItemsWithoutLocation =
                        stockItemRepository.findByTenantIdAndProductId(tenantId, productId).stream().filter(item -> item.getLocationId() == null)
                                .filter(item -> item.getClassification() != StockClassification.EXPIRED).filter(item -> item.getAvailableQuantity().getValue() > 0)
                                .collect(Collectors.toList());

                if (!stockItemsWithoutLocation.isEmpty()) {
                    log.debug("Insufficient stock at location {} (available: {}, required: {}). Found {} unassigned stock item(s) for product: {}. "
                                    + "Unassigned items will be assigned to the requested location during allocation.", locationId.getValueAsString(), availableAtLocation,
                            requestedQuantity, stockItemsWithoutLocation.size(), productId.getValueAsString());
                    stockItems.addAll(stockItemsWithoutLocation);
                } else {
                    log.debug("Insufficient stock at location {} (available: {}, required: {}). No unassigned stock items found for product: {}", locationId.getValueAsString(),
                            availableAtLocation, requestedQuantity, productId.getValueAsString());
                }
            } else {
                log.debug("Found {} stock item(s) at location: {} for product: {} with {} available units (sufficient for requested {} units)", stockItemsAtLocation.size(),
                        locationId.getValueAsString(), productId.getValueAsString(), availableAtLocation, requestedQuantity);
            }
        } else {
            // FEFO: Find all locations with stock, sort by expiration
            // When locationId is null, include stock items without location assignment (for FEFO allocation)
            stockItems = stockItemRepository.findByTenantIdAndProductId(tenantId, productId);
        }

        // Filter by available quantity, exclude expired stock, and sort by expiration (FEFO)
        return stockItems.stream().filter(item -> {
            // Exclude expired stock items - they cannot be allocated
            if (item.getClassification() == StockClassification.EXPIRED) {
                log.debug("Excluding expired stock item from allocation: stockItemId={}, productId={}", item.getId().getValueAsString(), productId.getValueAsString());
                return false;
            }
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
     * Allocates stock from one or more stock items using FEFO.
     * If a single stock item has sufficient quantity, allocates from it.
     * Otherwise, allocates from multiple stock items in FEFO order until required quantity is met.
     *
     * @param stockItems List of stock items (already sorted by FEFO)
     * @param command    Allocation command
     * @return AllocationResult containing allocations and updated stock items
     */
    private AllocationResult allocateFromStockItems(List<StockItem> stockItems, AllocateStockCommand command) {
        List<StockAllocation> allocations = new ArrayList<>();
        Set<StockItem> updatedStockItems = new LinkedHashSet<>();
        int remainingQuantity = command.getQuantity().getValue();

        log.debug("Starting allocation from {} stock item(s) for quantity: {}", stockItems.size(), command.getQuantity().getValue());

        for (StockItem stockItem : stockItems) {
            if (remainingQuantity <= 0) {
                log.debug("Required quantity fully allocated. Remaining: {}", remainingQuantity);
                break;
            }

            // Defensive check: Skip expired stock items - they cannot be allocated
            // This is a safety check in case expired items somehow made it through the filters
            if (stockItem.getClassification() == StockClassification.EXPIRED) {
                log.warn("Skipping expired stock item {} - expired stock should not be in allocation list. This indicates a bug in the filtering logic.",
                        stockItem.getId().getValueAsString());
                continue;
            }

            // Recalculate available quantity (may have changed if this stock item was updated in a previous iteration)
            Quantity available = stockItem.getAvailableQuantity();
            int availableQuantity = available.getValue();

            if (availableQuantity <= 0) {
                log.debug("Skipping stock item {} - no available quantity (total: {}, allocated: {})", stockItem.getId().getValueAsString(),
                        stockItem.getQuantity() != null ? stockItem.getQuantity().getValue() : 0,
                        stockItem.getAllocatedQuantity() != null ? stockItem.getAllocatedQuantity().getValue() : 0);
                continue;
            }

            // Determine how much to allocate from this stock item
            int quantityToAllocate = Math.min(remainingQuantity, availableQuantity);

            // For FEFO allocation (locationId is null), use the stock item's locationId (which may be null for unassigned items)
            // For specific location allocation, use the command's locationId
            LocationId allocationLocationId;

            if (command.getLocationId() != null) {
                // Specific location requested
                allocationLocationId = command.getLocationId();

                // If stock item doesn't have location assigned, assign it during allocation
                // (handles case where stock items are created from consignments but haven't been assigned locations yet)
                if (stockItem.getLocationId() == null) {
                    log.debug("Assigning location {} to stock item {} during allocation", command.getLocationId().getValueAsString(), stockItem.getId().getValueAsString());
                    stockItem.assignLocation(command.getLocationId(), stockItem.getQuantity());
                } else if (!stockItem.getLocationId().equals(command.getLocationId())) {
                    // Stock item is at a different location - skip it
                    log.debug("Skipping stock item {} - not at requested location {}. Current location: {}", stockItem.getId().getValueAsString(),
                            command.getLocationId().getValueAsString(), stockItem.getLocationId().getValueAsString());
                    continue;
                }
            } else {
                // FEFO allocation - use stock item's locationId (may be null for unassigned items)
                // Don't assign locations during FEFO allocation - that should be done separately via FEFO location assignment
                allocationLocationId = stockItem.getLocationId();
            }

            // Create allocation for this stock item
            StockAllocation.Builder allocationBuilder =
                    StockAllocation.builder().stockAllocationId(StockAllocationId.generate()).tenantId(command.getTenantId()).productId(command.getProductId())
                            .locationId(allocationLocationId).stockItemId(stockItem.getId()).quantity(Quantity.of(quantityToAllocate)).allocationType(command.getAllocationType())
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

            // Allocate (publishes domain event)
            allocation.allocate();

            // Update stock item allocated quantity
            Quantity currentAllocated = stockItem.getAllocatedQuantity();
            Quantity newAllocated = currentAllocated.add(Quantity.of(quantityToAllocate));
            stockItem.updateAllocatedQuantity(newAllocated);
            updatedStockItems.add(stockItem);

            allocations.add(allocation);
            remainingQuantity -= quantityToAllocate;

            log.debug("Allocated {} from stock item: {} (location: {}), remaining quantity to allocate: {}", quantityToAllocate, stockItem.getId().getValueAsString(),
                    stockItem.getLocationId() != null ? stockItem.getLocationId().getValueAsString() : "unassigned", remainingQuantity);
        }

        if (remainingQuantity > 0) {
            // This should not happen if validation was correct, but handle it gracefully
            // Calculate current available quantities for better error message
            String availableQuantities = stockItems.stream().map(item -> {
                int available = item.getAvailableQuantity().getValue();
                String location = item.getLocationId() != null ? item.getLocationId().getValueAsString() : "unassigned";
                return String.format("%d@%s", available, location);
            }).collect(Collectors.joining(", ", "[", "]"));

            int totalAllocated = command.getQuantity().getValue() - remainingQuantity;
            log.error("Could not allocate full quantity. Required: {}, Allocated: {}, Remaining: {}, Available quantities: {}", command.getQuantity().getValue(), totalAllocated,
                    remainingQuantity, availableQuantities);

            throw new IllegalStateException(
                    String.format("Could not allocate full quantity. Required: %d, Allocated: %d, Remaining: %d, Available quantities: %s", command.getQuantity().getValue(),
                            totalAllocated, remainingQuantity, availableQuantities));
        }

        log.info("Successfully created {} allocation(s) for quantity: {} from {} stock item(s)", allocations.size(), command.getQuantity().getValue(), updatedStockItems.size());
        return new AllocationResult(allocations, updatedStockItems);
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

    /**
     * Inner class to hold allocation results.
     */
    private static class AllocationResult {
        private final List<StockAllocation> allocations;
        private final Set<StockItem> updatedStockItems;

        AllocationResult(List<StockAllocation> allocations, Set<StockItem> updatedStockItems) {
            this.allocations = allocations;
            this.updatedStockItems = updatedStockItems;
        }

        List<StockAllocation> getAllocations() {
            return allocations;
        }

        Set<StockItem> getUpdatedStockItems() {
            return updatedStockItems;
        }
    }
}

