package com.ccbsa.wms.location.application.dto.query;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Result DTO: ListStockMovementsQueryResultDTO
 * <p>
 * Response DTO for listing stock movements.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Lists are immutable when returned from API.")
public class ListStockMovementsQueryResultDTO {
    private List<StockMovementQueryResultDTO> movements;
    private Long totalCount;
}

