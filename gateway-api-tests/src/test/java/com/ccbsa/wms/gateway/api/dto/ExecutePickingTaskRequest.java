package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePickingTaskRequest {
    private Integer pickedQuantity;
    private Boolean isPartialPicking;
    private String partialReason;
}
