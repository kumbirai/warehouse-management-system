package com.ccbsa.wms.gateway.api.fixture;

import java.util.UUID;

import com.ccbsa.wms.gateway.api.dto.CreateStockAdjustmentRequest;

/**
 * Test Data Builder: StockAdjustmentTestDataBuilder
 * <p>
 * Builds test data for stock adjustment operations in Sprint 4.
 */
public class StockAdjustmentTestDataBuilder {

    /**
     * Builds a request to increase stock.
     */
    public static CreateStockAdjustmentRequest buildIncreaseStockRequest(UUID productId, UUID locationId, Integer quantity, String reason) {
        return CreateStockAdjustmentRequest.builder().productId(productId).locationId(locationId).stockItemId(null).adjustmentType("INCREASE").quantity(quantity).reason(reason)
                .notes("Stock count adjustment").build();
    }

    /**
     * Builds a request to decrease stock.
     */
    public static CreateStockAdjustmentRequest buildDecreaseStockRequest(UUID productId, UUID locationId, Integer quantity, String reason) {
        return CreateStockAdjustmentRequest.builder().productId(productId).locationId(locationId).stockItemId(null).adjustmentType("DECREASE").quantity(quantity).reason(reason)
                .notes("Stock adjustment").build();
    }

    /**
     * Builds a request to correct stock (set to specific quantity).
     */
    public static CreateStockAdjustmentRequest buildCorrectionStockRequest(UUID productId, UUID locationId, Integer quantity, String reason) {
        return CreateStockAdjustmentRequest.builder().productId(productId).locationId(locationId).stockItemId(null).adjustmentType("CORRECTION").quantity(quantity).reason(reason)
                .notes("Stock correction").build();
    }

    /**
     * Builds a request with authorization code for large adjustments.
     */
    public static CreateStockAdjustmentRequest buildLargeAdjustmentRequest(UUID productId, UUID locationId, Integer quantity, String reason, String authorizationCode) {
        return CreateStockAdjustmentRequest.builder().productId(productId).locationId(locationId).stockItemId(null).adjustmentType("INCREASE").quantity(quantity).reason(reason)
                .notes("Large adjustment requiring authorization").authorizationCode(authorizationCode).build();
    }
}

