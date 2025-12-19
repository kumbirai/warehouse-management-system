package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest;

import java.time.LocalDate;

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
}

