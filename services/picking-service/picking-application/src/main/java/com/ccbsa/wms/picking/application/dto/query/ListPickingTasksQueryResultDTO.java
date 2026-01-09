package com.ccbsa.wms.picking.application.dto.query;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListPickingTasksQueryResultDTO
 * <p>
 * DTO for picking tasks list query results.
 */
@Getter
@Builder
public class ListPickingTasksQueryResultDTO {
    private final List<PickingTaskQueryResultDTO> pickingTasks;
    private final int totalElements;
    private final int page;
    private final int size;
    private final int totalPages;

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
