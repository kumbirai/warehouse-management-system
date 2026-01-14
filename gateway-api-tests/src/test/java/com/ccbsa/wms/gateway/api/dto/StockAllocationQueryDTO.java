package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDateTime;

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
public class StockAllocationQueryDTO {
    private String allocationId;
    private String productId;
    private String locationId;
    private String stockItemId;
    private Integer quantity;
    private String allocationType;
    private String referenceId;
    private String status;
    private LocalDateTime allocatedAt;
    private LocalDateTime releasedAt;
    private String allocatedBy;
    private String notes;
}

