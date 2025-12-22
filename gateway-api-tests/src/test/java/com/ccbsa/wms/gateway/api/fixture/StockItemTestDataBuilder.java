package com.ccbsa.wms.gateway.api.fixture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.ccbsa.wms.gateway.api.dto.AssignLocationToStockRequest;
import com.ccbsa.wms.gateway.api.dto.AssignLocationsFEFORequest;

/**
 * Test Data Builder: StockItemTestDataBuilder
 * <p>
 * Builds test data for stock item operations in Sprint 3.
 */
public class StockItemTestDataBuilder {

    /**
     * Builds a request to assign a location to a stock item.
     */
    public static AssignLocationToStockRequest buildAssignLocationRequest(String locationId, Integer quantity) {
        AssignLocationToStockRequest request = new AssignLocationToStockRequest();
        request.setLocationId(locationId);
        request.setQuantity(quantity);
        return request;
    }

    /**
     * Builds a FEFO assignment request with a single stock item.
     */
    public static AssignLocationsFEFORequest buildFEFOAssignmentRequest(
            String stockItemId,
            BigDecimal quantity,
            LocalDate expirationDate,
            String classification) {
        AssignLocationsFEFORequest request = new AssignLocationsFEFORequest();
        List<AssignLocationsFEFORequest.StockItemAssignmentRequest> stockItems = new ArrayList<>();
        
        AssignLocationsFEFORequest.StockItemAssignmentRequest item = 
                new AssignLocationsFEFORequest.StockItemAssignmentRequest();
        item.setStockItemId(stockItemId);
        item.setQuantity(quantity);
        item.setExpirationDate(expirationDate);
        item.setClassification(classification);
        
        stockItems.add(item);
        request.setStockItems(stockItems);
        return request;
    }

    /**
     * Builds a FEFO assignment request with multiple stock items.
     */
    public static AssignLocationsFEFORequest buildFEFOAssignmentRequestWithMultipleItems(
            List<StockItemAssignmentRequestData> items) {
        AssignLocationsFEFORequest request = new AssignLocationsFEFORequest();
        List<AssignLocationsFEFORequest.StockItemAssignmentRequest> stockItems = new ArrayList<>();
        
        for (StockItemAssignmentRequestData itemData : items) {
            AssignLocationsFEFORequest.StockItemAssignmentRequest item = 
                    new AssignLocationsFEFORequest.StockItemAssignmentRequest();
            item.setStockItemId(itemData.stockItemId);
            item.setQuantity(itemData.quantity);
            item.setExpirationDate(itemData.expirationDate);
            item.setClassification(itemData.classification);
            stockItems.add(item);
        }
        
        request.setStockItems(stockItems);
        return request;
    }

    /**
     * Helper class for building stock item assignment request data.
     */
    public static class StockItemAssignmentRequestData {
        public String stockItemId;
        public BigDecimal quantity;
        public LocalDate expirationDate;
        public String classification;

        public StockItemAssignmentRequestData(String stockItemId, BigDecimal quantity, 
                                             LocalDate expirationDate, String classification) {
            this.stockItemId = stockItemId;
            this.quantity = quantity;
            this.expirationDate = expirationDate;
            this.classification = classification;
        }
    }
}

