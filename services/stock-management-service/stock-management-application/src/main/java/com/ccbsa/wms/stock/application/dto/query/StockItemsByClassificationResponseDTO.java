package com.ccbsa.wms.stock.application.dto.query;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO: StockItemsByClassificationResponseDTO
 * <p>
 * Wrapper DTO for stock items by classification query response.
 * Matches frontend expectation: { stockItems: StockItem[] }
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Lists are immutable when returned from API.")
public class StockItemsByClassificationResponseDTO {
    private List<StockItemQueryDTO> stockItems;
}
