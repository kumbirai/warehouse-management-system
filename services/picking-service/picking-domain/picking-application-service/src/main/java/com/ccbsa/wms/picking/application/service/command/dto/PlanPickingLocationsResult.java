package com.ccbsa.wms.picking.application.service.command.dto;

import java.util.Collections;
import java.util.List;

import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: PlanPickingLocationsResult
 * <p>
 * Result object returned after planning picking locations.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor and getter returns defensive copy")
public final class PlanPickingLocationsResult {
    private final LoadId loadId;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor")
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

    /**
     * Returns a defensive copy of the picking task IDs list to prevent external modification.
     *
     * @return unmodifiable copy of the picking task IDs list
     */
    public List<PickingTaskId> getPickingTaskIds() {
        return Collections.unmodifiableList(pickingTaskIds);
    }
}
