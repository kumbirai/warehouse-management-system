package com.ccbsa.wms.picking.application.dto.command;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: CreatePickingTaskResultDTO
 * <p>
 * DTO for picking task creation result.
 */
@Getter
@Builder
public class CreatePickingTaskResultDTO {
    private String taskId;
    private String status;
    private String orderId;
}
