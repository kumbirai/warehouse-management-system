package com.ccbsa.wms.stock.application.dto.query;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO: StockAvailabilityItemDTO
 * <p>
 * Represents a stock item in stock availability responses.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAvailabilityItemDTO {
    private String locationId;
    private Integer availableQuantity;
    private LocalDate expirationDate;
    private String stockItemId;
}
