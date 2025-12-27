package com.ccbsa.wms.stock.application.service.command;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.stock.application.service.command.dto.CreateConsignmentCommand;
import com.ccbsa.wms.stock.application.service.command.dto.CreateConsignmentResult;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.StockConsignmentRepository;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stock.domain.core.exception.InvalidConsignmentReferenceException;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentLineItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentStatus;

/**
 * Command Handler: CreateConsignmentCommandHandler
 * <p>
 * Handles creation of new stock consignment.
 * <p>
 * Responsibilities: - Validate consignment reference uniqueness - Validate product codes via ProductServicePort - Create StockConsignment aggregate - Persist aggregate - Publish
 * StockConsignmentReceivedEvent
 */
@Component
public class CreateConsignmentCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateConsignmentCommandHandler.class);
    private final StockConsignmentRepository repository;
    private final StockManagementEventPublisher eventPublisher;
    private final ProductServicePort productServicePort;

    public CreateConsignmentCommandHandler(StockConsignmentRepository repository, StockManagementEventPublisher eventPublisher, ProductServicePort productServicePort) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.productServicePort = productServicePort;
    }

    @Transactional
    public CreateConsignmentResult handle(CreateConsignmentCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Validate consignment reference uniqueness
        if (repository.existsByConsignmentReferenceAndTenantId(command.getConsignmentReference(), command.getTenantId())) {
            throw new InvalidConsignmentReferenceException(String.format("Consignment reference '%s' already exists for tenant", command.getConsignmentReference().getValue()));
        }

        // 3. Validate product codes exist
        validateProductCodes(command.getLineItems(), command.getTenantId());

        // 4. Create aggregate using builder
        StockConsignment consignment =
                StockConsignment.builder().consignmentId(ConsignmentId.generate()).tenantId(command.getTenantId()).consignmentReference(command.getConsignmentReference())
                        .warehouseId(command.getWarehouseId()).receivedAt(command.getReceivedAt()).receivedBy(command.getReceivedBy()).lineItems(command.getLineItems()).build();

        // 5. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = List.copyOf(consignment.getDomainEvents());

        // 6. Persist aggregate
        repository.save(consignment);

        // 7. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            consignment.clearDomainEvents();
        }

        // 8. Return command-specific result
        return CreateConsignmentResult.builder().consignmentId(consignment.getId()).status(ConsignmentStatus.RECEIVED).receivedAt(consignment.getReceivedAt()).build();
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
        if (command.getLineItems() == null || command.getLineItems().isEmpty()) {
            throw new IllegalArgumentException("At least one line item is required");
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
            logger.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        // Register synchronization to publish events after transaction commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    logger.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    logger.error("Failed to publish domain events after transaction commit", e);
                    // Don't throw - transaction already committed, event publishing failure
                    // should be handled by retry mechanisms or dead letter queue
                }
            }
        });
    }
}

