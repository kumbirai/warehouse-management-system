package com.ccbsa.wms.gateway.api;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;

import reactor.core.publisher.Mono;

/**
 * Gateway API integration tests for Consignment Manual Entry functionality.
 * <p>
 * These tests validate manual consignment creation through the gateway to the Stock Management Service.
 */
@DisplayName("Consignment Manual Entry API Tests")
class ConsignmentManualEntryTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should create consignment with valid data")
    void shouldCreateConsignmentWithValidData() {
        // First, create a product that will be referenced
        String productId = createTestProduct("PROD-MANUAL-001", "6001067102001");

        // Create consignment request
        String consignmentRef = "CONS-MANUAL-" + System.currentTimeMillis();
        Map<String, Object> createConsignmentRequest = ConsignmentTestDataBuilder.builder()
                .consignmentReference(consignmentRef)
                .warehouseId("WH-001")
                .receivedBy("Test User")
                .lineItem("PROD-MANUAL-001", 100, null, null)
                .build();

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/stock-management-service/consignments")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(createConsignmentRequest), Map.class),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.consignmentId").exists()
                .jsonPath("$.data.consignmentReference").isEqualTo(consignmentRef)
                .jsonPath("$.data.status").isEqualTo("RECEIVED");
    }

    /**
     * Helper method to create a test product and return its ID.
     *
     * @param productCode Product code
     * @param barcode     Barcode
     * @return Product ID
     */
    private String createTestProduct(String productCode, String barcode) {
        Map<String, Object> request = ProductTestDataBuilder.builder()
                .productCode(productCode)
                .description("Test Product for Manual Entry")
                .primaryBarcode(barcode)
                .unitOfMeasure("EA")
                .build();

        byte[] responseBody = RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/product-service/products")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(request), Map.class),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult()
                .getResponseBody();

        return extractProductId(responseBody);
    }

    /**
     * Extracts product ID from API response.
     */
    private String extractProductId(byte[] responseBody) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode data = root.path("data");
            com.fasterxml.jackson.databind.JsonNode productIdNode = data.path("productId");

            if (productIdNode.isMissingNode()) {
                throw new RuntimeException("Product ID not found in response");
            }

            return productIdNode.asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract product ID from response", e);
        }
    }

    @Test
    @DisplayName("Should create consignment with multiple line items")
    void shouldCreateConsignmentWithMultipleLineItems() {
        // Create products
        String productId1 = createTestProduct("PROD-MANUAL-002", "6001067102002");
        String productId2 = createTestProduct("PROD-MANUAL-003", "6001067102003");

        // Create consignment request with multiple line items
        String consignmentRef = "CONS-MANUAL-MULTI-" + System.currentTimeMillis();
        Map<String, Object> createConsignmentRequest = ConsignmentTestDataBuilder.builder()
                .consignmentReference(consignmentRef)
                .warehouseId("WH-001")
                .receivedBy("Test User")
                .lineItem("PROD-MANUAL-002", 100, null, null)
                .lineItem("PROD-MANUAL-003", 150, null, "BATCH-001")
                .build();

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/stock-management-service/consignments")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(createConsignmentRequest), Map.class),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.consignmentId").exists();
    }

    @Test
    @DisplayName("Should return error for duplicate consignment reference")
    void shouldReturnErrorForDuplicateConsignmentReference() {
        // First, create a product
        String productId = createTestProduct("PROD-MANUAL-004", "6001067102004");

        // Create first consignment
        String consignmentRef = "CONS-DUP-" + System.currentTimeMillis();
        Map<String, Object> firstRequest = ConsignmentTestDataBuilder.builder()
                .consignmentReference(consignmentRef)
                .warehouseId("WH-001")
                .receivedBy("Test User")
                .lineItem("PROD-MANUAL-004", 100, null, null)
                .build();

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/stock-management-service/consignments")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(firstRequest), Map.class),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isCreated();

        // Try to create duplicate
        Map<String, Object> duplicateRequest = ConsignmentTestDataBuilder.builder()
                .consignmentReference(consignmentRef)
                .warehouseId("WH-001")
                .receivedBy("Test User")
                .lineItem("PROD-MANUAL-004", 100, null, null)
                .build();

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/stock-management-service/consignments")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(duplicateRequest), Map.class),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @DisplayName("Should return error for invalid product code")
    void shouldReturnErrorForInvalidProductCode() {
        String consignmentRef = "CONS-INVALID-PROD-" + System.currentTimeMillis();
        Map<String, Object> createConsignmentRequest = ConsignmentTestDataBuilder.builder()
                .consignmentReference(consignmentRef)
                .warehouseId("WH-001")
                .receivedBy("Test User")
                .lineItem("PROD-NONEXISTENT", 100, null, null)
                .build();

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/stock-management-service/consignments")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(createConsignmentRequest), Map.class),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @DisplayName("Should require authentication")
    void shouldRequireAuthentication() {
        Map<String, Object> createConsignmentRequest = new HashMap<>();

        webTestClient
                .post()
                .uri("/stock-management-service/consignments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(createConsignmentRequest), Map.class)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return error for missing required fields")
    void shouldReturnErrorForMissingRequiredFields() {
        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("consignmentReference", "CONS-TEST");

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/stock-management-service/consignments")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(invalidRequest), Map.class),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().is4xxClientError();
    }
}

