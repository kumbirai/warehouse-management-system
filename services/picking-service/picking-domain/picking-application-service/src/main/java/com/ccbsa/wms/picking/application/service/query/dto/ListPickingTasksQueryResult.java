package com.ccbsa.wms.picking.application.service.query.dto;

import java.util.List;

import com.ccbsa.wms.picking.domain.core.entity.PickingTask;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListPickingTasksQueryResult
 * <p>
 * Result object for listing picking tasks query.
 */
@Getter
@Builder
public final class ListPickingTasksQueryResult {
    private final List<PickingTask> pickingTasks;
    private final int totalElements;
    private final int page;
    private final int size;
    private final int totalPages;
}
