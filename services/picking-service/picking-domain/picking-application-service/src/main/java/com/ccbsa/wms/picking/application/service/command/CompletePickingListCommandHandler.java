package com.ccbsa.wms.picking.application.service.command;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.service.command.dto.CompletePickingListCommand;
import com.ccbsa.wms.picking.application.service.command.dto.CompletePickingListResult;
import com.ccbsa.wms.picking.application.service.port.messaging.PickingEventPublisher;
import com.ccbsa.wms.picking.application.service.port.repository.LoadRepository;
import com.ccbsa.wms.picking.application.service.port.repository.PickingListRepository;
import com.ccbsa.wms.picking.application.service.port.repository.PickingTaskRepository;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;
import com.ccbsa.wms.picking.domain.core.entity.PickingTask;
import com.ccbsa.wms.picking.domain.core.exception.PickingListNotFoundException;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: CompletePickingListCommandHandler
 * <p>
 * Handles completion of picking lists.
 * <p>
 * Responsibilities:
 * - Load picking list from repository
 * - Validate all picking tasks are completed or partially completed
 * - Complete picking list
 * - Save picking list
 * - Publish domain events
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CompletePickingListCommandHandler {

    private final PickingListRepository pickingListRepository;
    private final LoadRepository loadRepository;
    private final PickingTaskRepository pickingTaskRepository;
    private final PickingEventPublisher eventPublisher;

    @Transactional
    public CompletePickingListResult handle(CompletePickingListCommand command) {
        log.info("Completing picking list: {} by user: {}", command.getPickingListId().getValueAsString(), command.getCompletedByUserId().getValue());

        // 1. Retrieve picking list
        PickingList pickingList = pickingListRepository.findByIdAndTenantId(command.getPickingListId(), command.getTenantId())
                .orElseThrow(() -> new PickingListNotFoundException("Picking list not found: " + command.getPickingListId().getValueAsString()));

        // 2. Validate all picking tasks are completed or partially completed
        validateAllTasksCompleted(pickingList, command.getTenantId());

        // 3. Complete picking list
        pickingList.complete(command.getCompletedByUserId());

        // 4. Save picking list
        pickingListRepository.save(pickingList);

        // 5. Publish domain events
        List<DomainEvent<?>> events = pickingList.getDomainEvents();
        if (!events.isEmpty()) {
            eventPublisher.publish(events);
            pickingList.clearDomainEvents();
        }

        log.info("Picking list completed successfully: {}", command.getPickingListId().getValueAsString());

        // 6. Return result
        return CompletePickingListResult.builder().pickingListId(command.getPickingListId()).status(pickingList.getStatus().name()).build();
    }

    private void validateAllTasksCompleted(PickingList pickingList, TenantId tenantId) {
        List<Load> loads = pickingList.getLoads();

        for (Load load : loads) {
            List<PickingTask> tasks = pickingTaskRepository.findByLoadId(load.getId());

            for (PickingTask task : tasks) {
                PickingTaskStatus status = task.getStatus();
                if (status != PickingTaskStatus.COMPLETED && status != PickingTaskStatus.PARTIALLY_COMPLETED) {
                    throw new IllegalStateException(String.format("Cannot complete picking list. Task %s is in status %s. All tasks must be completed or partially completed.",
                            task.getId().getValueAsString(), status));
                }
            }
        }
    }
}
