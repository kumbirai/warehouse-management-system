package com.ccbsa.wms.gateway.api.fixture;

import java.util.UUID;

import com.ccbsa.wms.gateway.api.dto.CancelStockMovementRequest;
import com.ccbsa.wms.gateway.api.dto.CreateStockMovementRequest;

/**
 * Test Data Builder: StockMovementTestDataBuilder
 * <p>
 * Builds test data for stock movement operations in Sprint 4.
 */
public class StockMovementTestDataBuilder {

    /**
     * Builds a request to create a stock movement.
     */
    public static CreateStockMovementRequest buildCreateStockMovementRequest(
            String stockItemId,
            UUID productId,
            UUID sourceLocationId,
            UUID destinationLocationId,
            Integer quantity) {
        return CreateStockMovementRequest.builder()
                .stockItemId(stockItemId)
                .productId(productId)
                .sourceLocationId(sourceLocationId)
                .destinationLocationId(destinationLocationId)
                .quantity(quantity)
                .movementType("INTER_STORAGE")
                .reason("REPLENISHMENT")
                .build();
    }

    /**
     * Builds a request to create a stock movement with specific type and reason.
     */
    public static CreateStockMovementRequest buildCreateStockMovementRequestWithType(
            String stockItemId,
            UUID productId,
            UUID sourceLocationId,
            UUID destinationLocationId,
            Integer quantity,
            String movementType,
            String reason) {
        return CreateStockMovementRequest.builder()
                .stockItemId(stockItemId)
                .productId(productId)
                .sourceLocationId(sourceLocationId)
                .destinationLocationId(destinationLocationId)
                .quantity(quantity)
                .movementType(movementType)
                .reason(reason)
                .build();
    }

    /**
     * Builds a request to cancel a stock movement.
     */
    public static CancelStockMovementRequest buildCancelStockMovementRequest(String reason) {
        return CancelStockMovementRequest.builder()
                .cancellationReason(reason)
                .build();
    }
}

