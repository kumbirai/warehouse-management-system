package com.ccbsa.wms.gateway.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO: AssignLocationsFEFORequest
 * <p>
 * Request DTO for FEFO location assignment.
 */
public class AssignLocationsFEFORequest {
    private List<StockItemAssignmentRequest> stockItems;

    public AssignLocationsFEFORequest() {
    }

    public List<StockItemAssignmentRequest> getStockItems() {
        return stockItems;
    }

    public void setStockItems(List<StockItemAssignmentRequest> stockItems) {
        this.stockItems = stockItems;
    }

    /**
     * Nested DTO for stock item assignment request.
     */
    public static class StockItemAssignmentRequest {
        private String stockItemId;
        private BigDecimal quantity;
        private LocalDate expirationDate;
        private String classification;

        public StockItemAssignmentRequest() {
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

