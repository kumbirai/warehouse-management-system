package com.ccbsa.wms.stock.application.service.command.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CSV Row DTO: ConsignmentCsvRow
 * <p>
 * Represents a single row from a consignment CSV file.
 */
public final class ConsignmentCsvRow {
    private final long rowNumber;
    private final String consignmentReference;
    private final String productCode;
    private final int quantity;
    private final LocalDate expirationDate;
    private final LocalDateTime receivedDate;
    private final String warehouseId;

    private ConsignmentCsvRow(Builder builder) {
        this.rowNumber = builder.rowNumber;
        this.consignmentReference = builder.consignmentReference;
        this.productCode = builder.productCode;
        this.quantity = builder.quantity;
        this.expirationDate = builder.expirationDate;
        this.receivedDate = builder.receivedDate;
        this.warehouseId = builder.warehouseId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getRowNumber() {
        return rowNumber;
    }

    public String getConsignmentReference() {
        return consignmentReference;
    }

    public String getProductCode() {
        return productCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public LocalDateTime getReceivedDate() {
        return receivedDate;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public static class Builder {
        private long rowNumber;
        private String consignmentReference;
        private String productCode;
        private int quantity;
        private LocalDate expirationDate;
        private LocalDateTime receivedDate;
        private String warehouseId;

        public Builder rowNumber(long rowNumber) {
            this.rowNumber = rowNumber;
            return this;
        }

        public Builder consignmentReference(String consignmentReference) {
            this.consignmentReference = consignmentReference;
            return this;
        }

        public Builder productCode(String productCode) {
            this.productCode = productCode;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder expirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public Builder receivedDate(LocalDateTime receivedDate) {
            this.receivedDate = receivedDate;
            return this;
        }

        public Builder warehouseId(String warehouseId) {
            this.warehouseId = warehouseId;
            return this;
        }

        public ConsignmentCsvRow build() {
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
            return new ConsignmentCsvRow(this);
        }
    }
}

