package com.ccbsa.wms.picking.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command Result DTO: ExecutePickingTaskResultDTO
 * <p>
 * API response DTO for picking task execution result.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePickingTaskResultDTO {
    private String pickingTaskId;
    private String status;
    private Integer pickedQuantity;
    private Boolean isPartialPicking;
}
