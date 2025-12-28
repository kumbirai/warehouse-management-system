package com.ccbsa.wms.stock.application.dto.command;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result DTO: AdjustStockResultDTO
 * <p>
 * Response DTO for adjusting stock levels.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustStockResultDTO {
    private UUID adjustmentId;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private LocalDateTime adjustedAt;
}

