package com.ccbsa.wms.picking.application.service.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingTaskCommand;
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingTaskResult;
import com.ccbsa.wms.picking.application.service.port.messaging.PickingEventPublisher;
import com.ccbsa.wms.picking.application.service.port.repository.LoadRepository;
import com.ccbsa.wms.picking.application.service.port.repository.PickingListRepository;
import com.ccbsa.wms.picking.application.service.port.repository.PickingTaskRepository;
import com.ccbsa.wms.picking.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.picking.application.service.port.service.dto.ProductInfo;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import com.ccbsa.wms.picking.domain.core.entity.Order;
import com.ccbsa.wms.picking.domain.core.entity.OrderLineItem;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;
import com.ccbsa.wms.picking.domain.core.entity.PickingTask;
import com.ccbsa.wms.picking.domain.core.event.PickingTaskCreatedEvent;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.LocationId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderLineItemId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderStatus;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskStatus;
import com.ccbsa.wms.picking.domain.core.valueobject.ProductCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: CreatePickingTaskCommandHandler
 * <p>
 * Handles picking task creation.
 * <p>
 * Responsibilities:
 * - Validate products against Product Service
 * - Create Load and Order if needed
 * - Create PickingTask entities
 * - Publish domain events
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CreatePickingTaskCommandHandler {
    private final PickingTaskRepository pickingTaskRepository;
    private final LoadRepository loadRepository;
    private final PickingListRepository pickingListRepository;
    private final ProductServicePort productServicePort;
    private final PickingEventPublisher eventPublisher;

    @Transactional
    public CreatePickingTaskResult handle(CreatePickingTaskCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Get product codes from product IDs
        List<ProductCodeInfo> productCodeInfos = new ArrayList<>();
        for (CreatePickingTaskCommand.PickingItemCommand item : command.getItems()) {
            ProductInfo productInfo = productServicePort.getProductById(item.getProductId(), command.getTenantId().getValue())
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Product not found: %s", item.getProductId())));
            productCodeInfos.add(new ProductCodeInfo(productInfo.getProductCode()));
        }

        // 3. Create or find Load and Order together
        // Note: Order must be added to Load before saving due to NOT NULL constraint on orders.load_id
        LoadId loadId;
        OrderId orderId;
        LoadOrderResult loadOrderResult = findOrCreateLoadAndOrder(command);
        loadId = loadOrderResult.loadId();
        orderId = loadOrderResult.orderId();

        // 5. Create picking tasks
        List<PickingTask> pickingTasks = new ArrayList<>();
        List<DomainEvent<?>> allEvents = new ArrayList<>();
        int sequence = 0;

        for (int i = 0; i < command.getItems().size(); i++) {
            CreatePickingTaskCommand.PickingItemCommand item = command.getItems().get(i);
            ProductCodeInfo productCodeInfo = productCodeInfos.get(i);

            PickingTask task = PickingTask.builder().id(PickingTaskId.generate()).loadId(loadId).orderId(orderId).productCode(ProductCode.of(productCodeInfo.productCode))
                    .locationId(LocationId.of(UUID.fromString(item.getLocationId()))).quantity(Quantity.of(item.getQuantity())).status(PickingTaskStatus.PENDING)
                    .sequence(sequence++).build();

            pickingTasks.add(task);

            // Create domain event
            PickingTaskCreatedEvent event =
                    new PickingTaskCreatedEvent(task.getId().getValueAsString(), command.getTenantId(), loadId.getValueAsString(), orderId.getValueAsString(),
                            productCodeInfo.productCode, item.getLocationId(), item.getQuantity(), task.getSequence());
            allEvents.add(event);
        }

        // 6. Save picking tasks
        pickingTaskRepository.saveAll(pickingTasks);

        // 7. Publish events after transaction commit
        if (!allEvents.isEmpty()) {
            publishEventsAfterCommit(allEvents);
        }

        // 8. Return result (use first task as representative)
        PickingTask firstTask = pickingTasks.get(0);
        return CreatePickingTaskResult.builder().taskId(firstTask.getId()).status(firstTask.getStatus().name()).orderId(orderId.getValueAsString()).build();
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "Defensive validation is intentional - validates "
            + "command integrity even if constructor validation exists")
    private void validateCommand(CreatePickingTaskCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getOrderId() == null || command.getOrderId().isBlank()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }
    }

    /**
     * Finds or creates Load and Order together.
     * <p>
     * This method ensures that Order is always associated with a Load before saving,
     * as the database requires orders.load_id to be NOT NULL.
     *
     * @param command Command containing order and load information
     * @return LoadOrderResult containing both LoadId and OrderId
     */
    private LoadOrderResult findOrCreateLoadAndOrder(CreatePickingTaskCommand command) {
        // Generate load number from order ID
        String loadNumberStr = "LOAD-" + command.getOrderId();
        LoadNumber loadNumber = LoadNumber.of(loadNumberStr);
        OrderNumber orderNumber = OrderNumber.of(command.getOrderId());

        // Try to find existing load by load number
        var existingLoad = loadRepository.findByLoadNumberAndTenantId(loadNumber, command.getTenantId());

        if (existingLoad.isPresent()) {
            Load load = existingLoad.get();
            // Check if order already exists in this load
            var existingOrder = load.getOrders().stream().filter(o -> o.getOrderNumber().equals(orderNumber)).findFirst();

            if (existingOrder.isPresent()) {
                // Order already exists in load
                return new LoadOrderResult(load.getId(), existingOrder.get().getId());
            }

            // Order doesn't exist - need to create it and add to existing load
            Order newOrder = createOrder(command, orderNumber);
            load.addOrder(newOrder);
            loadRepository.save(load); // Cascade will save the order with load_id
            return new LoadOrderResult(load.getId(), newOrder.getId());
        }

        // Load doesn't exist - create PickingList, Load, and Order
        // Note: Load requires a PickingList due to NOT NULL constraint on loads.picking_list_id
        Order newOrder = createOrder(command, orderNumber);
        Load load = Load.builder().id(LoadId.generate()).tenantId(command.getTenantId()).loadNumber(loadNumber).build();

        // Add order to load before saving
        load.addOrder(newOrder);

        // Create minimal picking list for standalone picking tasks
        PickingList pickingList = PickingList.builder().id(PickingListId.generate()).tenantId(command.getTenantId()).load(load).notes("Standalone picking task").build();

        // Save picking list (cascade will save load and order)
        pickingListRepository.save(pickingList);

        return new LoadOrderResult(load.getId(), newOrder.getId());
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

    /**
     * Creates a new Order entity (but does not save it).
     * <p>
     * The Order must be added to a Load before saving due to NOT NULL constraint on orders.load_id.
     *
     * @param command     Command containing order information
     * @param orderNumber Order number
     * @return Created Order entity (not yet persisted)
     */
    private Order createOrder(CreatePickingTaskCommand command, OrderNumber orderNumber) {
        Priority priority = command.getPriority() != null ? Priority.fromString(command.getPriority()) : Priority.NORMAL;

        CustomerInfo customerInfo = CustomerInfo.of("STANDALONE", "Standalone Order");

        // Create order line items from picking items
        List<OrderLineItem> lineItems = new ArrayList<>();
        for (int i = 0; i < command.getItems().size(); i++) {
            CreatePickingTaskCommand.PickingItemCommand item = command.getItems().get(i);
            // Get product code from product service
            ProductInfo productInfo = productServicePort.getProductById(item.getProductId(), command.getTenantId().getValue())
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Product not found: %s", item.getProductId())));

            OrderLineItem lineItem =
                    OrderLineItem.builder().id(OrderLineItemId.generate()).productCode(ProductCode.of(productInfo.getProductCode())).quantity(Quantity.of(item.getQuantity()))
                            .build();
            lineItems.add(lineItem);
        }

        return Order.builder().id(OrderId.generate()).orderNumber(orderNumber).customerInfo(customerInfo).priority(priority).status(OrderStatus.PENDING).lineItems(lineItems)
                .build();
    }

    /**
     * Result record for Load and Order creation.
     */
    private record LoadOrderResult(LoadId loadId, OrderId orderId) {
    }

    /**
     * Helper class to hold product code mapping
     */
    private static class ProductCodeInfo {
        final String productCode;

        ProductCodeInfo(String productCode) {
            this.productCode = productCode;
        }
    }
}
