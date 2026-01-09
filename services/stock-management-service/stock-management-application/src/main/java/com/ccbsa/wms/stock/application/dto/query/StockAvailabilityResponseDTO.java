package com.ccbsa.wms.stock.application.dto.query;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO: StockAvailabilityResponseDTO
 * <p>
 * Response DTO for stock availability query for multiple products.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAvailabilityResponseDTO {
    private Map<String, List<StockAvailabilityItemDTO>> stockByProduct;
}
