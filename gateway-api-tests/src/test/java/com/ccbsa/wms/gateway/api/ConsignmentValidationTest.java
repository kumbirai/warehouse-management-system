package com.ccbsa.wms.gateway.api;

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
 * Gateway API integration tests for Consignment Validation functionality.
 * <p>
 * These tests validate consignment validation (confirmation) through the gateway to the Stock Management Service.
 */
@DisplayName("Consignment Validation API Tests")
class ConsignmentValidationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should validate consignment successfully")
    void shouldValidateConsignmentSuccessfully() {
        // First, create a product
        String productId = createTestProduct("PROD-VALIDATE-001", "6001067103001");

        // Create a consignment
        String consignmentId = createTestConsignment("CONS-VALIDATE-" + System.currentTimeMillis(), "PROD-VALIDATE-001");

        // Validate the consignment
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri(String.format("/stock-management-service/consignments/%s/validate", consignmentId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.consignmentId").isEqualTo(consignmentId)
                .jsonPath("$.data.newStatus").isEqualTo("CONFIRMED")
                .jsonPath("$.data.confirmedAt").exists();
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
                .description("Test Product for Validation")
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
     * Helper method to create a test consignment and return its ID.
     *
     * @param consignmentReference Consignment reference
     * @param productCode          Product code
     * @return Consignment ID
     */
    private String createTestConsignment(String consignmentReference, String productCode) {
        Map<String, Object> request = ConsignmentTestDataBuilder.builder()
                .consignmentReference(consignmentReference)
                .warehouseId("WH-001")
                .receivedBy("Test User")
                .lineItem(productCode, 100, null, null)
                .build();

        byte[] responseBody = RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/stock-management-service/consignments")
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

        return extractConsignmentId(responseBody);
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

    /**
     * Extracts consignment ID from API response.
     */
    private String extractConsignmentId(byte[] responseBody) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode data = root.path("data");
            com.fasterxml.jackson.databind.JsonNode consignmentIdNode = data.path("consignmentId");

            if (consignmentIdNode.isMissingNode()) {
                throw new RuntimeException("Consignment ID not found in response");
            }

            return consignmentIdNode.asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract consignment ID from response", e);
        }
    }

    @Test
    @DisplayName("Should return error when validating non-existent consignment")
    void shouldReturnErrorForNonExistentConsignment() {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri(String.format("/stock-management-service/consignments/%s/validate", nonExistentId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should return error when validating already confirmed consignment")
    void shouldReturnErrorForAlreadyConfirmedConsignment() {
        // First, create a product
        String productId = createTestProduct("PROD-VALIDATE-002", "6001067103002");

        // Create and validate a consignment
        String consignmentId = createTestConsignment("CONS-VALIDATE-2-" + System.currentTimeMillis(), "PROD-VALIDATE-002");
        validateConsignment(consignmentId);

        // Try to validate again
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri(String.format("/stock-management-service/consignments/%s/validate", consignmentId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    /**
     * Helper method to validate a consignment.
     *
     * @param consignmentId Consignment ID
     */
    private void validateConsignment(String consignmentId) {
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri(String.format("/stock-management-service/consignments/%s/validate", consignmentId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Should require authentication")
    void shouldRequireAuthentication() {
        String consignmentId = "00000000-0000-0000-0000-000000000000";

        webTestClient
                .post()
                .uri(String.format("/stock-management-service/consignments/%s/validate", consignmentId))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should get consignment details after validation")
    void shouldGetConsignmentDetailsAfterValidation() {
        // First, create a product
        String productId = createTestProduct("PROD-VALIDATE-003", "6001067103003");

        // Create a consignment
        String consignmentId = createTestConsignment("CONS-VALIDATE-3-" + System.currentTimeMillis(), "PROD-VALIDATE-003");

        // Validate the consignment
        validateConsignment(consignmentId);

        // Get consignment details
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri(String.format("/stock-management-service/consignments/%s", consignmentId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.consignmentId").isEqualTo(consignmentId)
                .jsonPath("$.data.status").isEqualTo("CONFIRMED")
                .jsonPath("$.data.confirmedAt").exists();
    }
}

