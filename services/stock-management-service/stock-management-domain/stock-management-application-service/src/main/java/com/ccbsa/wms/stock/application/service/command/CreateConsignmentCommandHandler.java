package com.ccbsa.wms.stock.application.service.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.command.dto.CreateConsignmentCommand;
import com.ccbsa.wms.stock.application.service.command.dto.CreateConsignmentResult;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.StockConsignmentRepository;
import com.ccbsa.wms.stock.application.service.port.service.LocationServicePort;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stock.domain.core.exception.InvalidConsignmentReferenceException;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentLineItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: CreateConsignmentCommandHandler
 * <p>
 * Handles creation of new stock consignment.
 * <p>
 * Responsibilities: - Validate consignment reference uniqueness - Validate product codes via ProductServicePort - Create StockConsignment aggregate - Persist aggregate - Publish
 * StockConsignmentReceivedEvent
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CreateConsignmentCommandHandler {
    private final StockConsignmentRepository repository;
    private final StockManagementEventPublisher eventPublisher;
    private final ProductServicePort productServicePort;
    private final LocationServicePort locationServicePort;

    @Transactional
    public CreateConsignmentResult handle(CreateConsignmentCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Validate consignment reference uniqueness
        if (repository.existsByConsignmentReferenceAndTenantId(command.getConsignmentReference(), command.getTenantId())) {
            throw new InvalidConsignmentReferenceException(String.format("Consignment reference '%s' already exists for tenant", command.getConsignmentReference().getValue()));
        }

        // 3. Validate warehouse location exists
        validateWarehouseLocation(command.getWarehouseId(), command.getTenantId());

        // 4. Validate product codes exist
        validateProductCodes(command.getLineItems(), command.getTenantId());

        // 5. Create aggregate using builder
        StockConsignment consignment =
                StockConsignment.builder().consignmentId(ConsignmentId.generate()).tenantId(command.getTenantId()).consignmentReference(command.getConsignmentReference())
                        .warehouseId(command.getWarehouseId()).receivedAt(command.getReceivedAt()).receivedBy(command.getReceivedBy()).lineItems(command.getLineItems()).build();

        // 6. Get domain events from creation BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(consignment.getDomainEvents());
        log.debug("Collected {} domain events from consignment creation", domainEvents.size());

        // 7. Persist aggregate
        repository.save(consignment);
        consignment.clearDomainEvents();

        // 8. Auto-confirm consignment (consignment creation via API represents goods already received and verified)
        // This triggers StockConsignmentConfirmedEvent which creates stock items asynchronously
        log.info("Consignment status before confirm: {}", consignment.getStatus());
        try {
            consignment.confirm();
            log.info("Consignment confirmed successfully. Status after confirm: {}, domain events count: {}", consignment.getStatus(), consignment.getDomainEvents().size());
        } catch (IllegalStateException e) {
            log.error("Failed to confirm consignment: {}. Current status: {}", e.getMessage(), consignment.getStatus(), e);
            throw e;
        }

        // 9. Collect domain events from confirmation BEFORE saving (to avoid any potential event clearing)
        List<DomainEvent<?>> confirmationEvents = new ArrayList<>(consignment.getDomainEvents());
        log.info("Collected {} domain events from consignment confirmation (before save)", confirmationEvents.size());
        for (DomainEvent<?> event : confirmationEvents) {
            log.info("Confirmation event: {}", event.getClass().getSimpleName());
        }
        if (confirmationEvents.isEmpty()) {
            log.warn("WARNING: No confirmation events collected after confirm() call! Consignment status: {}", consignment.getStatus());
        }
        domainEvents.addAll(confirmationEvents);
        consignment.clearDomainEvents();

        // 10. Save confirmed consignment
        repository.save(consignment);

        // 11. Publish all events after transaction commit
        log.info("Publishing {} domain events after transaction commit (creation: {}, confirmation: {})", domainEvents.size(), domainEvents.size() - confirmationEvents.size(),
                confirmationEvents.size());
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
        }

        // 12. Return command-specific result (status is now CONFIRMED)
        return CreateConsignmentResult.builder().consignmentId(consignment.getId()).status(consignment.getStatus()).receivedAt(consignment.getReceivedAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(CreateConsignmentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getConsignmentReference() == null) {
            throw new IllegalArgumentException("ConsignmentReference is required");
        }
        if (command.getWarehouseId() == null) {
            throw new IllegalArgumentException("WarehouseId is required");
        }
        if (command.getReceivedAt() == null) {
            throw new IllegalArgumentException("ReceivedAt is required");
        }
        // LineItems validation is done in DTO constructor - no need to check again
        if (command.getLineItems().isEmpty()) {
            throw new IllegalArgumentException("At least one line item is required");
        }
    }

    /**
     * Validates that warehouse location exists.
     *
     * @param warehouseId Warehouse location ID
     * @param tenantId    Tenant identifier
     * @throws IllegalArgumentException if warehouse location is invalid
     */
    private void validateWarehouseLocation(com.ccbsa.common.domain.valueobject.WarehouseId warehouseId, com.ccbsa.common.domain.valueobject.TenantId tenantId) {
        try {
            LocationId locationId = LocationId.of(warehouseId.getValue());
            LocationServicePort.LocationAvailability availability = locationServicePort.checkLocationAvailability(locationId, Quantity.of(1), tenantId);
            if (!availability.isAvailable()) {
                String reason = availability.getReason();
                // Distinguish between "location not found" and "service unavailable"
                if (reason != null && reason.contains("service unavailable")) {
                    // Service unavailable - this is an infrastructure issue
                    throw new IllegalArgumentException("Warehouse location validation failed: Location service is temporarily unavailable. Please try again later.");
                } else {
                    // Location not found or unavailable for business reasons
                    throw new IllegalArgumentException("Warehouse location not found or unavailable: " + (reason != null ? reason : "Location does not exist"));
                }
            }
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation exceptions
        } catch (RuntimeException e) {
            // Handle circuit breaker exceptions or other runtime exceptions
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Location service unavailable")) {
                throw new IllegalArgumentException("Warehouse location validation failed: Location service is temporarily unavailable. Please try again later.", e);
            } else {
                throw new IllegalArgumentException("Warehouse location validation failed: " + (errorMessage != null ? errorMessage : e.getClass().getSimpleName()), e);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Warehouse location validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that all product codes in line items exist in Product Service.
     *
     * @param lineItems List of line items to validate
     * @param tenantId  Tenant identifier
     * @throws IllegalArgumentException if any product code is invalid
     */
    private void validateProductCodes(List<ConsignmentLineItem> lineItems, com.ccbsa.common.domain.valueobject.TenantId tenantId) {
        for (ConsignmentLineItem lineItem : lineItems) {
            var productInfo = productServicePort.getProductByCode(lineItem.getProductCode(), tenantId);
            if (productInfo.isEmpty()) {
                throw new IllegalArgumentException(String.format("Product with code '%s' not found", lineItem.getProductCode().getValue()));
            }
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
                    log.info("Transaction committed - publishing {} domain events", domainEvents.size());
                    for (DomainEvent<?> event : domainEvents) {
                        log.debug("Publishing event: {}", event.getClass().getSimpleName());
                    }
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

