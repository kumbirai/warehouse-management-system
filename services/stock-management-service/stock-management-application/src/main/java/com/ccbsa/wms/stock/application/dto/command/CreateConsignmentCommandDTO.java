package com.ccbsa.wms.stock.application.dto.command;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Command DTO: CreateConsignmentCommandDTO
 * <p>
 * API request DTO for creating a new stock consignment.
 */
public class CreateConsignmentCommandDTO {
    @NotBlank(message = "Consignment reference is required")
    private String consignmentReference;

    @NotBlank(message = "Warehouse ID is required")
    private String warehouseId;

    @NotNull(message = "Received date is required")
    private LocalDateTime receivedAt;

    private String receivedBy;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<ConsignmentLineItemDTO> lineItems;

    public CreateConsignmentCommandDTO() {
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

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
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

    /**
     * Nested DTO for consignment line items.
     */
    public static class ConsignmentLineItemDTO {
        @NotBlank(message = "Product code is required")
        private String productCode;

        @NotNull(message = "Quantity is required")
        private Integer quantity;

        private LocalDate expirationDate;

        public ConsignmentLineItemDTO() {
        }

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
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

