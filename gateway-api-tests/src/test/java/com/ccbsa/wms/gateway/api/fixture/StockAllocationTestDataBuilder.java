package com.ccbsa.wms.gateway.api.fixture;

import java.util.UUID;

import com.ccbsa.wms.gateway.api.dto.CreateStockAllocationRequest;

/**
 * Test Data Builder: StockAllocationTestDataBuilder
 * <p>
 * Builds test data for stock allocation operations in Sprint 4.
 */
public class StockAllocationTestDataBuilder {

    /**
     * Builds a request to create a stock allocation.
     */
    public static CreateStockAllocationRequest buildCreateStockAllocationRequest(
            UUID productId,
            UUID locationId,
            Integer quantity,
            String referenceId) {
        return CreateStockAllocationRequest.builder()
                .productId(productId)
                .locationId(locationId)
                .quantity(quantity)
                .allocationType("PICKING_ORDER")
                .referenceId(referenceId)
                .notes("Test allocation")
                .build();
    }

    /**
     * Builds a request to create a stock allocation for FEFO (no location specified).
     */
    public static CreateStockAllocationRequest buildCreateStockAllocationRequestFEFO(
            UUID productId,
            Integer quantity,
            String referenceId) {
        return CreateStockAllocationRequest.builder()
                .productId(productId)
                .locationId(null) // Null for FEFO allocation
                .quantity(quantity)
                .allocationType("PICKING_ORDER")
                .referenceId(referenceId)
                .notes("FEFO allocation")
                .build();
    }

    /**
     * Builds a request to create a reservation allocation.
     */
    public static CreateStockAllocationRequest buildCreateReservationRequest(
            UUID productId,
            UUID locationId,
            Integer quantity,
            String referenceId) {
        return CreateStockAllocationRequest.builder()
                .productId(productId)
                .locationId(locationId)
                .quantity(quantity)
                .allocationType("RESERVATION")
                .referenceId(referenceId)
                .notes("Reservation allocation")
                .build();
    }
}

