package com.ccbsa.wms.returns.application.service.command;

import java.time.Instant;
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
import com.ccbsa.wms.returns.application.service.command.dto.HandlePartialOrderAcceptanceCommand;
import com.ccbsa.wms.returns.application.service.command.dto.HandlePartialOrderAcceptanceResult;
import com.ccbsa.wms.returns.application.service.port.messaging.ReturnsEventPublisher;
import com.ccbsa.wms.returns.application.service.port.repository.ReturnRepository;
import com.ccbsa.wms.returns.application.service.port.service.PickingServicePort;
import com.ccbsa.wms.returns.domain.core.entity.Return;
import com.ccbsa.wms.returns.domain.core.entity.ReturnLineItem;
import com.ccbsa.wms.returns.domain.core.valueobject.CustomerSignature;
import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.common.domain.valueobject.ReturnLineItemId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: HandlePartialOrderAcceptanceCommandHandler
 * <p>
 * Handles partial order acceptance when customer accepts only part of their order.
 * <p>
 * Responsibilities:
 * - Validate order exists and picking is completed
 * - Create Return aggregate via initiatePartialReturn
 * - Persist aggregate
 * - Publish ReturnInitiatedEvent after transaction commit
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HandlePartialOrderAcceptanceCommandHandler {
    private final ReturnRepository returnRepository;
    private final ReturnsEventPublisher eventPublisher;
    private final PickingServicePort pickingServicePort;

    @Transactional
    public HandlePartialOrderAcceptanceResult handle(HandlePartialOrderAcceptanceCommand command, TenantId tenantId) {
        log.info("Handling partial order acceptance for order: {}, tenant: {}", command.getOrderNumber().getValue(), tenantId.getValue());

        // 1. Validate command
        validateCommand(command);

        // 2. Validate order exists and picking is completed
        validateOrderPickingCompleted(command.getOrderNumber(), tenantId);

        // 3. Create return line items
        List<ReturnLineItem> lineItems = createReturnLineItems(command.getLineItems());

        // 4. Create customer signature
        CustomerSignature customerSignature = CustomerSignature.of(command.getSignatureData(), command.getSignedAt());

        // 5. Create Return aggregate
        ReturnId returnId = ReturnId.generate();
        Return returnAggregate = Return.initiatePartialReturn(returnId, command.getOrderNumber(), tenantId, lineItems, customerSignature);

        // 6. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(returnAggregate.getDomainEvents());
        log.debug("Collected {} domain events from return creation", domainEvents.size());

        // 7. Persist aggregate
        returnRepository.save(returnAggregate);
        returnAggregate.clearDomainEvents();

        // 8. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
        }

        // 9. Return result
        return HandlePartialOrderAcceptanceResult.builder().returnId(returnId).orderNumber(command.getOrderNumber()).returnType(returnAggregate.getReturnType())
                .status(returnAggregate.getStatus()).returnedAt(returnAggregate.getReturnedAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(HandlePartialOrderAcceptanceCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getOrderNumber() == null) {
            throw new IllegalArgumentException("OrderNumber is required");
        }
        if (command.getLineItems() == null || command.getLineItems().isEmpty()) {
            throw new IllegalArgumentException("At least one line item is required");
        }
        if (command.getSignatureData() == null || command.getSignatureData().trim().isEmpty()) {
            throw new IllegalArgumentException("Signature data is required");
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
    private List<ReturnLineItem> createReturnLineItems(List<HandlePartialOrderAcceptanceCommand.PartialReturnLineItemCommand> lineItemCommands) {
        return lineItemCommands.stream()
                .map(lineItemCommand -> ReturnLineItem.createPartial(lineItemCommand.getLineItemId(), lineItemCommand.getProductId(), lineItemCommand.getOrderedQuantity(),
                        lineItemCommand.getPickedQuantity(), lineItemCommand.getAcceptedQuantity(), lineItemCommand.getReturnReason(), lineItemCommand.getLineNotes()))
                .collect(Collectors.toList());
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
