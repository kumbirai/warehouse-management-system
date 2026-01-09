package com.ccbsa.wms.picking.application.service.command.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO: PickingListCsvRow
 * <p>
 * Represents a single row from the picking list CSV file.
 */
@Getter
@Builder
public final class PickingListCsvRow {
    private final long rowNumber;
    private final String loadNumber;
    private final String orderNumber;
    private final int orderLineNumber;
    private final String productCode;
    private final int quantity;
    private final String customerCode;
    private final String customerName;
    private final String priority;
    private final String requestedDeliveryDate;
    private final String warehouseId;
}
