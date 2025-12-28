package com.ccbsa.wms.stock.application.dto.command;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result DTO: ReleaseStockAllocationResultDTO
 * <p>
 * Response DTO for releasing a stock allocation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseStockAllocationResultDTO {
    private UUID allocationId;
    private AllocationStatus status;
    private LocalDateTime releasedAt;
}

