package com.ccbsa.wms.stock.application.dto.query;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Result DTO: ListStockAdjustmentsQueryResultDTO
 * <p>
 * API response DTO for listing stock adjustments.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Lists are immutable when returned from API.")
public class ListStockAdjustmentsQueryResultDTO {
    private List<StockAdjustmentQueryDTO> adjustments;
    private int totalCount;
}

