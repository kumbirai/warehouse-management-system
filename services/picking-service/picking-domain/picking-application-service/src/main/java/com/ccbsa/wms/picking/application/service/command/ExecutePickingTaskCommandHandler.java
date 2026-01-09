package com.ccbsa.wms.picking.application.service.command;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.picking.application.service.command.dto.ExecutePickingTaskCommand;
import com.ccbsa.wms.picking.application.service.command.dto.ExecutePickingTaskResult;
import com.ccbsa.wms.picking.application.service.port.messaging.PickingEventPublisher;
import com.ccbsa.wms.picking.application.service.port.repository.LoadRepository;
import com.ccbsa.wms.picking.application.service.port.repository.PickingTaskRepository;
import com.ccbsa.wms.picking.application.service.port.service.StockManagementServicePort;
import com.ccbsa.wms.picking.domain.core.entity.PickingTask;
import com.ccbsa.wms.picking.domain.core.exception.ExpiredStockException;
import com.ccbsa.wms.picking.domain.core.exception.PickingTaskNotFoundException;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: ExecutePickingTaskCommandHandler
 * <p>
 * Handles execution of picking tasks.
 * <p>
 * Responsibilities:
 * - Load picking task from repository
 * - Validate stock availability and expiration
 * - Execute picking task (full or partial)
 * - Save picking task
 * - Publish domain events
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExecutePickingTaskCommandHandler {

    private final PickingTaskRepository pickingTaskRepository;
    private final LoadRepository loadRepository;
    private final StockManagementServicePort stockManagementService;
    private final PickingEventPublisher eventPublisher;

    @Transactional
    public ExecutePickingTaskResult handle(ExecutePickingTaskCommand command) {
        log.info("Executing picking task: {} by user: {}", command.getPickingTaskId().getValueAsString(), command.getPickedByUserId().getValue());

        // 1. Retrieve picking task
        PickingTask pickingTask = pickingTaskRepository.findById(command.getPickingTaskId())
                .orElseThrow(() -> new PickingTaskNotFoundException("Picking task not found: " + command.getPickingTaskId().getValueAsString()));

        // 2. Get picking list ID from load
        PickingListId pickingListId = loadRepository.findPickingListIdByLoadId(pickingTask.getLoadId(), command.getTenantId())
                .orElseThrow(() -> new IllegalStateException("Picking list not found for load: " + pickingTask.getLoadId().getValueAsString()));

        // 3. Validate stock availability and expiration
        validateStockAvailability(pickingTask, command.getPickedQuantity());

        // 4. Execute picking task
        Quantity pickedQuantity = command.getPickedQuantity();

        if (command.isPartialPicking()) {
            pickingTask.executePartial(pickedQuantity, command.getPartialReason(), command.getPickedByUserId(), pickingListId, command.getTenantId());
            log.info("Partial picking executed for task: {} with quantity: {}", command.getPickingTaskId().getValueAsString(), pickedQuantity.getValue());
        } else {
            pickingTask.execute(pickedQuantity, command.getPickedByUserId(), pickingListId, command.getTenantId());
            log.info("Full picking executed for task: {} with quantity: {}", command.getPickingTaskId().getValueAsString(), pickedQuantity.getValue());
        }

        // 5. Save picking task
        pickingTaskRepository.save(pickingTask);

        // 6. Publish domain events
        List<DomainEvent<?>> events = pickingTask.getDomainEvents();
        if (!events.isEmpty()) {
            eventPublisher.publish(events);
            pickingTask.clearDomainEvents();
        }

        log.info("Picking task executed successfully: {}", command.getPickingTaskId().getValueAsString());

        // 7. Return result
        return ExecutePickingTaskResult.builder().pickingTaskId(command.getPickingTaskId()).status(pickingTask.getStatus().name())
                .pickedQuantity(command.getPickedQuantity().getValue()).isPartialPicking(command.isPartialPicking()).build();
    }

    private void validateStockAvailability(PickingTask pickingTask, Quantity pickedQuantity) {
        // Check stock availability
        boolean stockAvailable =
                stockManagementService.checkStockAvailability(pickingTask.getProductCode().getValue(), pickingTask.getLocationId().getValueAsString(), pickedQuantity.getValue());

        if (!stockAvailable) {
            throw new IllegalStateException(
                    String.format("Insufficient stock for product %s at location %s. Required: %d, Attempting to pick: %d", pickingTask.getProductCode().getValue(),
                            pickingTask.getLocationId().getValueAsString(), pickingTask.getQuantity().getValue(), pickedQuantity.getValue()));
        }

        // Check if stock is expired
        boolean stockExpired = stockManagementService.isStockExpired(pickingTask.getProductCode().getValue(), pickingTask.getLocationId().getValueAsString());

        if (stockExpired) {
            throw new ExpiredStockException(String.format("Cannot pick expired stock for product %s at location %s", pickingTask.getProductCode().getValue(),
                    pickingTask.getLocationId().getValueAsString()));
        }
    }
}
