package com.ccbsa.wms.stock.application.service.command;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.stock.application.service.command.dto.CreateStockItemCommand;
import com.ccbsa.wms.stock.application.service.command.dto.CreateStockItemResult;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

/**
 * Command Handler: CreateStockItemCommandHandler
 * <p>
 * Handles creation of new stock items.
 * <p>
 * Responsibilities:
 * - Create StockItem aggregate
 * - Classify stock by expiration date (automatic)
 * - Persist aggregate
 * - Publish StockClassifiedEvent (if classification assigned)
 */
@Component
public class CreateStockItemCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateStockItemCommandHandler.class);
    private final StockItemRepository repository;
    private final StockManagementEventPublisher eventPublisher;

    public CreateStockItemCommandHandler(StockItemRepository repository, StockManagementEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CreateStockItemResult handle(CreateStockItemCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Create aggregate using builder
        StockItem stockItem =
                StockItem.builder().stockItemId(StockItemId.generate()).tenantId(command.getTenantId()).productId(command.getProductId()).locationId(command.getLocationId())
                        .quantity(command.getQuantity()).expirationDate(command.getExpirationDate()).consignmentId(command.getConsignmentId()).build();

        // 3. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = List.copyOf(stockItem.getDomainEvents());

        // 4. Persist aggregate
        StockItem savedStockItem = repository.save(stockItem);

        // 5. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            savedStockItem.clearDomainEvents();
        }

        // 6. Return result
        return CreateStockItemResult.builder().stockItemId(savedStockItem.getId()).classification(savedStockItem.getClassification()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(CreateStockItemCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getProductId() == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (command.getQuantity() == null) {
            throw new IllegalArgumentException("Quantity is required");
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

