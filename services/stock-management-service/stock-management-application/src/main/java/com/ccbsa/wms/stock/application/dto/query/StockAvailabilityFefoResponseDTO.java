package com.ccbsa.wms.stock.application.dto.query;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO: StockAvailabilityFefoResponseDTO
 * <p>
 * Response DTO for FEFO stock availability query.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAvailabilityFefoResponseDTO {
    private String productCode;
    private List<StockAvailabilityItemDTO> stockItems;
}
