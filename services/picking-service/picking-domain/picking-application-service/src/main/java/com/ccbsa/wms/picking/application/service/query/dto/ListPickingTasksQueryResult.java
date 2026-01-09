package com.ccbsa.wms.picking.application.service.query.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ccbsa.wms.picking.domain.core.entity.PickingTask;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListPickingTasksQueryResult
 * <p>
 * Result object for listing picking tasks query.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder, and getter returns defensive copy")
public final class ListPickingTasksQueryResult {
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
    private final List<PickingTask> pickingTasks;
    private final int totalElements;
    private final int page;
    private final int size;
    private final int totalPages;

    /**
     * Returns a defensive copy of the picking tasks list to prevent external modification.
     *
     * @return unmodifiable copy of the picking tasks list
     */
    public List<PickingTask> getPickingTasks() {
        if (pickingTasks == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(pickingTasks));
    }
}
