package com.ccbsa.wms.stock.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO: StockAvailabilityFefoRequestDTO
 * <p>
 * Request DTO for FEFO stock availability query.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAvailabilityFefoRequestDTO {
    private String productCode;
    private Integer quantity;
}
