package com.ccbsa.wms.location.application.service.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.location.application.service.command.dto.CancelStockMovementCommand;
import com.ccbsa.wms.location.application.service.command.dto.CancelStockMovementResult;
import com.ccbsa.wms.location.application.service.port.messaging.LocationEventPublisher;
import com.ccbsa.wms.location.application.service.port.repository.StockMovementRepository;
import com.ccbsa.wms.location.domain.core.entity.StockMovement;
import com.ccbsa.wms.location.domain.core.exception.StockMovementNotFoundException;
import com.ccbsa.wms.location.domain.core.valueobject.CancellationReason;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: CancelStockMovementCommandHandler
 * <p>
 * Handles cancellation of a StockMovement aggregate.
 * <p>
 * Responsibilities:
 * - Loads StockMovement aggregate
 * - Executes business logic via aggregate (cancel method)
 * - Persists aggregate changes
 * - Publishes domain events after transaction commit
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CancelStockMovementCommandHandler {
    private final StockMovementRepository repository;
    private final LocationEventPublisher eventPublisher;

    @Transactional
    public CancelStockMovementResult handle(CancelStockMovementCommand command) {
        log.debug("Handling CancelStockMovementCommand for movement: {}", command.getStockMovementId());

        // 1. Validate command
        validateCommand(command);

        // 2. Load aggregate
        StockMovement movement = repository.findByIdAndTenantId(command.getStockMovementId(), command.getTenantId())
                .orElseThrow(() -> new StockMovementNotFoundException("Stock movement not found: " + command.getStockMovementId().getValueAsString()));

        // 3. Execute business logic
        movement.cancel(command.getCancelledBy(), CancellationReason.of(command.getCancellationReason()));

        // 4. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(movement.getDomainEvents());

        // 5. Persist aggregate
        StockMovement savedMovement = repository.save(movement);

        // 6. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            movement.clearDomainEvents();
        }

        // 7. Return result
        return CancelStockMovementResult.builder().stockMovementId(savedMovement.getId()).status(savedMovement.getStatus()).cancelledAt(savedMovement.getCancelledAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(CancelStockMovementCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getStockMovementId() == null) {
            throw new IllegalArgumentException("StockMovementId is required");
        }
        if (command.getCancelledBy() == null) {
            throw new IllegalArgumentException("CancelledBy is required");
        }
        if (command.getCancellationReason() == null || command.getCancellationReason().trim().isEmpty()) {
            throw new IllegalArgumentException("CancellationReason is required");
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

