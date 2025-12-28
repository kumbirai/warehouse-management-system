package com.ccbsa.wms.stock.application.dto.command;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.common.domain.valueobject.AllocationType;
import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result DTO: AllocateStockResultDTO
 * <p>
 * Response DTO for allocating stock.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocateStockResultDTO {
    private UUID allocationId;
    private UUID productId;
    private UUID locationId;
    private UUID stockItemId;
    private Integer quantity;
    private AllocationType allocationType;
    private String referenceId;
    private AllocationStatus status;
    private LocalDateTime allocatedAt;
}

