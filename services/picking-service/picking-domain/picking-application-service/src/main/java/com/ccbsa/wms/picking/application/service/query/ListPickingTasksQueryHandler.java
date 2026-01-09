package com.ccbsa.wms.picking.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.picking.application.service.port.repository.PickingTaskRepository;
import com.ccbsa.wms.picking.application.service.query.dto.ListPickingTasksQuery;
import com.ccbsa.wms.picking.application.service.query.dto.ListPickingTasksQueryResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: ListPickingTasksQueryHandler
 * <p>
 * Handles query for listing picking tasks with filtering and pagination.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ListPickingTasksQueryHandler {
    private final PickingTaskRepository pickingTaskRepository;

    @Transactional(readOnly = true)
    public ListPickingTasksQueryResult handle(ListPickingTasksQuery query) {
        var pickingTasks = pickingTaskRepository.findAll(query.getStatus(), query.getPage(), query.getSize());

        long totalElements = pickingTaskRepository.countAll(query.getStatus());

        int totalPages = (int) Math.ceil((double) totalElements / query.getSize());

        // Create defensive copy of picking tasks list for builder
        return ListPickingTasksQueryResult.builder()
                .pickingTasks(new java.util.ArrayList<>(pickingTasks))
                .totalElements((int) totalElements)
                .page(query.getPage())
                .size(query.getSize())
                .totalPages(totalPages)
                .build();
    }
}
