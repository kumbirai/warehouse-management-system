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
                .productId(UUID.fromString(productId))
                .locationId(sourceLocationId != null ? UUID.fromString(sourceLocationId) : null)
                .quantity(quantity)
                .allocationType("PICKING_ORDER")
                .referenceId(referenceId)
                .build();
    }

    public static CreateStockMovementRequest buildCreateStockMovementRequest(
            String productId, String sourceLocationId, String targetLocationId, int quantity) {
        return CreateStockMovementRequest.builder()
                .productId(UUID.fromString(productId))
                .sourceLocationId(UUID.fromString(sourceLocationId))
                .destinationLocationId(UUID.fromString(targetLocationId))
                .quantity(quantity)
                .movementType("INTER_STORAGE")  // Use valid MovementType enum value - INTER_STORAGE for movements between storage locations
                .reason("REORGANIZATION")  // Use valid MovementReason enum value instead of "Optimization"
                .build();
    }

    public static CreateStockAdjustmentRequest buildCreateStockAdjustmentRequest(
            String productId, String locationId, String adjustmentType, int quantity, String reason) {
        // Normalize reason to enum value if human-readable string is provided
        String normalizedReason = normalizeAdjustmentReason(reason);
        return CreateStockAdjustmentRequest.builder()
                .productId(UUID.fromString(productId))
                .locationId(locationId != null ? UUID.fromString(locationId) : null)
                .stockItemId(null)
                .adjustmentType(adjustmentType)
                .quantity(quantity)
                .reason(normalizedReason)
                .build();
    }

    /**
     * Normalizes human-readable adjustment reason strings to enum values.
     * Supports both enum values (STOCK_COUNT, DAMAGE, etc.) and human-readable strings.
     *
     * @param reason The reason string (enum value or human-readable)
     * @return Normalized enum value
     */
    private static String normalizeAdjustmentReason(String reason) {
        if (reason == null) {
            return "OTHER";
        }
        String upperReason = reason.toUpperCase().trim();
        
        // If already an enum value, return as-is
        if (upperReason.equals("STOCK_COUNT") || upperReason.equals("DAMAGE") || 
            upperReason.equals("CORRECTION") || upperReason.equals("THEFT") || 
            upperReason.equals("EXPIRATION") || upperReason.equals("OTHER")) {
            return upperReason;
        }
        
        // Map human-readable strings to enum values
        if (upperReason.contains("STOCK") && upperReason.contains("COUNT")) {
            return "STOCK_COUNT";
        }
        if (upperReason.contains("DAMAGE") || upperReason.contains("DAMAGED")) {
            return "DAMAGE";
        }
        if (upperReason.contains("CORRECTION") || upperReason.contains("CORRECT")) {
            return "CORRECTION";
        }
        if (upperReason.contains("THEFT") || upperReason.contains("STOLEN")) {
            return "THEFT";
        }
        if (upperReason.contains("EXPIR") || upperReason.contains("EXPIRED")) {
            return "EXPIRATION";
        }
        
        // Default to OTHER if no match
        return "OTHER";
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

