package com.ccbsa.wms.gateway.api.fixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest;
import com.ccbsa.wms.gateway.api.dto.CreateStockAllocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateStockMovementRequest;
import com.ccbsa.wms.gateway.api.dto.CreateStockAdjustmentRequest;

/**
 * Builder for creating stock consignment test data.
 */
public class ConsignmentTestDataBuilder {

    public static CreateConsignmentRequest buildCreateConsignmentRequest(String productId, String locationId) {
        return CreateConsignmentRequest.builder()
                .productId(productId)
                .locationId(locationId)
                .quantity(TestData.stockQuantity())
                .batchNumber(TestData.batchNumber())
                .expirationDate(TestData.expirationDate())
                .manufactureDate(TestData.manufactureDate())
                .supplierReference(TestData.supplierReference())
                .receivedDate(LocalDate.now())
                .build();
    }

    public static CreateConsignmentRequest buildCreateConsignmentRequestWithQuantity(
            String productId, String locationId, int quantity) {
        return CreateConsignmentRequest.builder()
                .productId(productId)
                .locationId(locationId)
                .quantity(quantity)
                .batchNumber(TestData.batchNumber())
                .expirationDate(TestData.expirationDate())
                .manufactureDate(TestData.manufactureDate())
                .supplierReference(TestData.supplierReference())
                .receivedDate(LocalDate.now())
                .build();
    }

    public static CreateConsignmentRequest buildCreateConsignmentRequestWithExpiration(
            String productId, String locationId, LocalDate expirationDate) {
        return CreateConsignmentRequest.builder()
                .productId(productId)
                .locationId(locationId)
                .quantity(TestData.stockQuantity())
                .batchNumber(TestData.batchNumber())
                .expirationDate(expirationDate)
                .manufactureDate(TestData.manufactureDate())
                .supplierReference(TestData.supplierReference())
                .receivedDate(LocalDate.now())
                .build();
    }

    public static CreateConsignmentRequest buildCreateConsignmentRequestWithBatch(
            String productId, String locationId, String batchNumber) {
        return CreateConsignmentRequest.builder()
                .productId(productId)
                .locationId(locationId)
                .quantity(TestData.stockQuantity())
                .batchNumber(batchNumber)
                .expirationDate(TestData.expirationDate())
                .manufactureDate(TestData.manufactureDate())
                .supplierReference(TestData.supplierReference())
                .receivedDate(LocalDate.now())
                .build();
    }

    public static CreateStockAllocationRequest buildCreateStockAllocationRequest(
            String productId, String sourceLocationId, int quantity, String referenceId) {
        return CreateStockAllocationRequest.builder()
                .productId(productId)
                .sourceLocationId(sourceLocationId)
                .quantity(quantity)
                .allocationType("PICKING_ORDER")
                .referenceId(referenceId)
                .build();
    }

    public static CreateStockMovementRequest buildCreateStockMovementRequest(
            String productId, String sourceLocationId, String targetLocationId, int quantity) {
        return CreateStockMovementRequest.builder()
                .productId(productId)
                .sourceLocationId(sourceLocationId)
                .targetLocationId(targetLocationId)
                .quantity(quantity)
                .movementType("RELOCATION")
                .reason("Optimization")
                .build();
    }

    public static CreateStockAdjustmentRequest buildCreateStockAdjustmentRequest(
            String consignmentId, String adjustmentType, int quantity, String reason) {
        return CreateStockAdjustmentRequest.builder()
                .consignmentId(consignmentId)
                .adjustmentType(adjustmentType)
                .quantity(quantity)
                .reason(reason)
                .build();
    }

    /**
     * Builds a CreateConsignmentRequest with the new API structure (consignmentReference, warehouseId, lineItems).
     * This is the correct structure for Sprint 3 and matches the actual API.
     */
    public static CreateConsignmentRequest buildCreateConsignmentRequestV2(
            String warehouseId, String productCode, Integer quantity, LocalDate expirationDate) {
        List<CreateConsignmentRequest.ConsignmentLineItem> lineItems = new ArrayList<>();
        CreateConsignmentRequest.ConsignmentLineItem lineItem = CreateConsignmentRequest.ConsignmentLineItem.builder()
                .productCode(productCode)
                .quantity(quantity)
                .expirationDate(expirationDate)
                .build();
        lineItems.add(lineItem);

        return CreateConsignmentRequest.builder()
                .consignmentReference("CONS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .warehouseId(warehouseId)
                .receivedAt(LocalDateTime.now())
                .receivedBy("Test User")
                .lineItems(lineItems)
                .build();
    }

    /**
     * Builds a CreateConsignmentRequest with expiration date for classification testing.
     */
    public static CreateConsignmentRequest buildCreateConsignmentRequestV2WithExpiration(
            String warehouseId, String productCode, LocalDate expirationDate) {
        return buildCreateConsignmentRequestV2(warehouseId, productCode, TestData.stockQuantity(), expirationDate);
    }
}

