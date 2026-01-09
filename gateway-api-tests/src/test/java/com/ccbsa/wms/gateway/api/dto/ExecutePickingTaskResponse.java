package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePickingTaskResponse {
    private String taskId;
    private String status;
    private Integer pickedQuantity;
    private Boolean isPartialPicking;
}
