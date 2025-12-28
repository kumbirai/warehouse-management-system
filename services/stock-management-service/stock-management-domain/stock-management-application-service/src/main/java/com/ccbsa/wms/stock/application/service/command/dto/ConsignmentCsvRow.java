package com.ccbsa.wms.stock.application.service.command.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

/**
 * CSV Row DTO: ConsignmentCsvRow
 * <p>
 * Represents a single row from a consignment CSV file.
 */
@Getter
@Builder
public final class ConsignmentCsvRow {
    private final long rowNumber;
    private final String consignmentReference;
    private final String productCode;
    private final int quantity;
    private final LocalDate expirationDate;
    private final LocalDateTime receivedDate;
    private final String warehouseId;

    public ConsignmentCsvRow(long rowNumber, String consignmentReference, String productCode, int quantity, LocalDate expirationDate, LocalDateTime receivedDate,
                             String warehouseId) {
        if (consignmentReference == null || consignmentReference.trim().isEmpty()) {
            throw new IllegalArgumentException("ConsignmentReference is required");
        }
        if (productCode == null || productCode.trim().isEmpty()) {
            throw new IllegalArgumentException("ProductCode is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (warehouseId == null || warehouseId.trim().isEmpty()) {
            throw new IllegalArgumentException("WarehouseId is required");
        }
        this.rowNumber = rowNumber;
        this.consignmentReference = consignmentReference;
        this.productCode = productCode;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
        this.receivedDate = receivedDate;
        this.warehouseId = warehouseId;
    }
}

