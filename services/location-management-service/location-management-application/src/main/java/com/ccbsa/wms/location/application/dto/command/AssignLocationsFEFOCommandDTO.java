package com.ccbsa.wms.location.application.dto.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * Command DTO: AssignLocationsFEFOCommandDTO
 * <p>
 * API request DTO for FEFO location assignment.
 */
public class AssignLocationsFEFOCommandDTO {
    @NotEmpty(message = "At least one stock item is required")
    @Valid
    private List<StockItemAssignmentRequestDTO> stockItems;

    public AssignLocationsFEFOCommandDTO() {
    }

    public List<StockItemAssignmentRequestDTO> getStockItems() {
        return stockItems;
    }

    public void setStockItems(List<StockItemAssignmentRequestDTO> stockItems) {
        this.stockItems = stockItems;
    }

    /**
     * Nested DTO for stock item assignment request.
     */
    public static class StockItemAssignmentRequestDTO {
        private String stockItemId;
        private BigDecimal quantity;
        private LocalDate expirationDate;
        private String classification;

        public StockItemAssignmentRequestDTO() {
        }

        public String getStockItemId() {
            return stockItemId;
        }

        public void setStockItemId(String stockItemId) {
            this.stockItemId = stockItemId;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public LocalDate getExpirationDate() {
            return expirationDate;
        }

        public void setExpirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
        }

        public String getClassification() {
            return classification;
        }

        public void setClassification(String classification) {
            this.classification = classification;
        }
    }
}

