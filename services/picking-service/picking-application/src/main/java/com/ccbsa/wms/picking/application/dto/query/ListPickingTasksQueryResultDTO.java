package com.ccbsa.wms.picking.application.dto.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListPickingTasksQueryResultDTO
 * <p>
 * DTO for picking tasks list query results.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder, and getter returns defensive copy")
public class ListPickingTasksQueryResultDTO {
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
    private final List<PickingTaskQueryResultDTO> pickingTasks;
    private final int totalElements;
    private final int page;
    private final int size;
    private final int totalPages;

    /**
     * Returns a defensive copy of the picking tasks list to prevent external modification.
     *
     * @return unmodifiable copy of the picking tasks list
     */
    public List<PickingTaskQueryResultDTO> getPickingTasks() {
        if (pickingTasks == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(pickingTasks));
    }

    @Getter
    @Builder
    public static class PickingTaskQueryResultDTO {
        private final String taskId;
        private final String loadId;
        private final String orderId;
        private final String productCode;
        private final String locationId;
        private final int quantity;
        private final String status;
        private final int sequence;
    }
}
