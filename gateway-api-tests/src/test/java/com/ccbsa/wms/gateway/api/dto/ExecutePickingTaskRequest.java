package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePickingTaskRequest {
    private Integer pickedQuantity;
    private Boolean isPartialPicking;
    private String partialReason;
}
