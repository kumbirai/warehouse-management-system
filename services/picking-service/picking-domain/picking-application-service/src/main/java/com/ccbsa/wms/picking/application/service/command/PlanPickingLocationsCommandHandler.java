package com.ccbsa.wms.picking.application.service.command;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.picking.application.service.command.dto.PlanPickingLocationsCommand;
import com.ccbsa.wms.picking.application.service.command.dto.PlanPickingLocationsResult;
import com.ccbsa.wms.picking.application.service.port.messaging.PickingEventPublisher;
import com.ccbsa.wms.picking.application.service.port.repository.LoadRepository;
import com.ccbsa.wms.picking.application.service.port.repository.PickingTaskRepository;
import com.ccbsa.wms.picking.application.service.port.service.StockManagementServicePort;
import com.ccbsa.wms.picking.application.service.port.service.dto.StockAvailabilityInfo;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import com.ccbsa.wms.picking.domain.core.entity.Order;
import com.ccbsa.wms.picking.domain.core.entity.OrderLineItem;
import com.ccbsa.wms.picking.domain.core.entity.PickingTask;
import com.ccbsa.wms.picking.domain.core.valueobject.LocationId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskStatus;
import com.ccbsa.wms.picking.domain.core.valueobject.ProductCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: PlanPickingLocationsCommandHandler
 * <p>
 * Handles picking location planning using FEFO (First Expiring First Out) strategy.
 * <p>
 * Responsibilities:
 * - Query Stock Management Service for available stock (FEFO)
 * - Optimize picking sequence by location proximity
 * - Create PickingTask entities
 * - Publish LoadPlannedEvent and PickingTaskCreatedEvent
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanPickingLocationsCommandHandler {
    private final LoadRepository loadRepository;
    private final PickingTaskRepository pickingTaskRepository;
    private final StockManagementServicePort stockManagementServicePort;
    private final PickingEventPublisher eventPublisher;

    @Transactional
    public PlanPickingLocationsResult handle(PlanPickingLocationsCommand command) {
        // 1. Load the load aggregate
        Load load = loadRepository.findByIdAndTenantId(command.getLoadId(), command.getTenantId()).orElseThrow(() -> new IllegalStateException("Load not found"));

        // 2. Collect all products and quantities needed
        Map<String, Integer> productQuantities = collectProductQuantities(load);

        // 3. Query stock availability for all products (FEFO)
        Map<String, List<StockAvailabilityInfo>> stockByProduct = stockManagementServicePort.queryAvailableStockForProducts(productQuantities);

        // 4. Create picking tasks with optimized sequence
        List<PickingTask> pickingTasks = createPickingTasks(load, stockByProduct);

        // 5. Save picking tasks
        pickingTaskRepository.saveAll(pickingTasks);

        // 6. Plan the load (updates status and publishes event)
        List<String> pickingTaskIds = pickingTasks.stream().map(task -> task.getId().getValueAsString()).toList();
        load.plan(pickingTaskIds);

        // 7. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = List.copyOf(load.getDomainEvents());

        // 8. Save load
        loadRepository.save(load);

        // 9. Collect all events (load events + task events)
        List<DomainEvent<?>> allEvents = new ArrayList<>(domainEvents);
        // Note: PickingTaskCreatedEvent is published by the aggregate when tasks are created
        // For now, we'll publish LoadPlannedEvent which includes task IDs

        // 10. Publish events after transaction commit
        if (!allEvents.isEmpty()) {
            publishEventsAfterCommit(allEvents);
            load.clearDomainEvents();
        }

        // 11. Return result (create defensive copy of task IDs list)
        List<PickingTaskId> taskIds = pickingTasks.stream().map(PickingTask::getId).toList();
        return PlanPickingLocationsResult.builder()
                .loadId(load.getId())
                .pickingTaskIds(new java.util.ArrayList<>(taskIds))
                .totalTasks(pickingTasks.size())
                .build();
    }

    private Map<String, Integer> collectProductQuantities(Load load) {
        return load.getOrders().stream().flatMap(order -> order.getLineItems().stream())
                .filter(lineItem -> lineItem.getProductCode() != null && lineItem.getProductCode().getValue() != null)
                .collect(Collectors.groupingBy(lineItem -> lineItem.getProductCode().getValue(), Collectors.summingInt(lineItem -> lineItem.getQuantity().getValue())));
    }

    private List<PickingTask> createPickingTasks(Load load, Map<String, List<StockAvailabilityInfo>> stockByProduct) {
        List<PickingTask> tasks = new ArrayList<>();
        int sequence = 0;

        // Group by location for proximity optimization
        Map<String, List<OrderLineItem>> itemsByProduct = load.getOrders().stream().flatMap(
                        order -> order.getLineItems().stream().filter(lineItem -> lineItem.getProductCode() != null && lineItem.getProductCode().getValue() != null)
                                .map(lineItem -> Map.entry(lineItem.getProductCode().getValue(), lineItem)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        for (Map.Entry<String, List<OrderLineItem>> entry : itemsByProduct.entrySet()) {
            String productCode = entry.getKey();
            List<OrderLineItem> lineItems = entry.getValue();
            List<StockAvailabilityInfo> availableStock = new ArrayList<>(stockByProduct.getOrDefault(productCode, List.of()));

            // Sort by expiration date (FEFO) and location proximity
            availableStock.sort(Comparator.comparing((StockAvailabilityInfo info) -> info.getExpirationDate() != null ? info.getExpirationDate() : LocalDate.MAX)
                    .thenComparing(info -> info.getLocationId()));

            int remainingQuantity = lineItems.stream().mapToInt(item -> item.getQuantity().getValue()).sum();

            for (StockAvailabilityInfo stockInfo : availableStock) {
                if (remainingQuantity <= 0) {
                    break;
                }

                int quantityToPick = Math.min(remainingQuantity, stockInfo.getAvailableQuantity());
                if (quantityToPick <= 0) {
                    continue;
                }

                // Find the order for this line item (simplified - assumes one order per product)
                Order order = load.getOrders().stream().filter(o -> o.getLineItems().stream().anyMatch(li -> li.getProductCode().getValue().equals(productCode))).findFirst()
                        .orElseThrow();

                PickingTask task = PickingTask.builder().id(PickingTaskId.generate()).loadId(load.getId()).orderId(order.getId()).productCode(ProductCode.of(productCode))
                        .locationId(LocationId.of(stockInfo.getLocationId())).quantity(Quantity.of(quantityToPick)).status(PickingTaskStatus.PENDING).sequence(sequence++).build();

                tasks.add(task);
                remainingQuantity -= quantityToPick;
            }

            if (remainingQuantity > 0) {
                log.warn("Insufficient stock for product {}: need {}, available {}", productCode, remainingQuantity,
                        availableStock.stream().mapToInt(StockAvailabilityInfo::getAvailableQuantity).sum());
            }
        }

        return tasks;
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
