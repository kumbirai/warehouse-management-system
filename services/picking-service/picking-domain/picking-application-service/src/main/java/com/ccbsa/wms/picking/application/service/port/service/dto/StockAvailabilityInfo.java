package com.ccbsa.wms.picking.application.service.port.service.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO: StockAvailabilityInfo
 * <p>
 * Represents stock availability information for picking location planning.
 */
@Getter
@Builder
public class StockAvailabilityInfo {
    private final String locationId;
    private final String productCode;
    private final int availableQuantity;
    private final LocalDate expirationDate;
    private final String stockItemId;
}
