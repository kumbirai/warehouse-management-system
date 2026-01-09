package com.ccbsa.wms.picking.application.service.command.dto;

import java.util.List;

import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: PlanPickingLocationsResult
 * <p>
 * Result object returned after planning picking locations.
 */
@Getter
@Builder
public final class PlanPickingLocationsResult {
    private final LoadId loadId;
    private final List<PickingTaskId> pickingTaskIds;
    private final int totalTasks;

    public PlanPickingLocationsResult(LoadId loadId, List<PickingTaskId> pickingTaskIds, int totalTasks) {
        if (loadId == null) {
            throw new IllegalArgumentException("LoadId is required");
        }
        this.loadId = loadId;
        this.pickingTaskIds = pickingTaskIds != null ? List.copyOf(pickingTaskIds) : List.of();
        this.totalTasks = totalTasks;
    }
}
