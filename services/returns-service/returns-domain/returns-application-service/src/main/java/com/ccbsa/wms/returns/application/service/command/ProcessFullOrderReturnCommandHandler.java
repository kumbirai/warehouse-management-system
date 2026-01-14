package com.ccbsa.wms.returns.application.service.command;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.application.service.command.dto.ProcessFullOrderReturnCommand;
import com.ccbsa.wms.returns.application.service.command.dto.ProcessFullOrderReturnResult;
import com.ccbsa.wms.returns.application.service.port.messaging.ReturnsEventPublisher;
import com.ccbsa.wms.returns.application.service.port.repository.ReturnRepository;
import com.ccbsa.wms.returns.application.service.port.service.PickingServicePort;
import com.ccbsa.wms.returns.domain.core.entity.Return;
import com.ccbsa.wms.returns.domain.core.entity.ReturnLineItem;
import com.ccbsa.common.domain.valueobject.ReturnId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: ProcessFullOrderReturnCommandHandler
 * <p>
 * Handles full order returns when entire order is returned.
 * <p>
 * Responsibilities:
 * - Validate order exists and picking is completed
 * - Create Return aggregate via processFullReturn
 * - Persist aggregate
 * - Publish ReturnProcessedEvent after transaction commit
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessFullOrderReturnCommandHandler {
    private final ReturnRepository returnRepository;
    private final ReturnsEventPublisher eventPublisher;
    private final PickingServicePort pickingServicePort;

    @Transactional
    public ProcessFullOrderReturnResult handle(ProcessFullOrderReturnCommand command, TenantId tenantId) {
        log.info("Processing full order return for order: {}, tenant: {}", command.getOrderNumber().getValue(), tenantId.getValue());

        // 1. Validate command
        validateCommand(command);

        // 2. Validate order exists and picking is completed
        validateOrderPickingCompleted(command.getOrderNumber(), tenantId);

        // 3. Create return line items
        List<ReturnLineItem> lineItems = createReturnLineItems(command.getLineItems());

        // 4. Create Return aggregate
        ReturnId returnId = ReturnId.generate();
        Return returnAggregate = Return.processFullReturn(returnId, command.getOrderNumber(), tenantId, lineItems, command.getPrimaryReturnReason(), command.getReturnNotes());

        // 5. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(returnAggregate.getDomainEvents());
        log.debug("Collected {} domain events from return processing", domainEvents.size());

        // 6. Persist aggregate
        returnRepository.save(returnAggregate);
        returnAggregate.clearDomainEvents();

        // 7. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
        }

        // 8. Return result
        return ProcessFullOrderReturnResult.builder().returnId(returnId).orderNumber(command.getOrderNumber()).returnType(returnAggregate.getReturnType())
                .status(returnAggregate.getStatus()).primaryReturnReason(command.getPrimaryReturnReason()).returnedAt(returnAggregate.getReturnedAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(ProcessFullOrderReturnCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getOrderNumber() == null) {
            throw new IllegalArgumentException("OrderNumber is required");
        }
        if (command.getLineItems() == null || command.getLineItems().isEmpty()) {
            throw new IllegalArgumentException("At least one line item is required");
        }
        if (command.getPrimaryReturnReason() == null) {
            throw new IllegalArgumentException("Primary return reason is required");
        }
    }

    /**
     * Validates that order picking is completed.
     *
     * @param orderNumber Order number
     * @param tenantId    Tenant identifier
     * @throws IllegalStateException if order picking is not completed
     */
    private void validateOrderPickingCompleted(OrderNumber orderNumber, TenantId tenantId) {
        boolean isCompleted = pickingServicePort.isOrderPickingCompleted(orderNumber, tenantId);
        if (!isCompleted) {
            throw new IllegalStateException(String.format("Cannot process return for order %s. Picking must be completed first.", orderNumber.getValue()));
        }
    }

    /**
     * Creates return line items from command.
     *
     * @param lineItemCommands List of line item commands
     * @return List of ReturnLineItem entities
     */
    private List<ReturnLineItem> createReturnLineItems(List<ProcessFullOrderReturnCommand.FullReturnLineItemCommand> lineItemCommands) {
        return lineItemCommands.stream()
                .map(lineItemCommand -> ReturnLineItem.createFull(lineItemCommand.getLineItemId(), lineItemCommand.getProductId(), lineItemCommand.getOrderedQuantity(),
                        lineItemCommand.getPickedQuantity(), lineItemCommand.getProductCondition(), lineItemCommand.getReturnReason(), lineItemCommand.getLineNotes()))
                .collect(Collectors.toList());
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
                    log.info("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    log.error("Failed to publish domain events after transaction commit", e);
                }
            }
        });
    }
}
