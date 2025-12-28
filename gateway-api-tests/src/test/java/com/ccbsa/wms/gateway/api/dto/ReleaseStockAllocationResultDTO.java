package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseStockAllocationResultDTO {
    private UUID allocationId;
    private String status; // AllocationStatus enum value
    private LocalDateTime releasedAt;
}

