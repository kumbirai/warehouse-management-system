package com.ccbsa.wms.stock.application.service.command;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
            eventPublisher.publish(domainEvents);
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
}

