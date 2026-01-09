package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseStockAllocationResultDTO {
    private UUID allocationId;
    private String status; // AllocationStatus enum value
    private LocalDateTime releasedAt;
}

