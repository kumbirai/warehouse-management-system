package com.ccbsa.wms.picking.application.service.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.CustomerInfo;
import com.ccbsa.common.domain.valueobject.LoadNumber;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.Priority;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingListCommand;
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingListResult;
import com.ccbsa.wms.picking.application.service.port.messaging.PickingEventPublisher;
import com.ccbsa.wms.picking.application.service.port.repository.PickingListReferenceGenerator;
import com.ccbsa.wms.picking.application.service.port.repository.PickingListRepository;
import com.ccbsa.wms.picking.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import com.ccbsa.wms.picking.domain.core.entity.Order;
import com.ccbsa.wms.picking.domain.core.entity.OrderLineItem;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderLineItemId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderStatus;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.ProductCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: CreatePickingListCommandHandler
 * <p>
 * Handles manual picking list creation.
 * <p>
 * Responsibilities:
 * - Validate products against Product Service
 * - Create PickingList aggregates with Loads and Orders
 * - Publish domain events
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CreatePickingListCommandHandler {
    private final PickingListRepository pickingListRepository;
    private final ProductServicePort productServicePort;
    private final PickingEventPublisher eventPublisher;
    private final PickingListReferenceGenerator referenceGenerator;

    @Transactional
    public CreatePickingListResult handle(CreatePickingListCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Create loads and orders
        List<Load> loads = new ArrayList<>();
        List<DomainEvent<?>> allEvents = new ArrayList<>();

        for (CreatePickingListCommand.LoadCommand loadCommand : command.getLoads()) {
            // Create Load
            LoadNumber loadNumber = LoadNumber.of(loadCommand.getLoadNumber());
            Load load = Load.builder().id(LoadId.generate()).tenantId(command.getTenantId()).loadNumber(loadNumber).build();

            // Create Orders for this load
            for (CreatePickingListCommand.OrderCommand orderCommand : loadCommand.getOrders()) {
                // Validate products
                for (CreatePickingListCommand.OrderLineItemCommand lineItemCommand : orderCommand.getLineItems()) {
                    if (!productServicePort.productExists(lineItemCommand.getProductCode())) {
                        throw new IllegalArgumentException(String.format("Product does not exist: %s", lineItemCommand.getProductCode()));
                    }
                }

                // Create Order
                OrderNumber orderNumber = OrderNumber.of(orderCommand.getOrderNumber());
                CustomerInfo customerInfo = CustomerInfo.of(orderCommand.getCustomerCode(), orderCommand.getCustomerName());
                Priority priority = orderCommand.getPriority() != null ? Priority.fromString(orderCommand.getPriority()) : Priority.NORMAL;

                // Build line items
                List<OrderLineItem> lineItems = new ArrayList<>();
                for (CreatePickingListCommand.OrderLineItemCommand lineItemCommand : orderCommand.getLineItems()) {
                    OrderLineItem lineItem = OrderLineItem.builder().id(OrderLineItemId.generate()).productCode(ProductCode.of(lineItemCommand.getProductCode()))
                            .quantity(Quantity.of(lineItemCommand.getQuantity())).notes(lineItemCommand.getNotes()).build();
                    lineItems.add(lineItem);
                }

                // Build order
                Order order = Order.builder().id(OrderId.generate()).orderNumber(orderNumber).customerInfo(customerInfo).priority(priority).status(OrderStatus.PENDING)
                        .lineItems(lineItems).build();

                // Add order to load (will be saved via cascade when picking list is saved)
                load.addOrder(order);
            }

            // Add load to list (will be saved via cascade when picking list is saved)
            loads.add(load);

            // Collect events (but don't clear yet - will be cleared after picking list save)
            allEvents.addAll(load.getDomainEvents());
        }

        // 3. Generate picking list reference
        var pickingListReference = referenceGenerator.generate(command.getTenantId());

        // 4. Create PickingList
        PickingList pickingList =
                PickingList.builder().id(PickingListId.generate()).tenantId(command.getTenantId()).pickingListReference(pickingListReference).loads(loads).notes(command.getNotes())
                        .build();

        // Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = List.copyOf(pickingList.getDomainEvents());

        // Save picking list (this will cascade save all loads and orders)
        pickingListRepository.save(pickingList);

        // Collect all events and clear domain events
        allEvents.addAll(domainEvents);
        pickingList.clearDomainEvents();
        // Clear events from all loads now that they're saved
        loads.forEach(Load::clearDomainEvents);

        // 5. Publish events after transaction commit
        if (!allEvents.isEmpty()) {
            publishEventsAfterCommit(allEvents);
        }

        // 6. Return result
        return CreatePickingListResult.builder().pickingListId(pickingList.getId()).status(pickingList.getStatus()).receivedAt(pickingList.getReceivedAt())
                .loadCount(pickingList.getLoadCount()).build();
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "Defensive validation is intentional - validates "
            + "command integrity even if constructor validation exists")
    private void validateCommand(CreatePickingListCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getLoads() == null || command.getLoads().isEmpty()) {
            throw new IllegalArgumentException("At least one load is required");
        }
    }

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
