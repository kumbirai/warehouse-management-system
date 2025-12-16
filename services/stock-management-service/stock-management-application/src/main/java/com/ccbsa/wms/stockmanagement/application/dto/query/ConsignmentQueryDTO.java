package com.ccbsa.wms.stockmanagement.application.dto.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Query Result DTO: ConsignmentQueryDTO
 * <p>
 * API response DTO for consignment queries.
 */
public class ConsignmentQueryDTO {
    private String consignmentId;
    private String consignmentReference;
    private String warehouseId;
    private String status;
    private LocalDateTime receivedAt;
    private LocalDateTime confirmedAt;
    private String receivedBy;
    private List<ConsignmentLineItemDTO> lineItems;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    public ConsignmentQueryDTO() {
    }

    public String getConsignmentId() {
        return consignmentId;
    }

    public void setConsignmentId(String consignmentId) {
        this.consignmentId = consignmentId;
    }

    public String getConsignmentReference() {
        return consignmentReference;
    }

    public void setConsignmentReference(String consignmentReference) {
        this.consignmentReference = consignmentReference;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    public List<ConsignmentLineItemDTO> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<ConsignmentLineItemDTO> lineItems) {
        this.lineItems = lineItems;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    /**
     * Nested DTO for consignment line items.
     */
    public static class ConsignmentLineItemDTO {
        private String productCode;
        private int quantity;
        private LocalDate expirationDate;

        public ConsignmentLineItemDTO() {
        }

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public LocalDate getExpirationDate() {
            return expirationDate;
        }

        public void setExpirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
        }
    }
}

