package com.ccbsa.wms.stock.application.service.command;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.stock.application.service.command.dto.UpdateStockItemExpirationDateCommand;
import com.ccbsa.wms.stock.application.service.command.dto.UpdateStockItemExpirationDateResult;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.exception.StockItemNotFoundException;

/**
 * Command Handler: UpdateStockItemExpirationDateCommandHandler
 * <p>
 * Handles updating stock item expiration date and reclassification.
 * <p>
 * Responsibilities:
 * - Validate stock item exists
 * - Update expiration date
 * - Trigger reclassification (automatic)
 * - Publish StockClassifiedEvent if classification changes
 */
@Component
public class UpdateStockItemExpirationDateCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(UpdateStockItemExpirationDateCommandHandler.class);
    private final StockItemRepository repository;
    private final StockManagementEventPublisher eventPublisher;

    public UpdateStockItemExpirationDateCommandHandler(StockItemRepository repository, StockManagementEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UpdateStockItemExpirationDateResult handle(UpdateStockItemExpirationDateCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Get stock item
        StockItem stockItem = repository.findById(command.getStockItemId(), command.getTenantId())
                .orElseThrow(() -> new StockItemNotFoundException(String.format("Stock item not found: %s", command.getStockItemId().getValueAsString())));

        // 3. Update expiration date (triggers reclassification)
        stockItem.updateExpirationDate(command.getExpirationDate());

        // 4. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = List.copyOf(stockItem.getDomainEvents());

        // 5. Persist stock item
        StockItem savedStockItem = repository.save(stockItem);

        // 6. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            savedStockItem.clearDomainEvents();
        }

        // 7. Return result
        return UpdateStockItemExpirationDateResult.builder().stockItemId(savedStockItem.getId()).classification(savedStockItem.getClassification()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(UpdateStockItemExpirationDateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getStockItemId() == null) {
            throw new IllegalArgumentException("StockItemId is required");
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

