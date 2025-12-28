package com.ccbsa.wms.location.application.service.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.wms.location.application.service.command.dto.CreateStockMovementCommand;
import com.ccbsa.wms.location.application.service.command.dto.CreateStockMovementResult;
import com.ccbsa.wms.location.application.service.port.messaging.LocationEventPublisher;
import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.application.service.port.repository.StockMovementRepository;
import com.ccbsa.wms.location.application.service.port.service.StockManagementServicePort;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.entity.StockMovement;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: CreateStockMovementCommandHandler
 * <p>
 * Handles creation of new StockMovement aggregate.
 * <p>
 * Responsibilities:
 * - Validates source and destination locations
 * - Validates stock item via Stock Management Service
 * - Creates StockMovement aggregate
 * - Persists aggregate
 * - Publishes domain events after transaction commit
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CreateStockMovementCommandHandler {
    private final StockMovementRepository repository;
    private final LocationRepository locationRepository;
    private final LocationEventPublisher eventPublisher;
    private final StockManagementServicePort stockManagementService;

    @Transactional
    public CreateStockMovementResult handle(CreateStockMovementCommand command) {
        log.debug("Handling CreateStockMovementCommand for tenant: {}", command.getTenantId());

        // 1. Validate command
        validateCommand(command);

        // 2. Validate source location exists and is accessible
        locationRepository.findByIdAndTenantId(command.getSourceLocationId(), command.getTenantId())
                .orElseThrow(() -> new LocationNotFoundException("Source location not found: " + command.getSourceLocationId().getValueAsString()));

        // 3. Validate destination location exists and has capacity
        Location destinationLocation = locationRepository.findByIdAndTenantId(command.getDestinationLocationId(), command.getTenantId())
                .orElseThrow(() -> new LocationNotFoundException("Destination location not found: " + command.getDestinationLocationId().getValueAsString()));

        if (!destinationLocation.canAccommodate(convertQuantityToBigDecimal(command.getQuantity()))) {
            throw new IllegalStateException("Destination location does not have sufficient capacity for quantity: " + command.getQuantity().getValue());
        }

        // 4. Validate stock item exists and has sufficient quantity via Stock Management Service
        String stockItemId = command.getStockItemId();

        // If stockItemId is not provided, find it by productId and sourceLocationId
        if (stockItemId == null || stockItemId.trim().isEmpty()) {
            // First try to find stock item by productId and sourceLocationId
            StockManagementServicePort.StockItemQueryResult stockItemQuery =
                    stockManagementService.findStockItemByProductAndLocation(command.getProductId(), command.getSourceLocationId(), command.getTenantId());

            if (!stockItemQuery.isFound()) {
                // If not found by location, try to find by productId only (stock items without locations)
                // This handles the case where stock items are created from consignments but haven't been assigned locations yet
                StockManagementServicePort.StockItemQueryResult stockItemQueryByProduct =
                        stockManagementService.findStockItemByProduct(command.getProductId(), command.getTenantId());

                if (!stockItemQueryByProduct.isFound()) {
                    throw new IllegalArgumentException(String.format("No stock item found for product: %s. " + "Please ensure stock exists for this product. "
                            + "If stock was recently created from a consignment, it may need location assignment first.", command.getProductId().getValueAsString()));
                }

                // Found stock item without location - use it but log a warning
                log.warn("Found stock item {} for product {} without location assignment. " + "Stock movement will proceed, but location should be assigned via FEFO.",
                        stockItemQueryByProduct.getStockItemId(), command.getProductId().getValueAsString());
                stockItemId = stockItemQueryByProduct.getStockItemId();
            } else {
                stockItemId = stockItemQuery.getStockItemId();
            }
        }

        // Validate stock item exists and has sufficient quantity
        StockManagementServicePort.StockItemValidationResult stockItemValidation =
                stockManagementService.validateStockItem(stockItemId, command.getQuantity(), command.getTenantId());

        if (!stockItemValidation.isValid()) {
            throw new IllegalArgumentException(String.format("Stock item validation failed: %s. " + "Please ensure the stock item exists and has sufficient available quantity.",
                    stockItemValidation.getErrorMessage()));
        }

        // Use product ID from validation result or command
        ProductId productId = stockItemValidation.getProductId() != null ? stockItemValidation.getProductId() : command.getProductId();

        if (productId == null) {
            throw new IllegalArgumentException(
                    "ProductId is required. " + "Unable to determine productId from stock item validation. " + "Please provide productId in the request.");
        }

        // 5. Create aggregate using builder
        StockMovement movement =
                StockMovement.builder().stockMovementId(StockMovementId.generate()).tenantId(command.getTenantId()).stockItemId(StockItemId.of(stockItemId)).productId(productId)
                        .sourceLocationId(command.getSourceLocationId()).destinationLocationId(command.getDestinationLocationId()).quantity(command.getQuantity())
                        .movementType(command.getMovementType()).reason(command.getReason()).initiatedBy(command.getInitiatedBy()).build();

        // 6. Get domain events BEFORE saving (save() returns a new instance without events)
        List<DomainEvent<?>> domainEvents = new ArrayList<>(movement.getDomainEvents());

        // 7. Persist aggregate
        StockMovement savedMovement = repository.save(movement);

        // 8. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            movement.clearDomainEvents();
        }

        // 9. Return result
        return CreateStockMovementResult.builder().stockMovementId(savedMovement.getId()).status(savedMovement.getStatus()).initiatedAt(savedMovement.getInitiatedAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(CreateStockMovementCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        // StockItemId is optional - if not provided, will be found by productId and sourceLocationId
        // ProductId and sourceLocationId are required for stock movements
        if (command.getSourceLocationId() == null) {
            throw new IllegalArgumentException("SourceLocationId is required");
        }
        if (command.getDestinationLocationId() == null) {
            throw new IllegalArgumentException("DestinationLocationId is required");
        }
        if (command.getSourceLocationId().equals(command.getDestinationLocationId())) {
            throw new IllegalArgumentException("Source and destination locations must be different");
        }
        if (command.getQuantity() == null || !command.getQuantity().isPositive()) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (command.getMovementType() == null) {
            throw new IllegalArgumentException("MovementType is required");
        }
        if (command.getReason() == null) {
            throw new IllegalArgumentException("MovementReason is required");
        }
        if (command.getInitiatedBy() == null) {
            throw new IllegalArgumentException("InitiatedBy is required");
        }
    }

    /**
     * Converts Quantity to BigDecimal for location capacity checks.
     */
    private java.math.BigDecimal convertQuantityToBigDecimal(Quantity quantity) {
        return java.math.BigDecimal.valueOf(quantity.getValue());
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

