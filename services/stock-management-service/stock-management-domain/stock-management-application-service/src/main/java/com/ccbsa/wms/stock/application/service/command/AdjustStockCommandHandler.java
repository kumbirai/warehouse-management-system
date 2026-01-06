package com.ccbsa.wms.stock.application.service.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.AdjustmentType;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.stock.application.service.command.dto.AdjustStockCommand;
import com.ccbsa.wms.stock.application.service.command.dto.AdjustStockResult;
import com.ccbsa.wms.stock.application.service.command.dto.CreateStockItemCommand;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.StockAdjustmentRepository;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockAdjustment;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAdjustmentId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: AdjustStockCommandHandler
 * <p>
 * Handles stock adjustment commands.
 * <p>
 * Responsibilities:
 * - Validates command (including authorization for large adjustments)
 * - Gets current quantity from stock item(s)
 * - Validates adjustment doesn't result in negative stock
 * - Creates StockAdjustment aggregate
 * - Updates stock item quantity
 * - Persists aggregates
 * - Publishes domain events after transaction commit
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AdjustStockCommandHandler {
    private static final int AUTHORIZATION_THRESHOLD = 100;

    private final StockAdjustmentRepository adjustmentRepository;
    private final StockItemRepository stockItemRepository;
    private final StockManagementEventPublisher eventPublisher;
    private final CreateStockItemCommandHandler createStockItemCommandHandler;

    @Transactional
    public AdjustStockResult handle(AdjustStockCommand command) {
        log.debug("Handling AdjustStockCommand for product: {}, type: {}, quantity: {}", command.getProductId(), command.getAdjustmentType(), command.getQuantity());

        // 1. Validate command
        validateCommand(command);

        // 2. Get current quantity
        int currentQuantity = getCurrentQuantity(command);
        log.debug("Current quantity for adjustment: productId={}, locationId={}, currentQuantity={}", command.getProductId().getValueAsString(),
                command.getLocationId() != null ? command.getLocationId().getValueAsString() : "all locations", currentQuantity);

        // 3. Validate stock items exist for adjustment
        if (currentQuantity == 0 && command.getAdjustmentType() == AdjustmentType.DECREASE) {
            throw new IllegalStateException(String.format("Cannot decrease stock - no stock items exist for product: %s%s", command.getProductId().getValueAsString(),
                    command.getLocationId() != null ? " at location: " + command.getLocationId().getValueAsString() : ""));
        }

        // 4. Validate adjustment doesn't result in negative stock
        if (command.getAdjustmentType() == AdjustmentType.DECREASE) {
            if (currentQuantity < command.getQuantity().getValue()) {
                throw new IllegalStateException(String.format("Insufficient stock for adjustment. Current: %d, Adjustment: %d", currentQuantity, command.getQuantity().getValue()));
            }
        }

        // 5. Create adjustment aggregate using builder
        // Note: quantityBefore is not set in builder - it will be set by adjust() method
        StockAdjustment adjustment = StockAdjustment.builder().stockAdjustmentId(StockAdjustmentId.generate()).tenantId(command.getTenantId()).productId(command.getProductId())
                .locationId(command.getLocationId()).stockItemId(command.getStockItemId()).adjustmentType(command.getAdjustmentType()).quantity(command.getQuantity())
                .reason(command.getReason()).notes(command.getNotes()).adjustedBy(command.getUserId()).authorizationCode(command.getAuthorizationCode()).build();

        // 6. Adjust (publishes domain event and calculates quantityBefore and quantityAfter)
        adjustment.adjust(Quantity.of(currentQuantity));

        // 7. Update stock item quantity
        updateStockItemQuantity(command, adjustment);

        // 8. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(adjustment.getDomainEvents());

        // 9. Persist adjustment
        StockAdjustment savedAdjustment = adjustmentRepository.save(adjustment);

        // 10. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            adjustment.clearDomainEvents();
        }

        // 11. Return result
        return AdjustStockResult.builder().adjustmentId(savedAdjustment.getId()).productId(savedAdjustment.getProductId()).locationId(savedAdjustment.getLocationId())
                .stockItemId(savedAdjustment.getStockItemId()).adjustmentType(savedAdjustment.getAdjustmentType()).quantity(savedAdjustment.getQuantity())
                .quantityBefore(savedAdjustment.getQuantityBefore() != null ? savedAdjustment.getQuantityBefore().getValue() : 0)
                .quantityAfter(savedAdjustment.getQuantityAfter() != null ? savedAdjustment.getQuantityAfter().getValue() : 0).reason(savedAdjustment.getReason())
                .notes(savedAdjustment.getNotes() != null ? savedAdjustment.getNotes().getValue() : null).adjustedAt(savedAdjustment.getAdjustedAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(AdjustStockCommand command) {
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
        if (command.getAdjustmentType() == null) {
            throw new IllegalArgumentException("AdjustmentType is required");
        }
        if (command.getReason() == null) {
            throw new IllegalArgumentException("AdjustmentReason is required");
        }
        if (command.getUserId() == null) {
            throw new IllegalArgumentException("UserId is required");
        }

        // Validate authorization for large adjustments
        if (command.getQuantity().getValue() >= AUTHORIZATION_THRESHOLD) {
            if (command.getAuthorizationCode() == null || command.getAuthorizationCode().trim().isEmpty()) {
                throw new IllegalArgumentException(String.format("Authorization code required for adjustments >= %d", AUTHORIZATION_THRESHOLD));
            }
        }
    }

    /**
     * Gets current quantity based on command scope (stock item, product/location, or product-wide).
     *
     * @param command Adjustment command
     * @return Current quantity
     */
    private int getCurrentQuantity(AdjustStockCommand command) {
        if (command.getStockItemId() != null) {
            // Adjust specific stock item
            StockItem stockItem = stockItemRepository.findById(command.getStockItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Stock item not found: " + command.getStockItemId().getValueAsString()));
            Quantity quantity = stockItem.getQuantity();
            return quantity != null ? quantity.getValue() : 0;
        } else if (command.getLocationId() != null) {
            // Adjust at product/location level
            // First try to find stock items with the specified location
            int quantityByLocation = stockItemRepository.findByTenantIdAndProductIdAndLocationId(command.getTenantId(), command.getProductId(), command.getLocationId()).stream()
                    .filter(item -> item.getQuantity() != null).mapToInt(item -> item.getQuantity().getValue()).sum();

            // Also check for stock items without location assignment
            // (stock items created from consignments but not yet assigned via FEFO)
            // We include both to allow adjustment of stock at the location AND unassigned stock
            // Exclude expired stock items - they cannot have locations assigned
            int quantityWithoutLocation = stockItemRepository.findByTenantIdAndProductId(command.getTenantId(), command.getProductId()).stream()
                    .filter(item -> item.getLocationId() == null && item.getQuantity() != null)
                    .filter(item -> item.getClassification() != com.ccbsa.common.domain.valueobject.StockClassification.EXPIRED).mapToInt(item -> item.getQuantity().getValue())
                    .sum();

            int totalQuantity = quantityByLocation + quantityWithoutLocation;
            log.debug("Current quantity for adjustment: productId={}, locationId={}, quantityAtLocation={}, quantityWithoutLocation={}, total={}",
                    command.getProductId().getValueAsString(), command.getLocationId().getValueAsString(), quantityByLocation, quantityWithoutLocation, totalQuantity);

            return totalQuantity;
        } else {
            // Adjust at product level (all locations)
            return stockItemRepository.findByTenantIdAndProductId(command.getTenantId(), command.getProductId()).stream().filter(item -> item.getQuantity() != null)
                    .mapToInt(item -> item.getQuantity().getValue()).sum();
        }
    }

    /**
     * Updates stock item quantity based on adjustment.
     * <p>
     * If stockItemId is specified, updates that specific item.
     * Otherwise, updates the first stock item found (for product/location or product-wide adjustments).
     *
     * @param command    Adjustment command
     * @param adjustment Stock adjustment (contains quantityAfter)
     */
    private void updateStockItemQuantity(AdjustStockCommand command, StockAdjustment adjustment) {
        if (command.getStockItemId() != null) {
            // Update specific stock item - use quantityAfter since we're updating a single item
            StockItem stockItem = stockItemRepository.findById(command.getStockItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Stock item not found: " + command.getStockItemId().getValueAsString()));

            stockItem.updateQuantity(adjustment.getQuantityAfter());
            stockItemRepository.save(stockItem);
        } else {
            // Update first stock item found (for product/location or product-wide adjustments)
            // When multiple stock items exist, we add/subtract the adjustment amount to/from the first item
            // rather than setting it to the total quantityAfter (which includes all stock items)
            Optional<StockItem> stockItemOpt;
            if (command.getLocationId() != null) {
                // First try to find stock items with the specified location
                stockItemOpt =
                        stockItemRepository.findByTenantIdAndProductIdAndLocationId(command.getTenantId(), command.getProductId(), command.getLocationId()).stream().findFirst();

                // If no stock items found at the location, try to find stock items without location assignment
                // (stock items created from consignments but not yet assigned via FEFO)
                // Exclude expired stock items - they cannot have locations assigned
                if (stockItemOpt.isEmpty()) {
                    stockItemOpt =
                            stockItemRepository.findByTenantIdAndProductId(command.getTenantId(), command.getProductId()).stream().filter(item -> item.getLocationId() == null)
                                    .filter(item -> item.getClassification() != com.ccbsa.common.domain.valueobject.StockClassification.EXPIRED).findFirst();

                    if (stockItemOpt.isPresent()) {
                        log.debug("Found stock item without location assignment for adjustment. " + "Location will be assigned via FEFO or during the adjustment process.");
                    }
                }
            } else {
                stockItemOpt = stockItemRepository.findByTenantIdAndProductId(command.getTenantId(), command.getProductId()).stream().findFirst();
            }

            if (stockItemOpt.isPresent()) {
                StockItem stockItem = stockItemOpt.get();
                // If stock item doesn't have location assigned and command specifies a location, assign it
                if (command.getLocationId() != null && stockItem.getLocationId() == null) {
                    log.debug("Assigning location {} to stock item {} during adjustment", command.getLocationId().getValueAsString(), stockItem.getId().getValueAsString());
                    Quantity stockItemQuantity = stockItem.getQuantity();
                    if (stockItemQuantity == null || stockItemQuantity.getValue() <= 0) {
                        throw new IllegalStateException(
                                String.format("Cannot assign location to stock item with invalid quantity: stockItemId=%s, quantity=%s", stockItem.getId().getValueAsString(),
                                        stockItemQuantity));
                    }
                    stockItem.assignLocation(command.getLocationId(), stockItemQuantity);
                }

                // Apply adjustment by adding/subtracting the adjustment amount, not setting to total
                if (command.getAdjustmentType() == AdjustmentType.INCREASE) {
                    stockItem.increaseQuantity(command.getQuantity());
                    log.debug("Increased stock item quantity: stockItemId={}, adjustmentAmount={}, newQuantity={}", stockItem.getId().getValueAsString(),
                            command.getQuantity().getValue(), stockItem.getQuantity().getValue());
                } else {
                    // For DECREASE, distribute the decrease across stock items
                    int remainingDecrease = command.getQuantity().getValue();

                    // Get all stock items to distribute the decrease
                    List<StockItem> stockItems;
                    if (command.getLocationId() != null) {
                        stockItems = new ArrayList<>(
                                stockItemRepository.findByTenantIdAndProductIdAndLocationId(command.getTenantId(), command.getProductId(), command.getLocationId()));
                        // Also include unassigned stock items (exclude expired - they cannot have locations assigned)
                        stockItems.addAll(
                                stockItemRepository.findByTenantIdAndProductId(command.getTenantId(), command.getProductId()).stream().filter(item -> item.getLocationId() == null)
                                        .filter(item -> item.getClassification() != com.ccbsa.common.domain.valueobject.StockClassification.EXPIRED).toList());
                    } else {
                        stockItems = new ArrayList<>(stockItemRepository.findByTenantIdAndProductId(command.getTenantId(), command.getProductId()));
                    }

                    // Distribute decrease across stock items (FIFO - first item first)
                    for (StockItem item : stockItems) {
                        if (remainingDecrease <= 0) {
                            break;
                        }

                        Quantity itemQuantity = item.getQuantity();
                        if (itemQuantity == null || itemQuantity.getValue() <= 0) {
                            continue;
                        }

                        int decreaseFromItem = Math.min(remainingDecrease, itemQuantity.getValue());
                        item.decreaseQuantity(Quantity.of(decreaseFromItem));
                        stockItemRepository.save(item);
                        remainingDecrease -= decreaseFromItem;

                        log.debug("Decreased stock item quantity: stockItemId={}, decreaseAmount={}, remainingDecrease={}, newQuantity={}", item.getId().getValueAsString(),
                                decreaseFromItem, remainingDecrease, item.getQuantity().getValue());
                    }

                    if (remainingDecrease > 0) {
                        throw new IllegalStateException(
                                String.format("Insufficient stock for decrease. Requested: %d, Available: %d, Remaining: %d", command.getQuantity().getValue(),
                                        command.getQuantity().getValue() - remainingDecrease, remainingDecrease));
                    }
                }

                stockItemRepository.save(stockItem);
            } else {
                // No stock items found
                if (command.getAdjustmentType() == AdjustmentType.INCREASE) {
                    // For INCREASE adjustments, create a new stock item if none exists
                    log.debug("No stock items found for product: {} at location: {}. Creating new stock item for INCREASE adjustment.", command.getProductId().getValueAsString(),
                            command.getLocationId() != null ? command.getLocationId().getValueAsString() : "all locations");

                    CreateStockItemCommand createCommand =
                            CreateStockItemCommand.builder().tenantId(command.getTenantId()).productId(command.getProductId()).quantity(command.getQuantity())
                                    .locationId(command.getLocationId()).expirationDate(null) // No expiration date for adjustments
                                    .consignmentId(null) // No consignment for manual adjustments
                                    .build();

                    var createResult = createStockItemCommandHandler.handle(createCommand);

                    // Update the adjustment with the newly created stock item ID
                    // Note: We need to reload the adjustment to update stockItemId
                    // Since StockAdjustment is immutable after creation, we'll note this in the adjustment
                    log.debug("Created new stock item: {} for adjustment", createResult.getStockItemId().getValueAsString());
                } else {
                    // For DECREASE adjustments, stock items must exist
                    throw new IllegalStateException(
                            String.format("No stock items found to update for product: %s%s. " + "Stock items must exist before DECREASE adjustments can be made.",
                                    command.getProductId().getValueAsString(),
                                    command.getLocationId() != null ? " at location: " + command.getLocationId().getValueAsString() : ""));
                }
            }
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

