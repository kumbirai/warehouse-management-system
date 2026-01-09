package com.ccbsa.wms.stock.application.dto.query;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO: StockAvailabilityRequestDTO
 * <p>
 * Request DTO for stock availability query for multiple products.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAvailabilityRequestDTO {
    private Map<String, Integer> productQuantities;
}
