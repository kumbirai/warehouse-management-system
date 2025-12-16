package com.ccbsa.wms.gateway.api;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;

import reactor.core.publisher.Mono;

/**
 * Gateway API integration tests for Product Barcode Validation functionality.
 * <p>
 * These tests validate product barcode validation through the gateway to the Product Service.
 */
@DisplayName("Product Barcode Validation API Tests")
class ProductBarcodeValidationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should validate valid barcode and return product info")
    void shouldValidateValidBarcodeAndReturnProductInfo() {
        // First, create a product with a known barcode
        String barcode = "6001067104001";
        String productId = createTestProduct("PROD-BARCODE-001", barcode);

        // Validate the barcode
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/product-service/products/validate-barcode")
                                        .queryParam("barcode", barcode)
                                        .build())
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.valid").isEqualTo(true)
                .jsonPath("$.data.productInfo").exists()
                .jsonPath("$.data.productInfo.productId").isEqualTo(productId)
                .jsonPath("$.data.productInfo.productCode").isEqualTo("PROD-BARCODE-001")
                .jsonPath("$.data.productInfo.primaryBarcode").isEqualTo(barcode)
                .jsonPath("$.data.barcodeFormat").exists();
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
                .description("Test Product for Barcode Validation")
                .primaryBarcode(barcode)
                .unitOfMeasure("EA")
                .build();

        byte[] responseBody = RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/product-service/products")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
    @DisplayName("Should return invalid for non-existent barcode")
    void shouldReturnInvalidForNonExistentBarcode() {
        String nonExistentBarcode = "9999999999999";

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/product-service/products/validate-barcode")
                                        .queryParam("barcode", nonExistentBarcode)
                                        .build())
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.valid").isEqualTo(false)
                .jsonPath("$.data.productInfo").doesNotExist()
                .jsonPath("$.data.barcodeFormat").exists();
    }

    @Test
    @DisplayName("Should validate secondary barcode")
    void shouldValidateSecondaryBarcode() {
        // Create a product with primary and secondary barcodes
        String primaryBarcode = "6001067104002";
        String secondaryBarcode = "6001067104003";
        String productId = createTestProductWithSecondaryBarcode("PROD-BARCODE-002", primaryBarcode, secondaryBarcode);

        // Validate using secondary barcode
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/product-service/products/validate-barcode")
                                        .queryParam("barcode", secondaryBarcode)
                                        .build())
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.valid").isEqualTo(true)
                .jsonPath("$.data.productInfo").exists()
                .jsonPath("$.data.productInfo.productId").isEqualTo(productId)
                .jsonPath("$.data.productInfo.primaryBarcode").isEqualTo(primaryBarcode);
    }

    /**
     * Helper method to create a test product with secondary barcodes and return its ID.
     *
     * @param productCode      Product code
     * @param primaryBarcode   Primary barcode
     * @param secondaryBarcode Secondary barcode
     * @return Product ID
     */
    private String createTestProductWithSecondaryBarcode(
            String productCode,
            String primaryBarcode,
            String secondaryBarcode) {
        Map<String, Object> request = ProductTestDataBuilder.builder()
                .productCode(productCode)
                .description("Test Product with Secondary Barcode")
                .primaryBarcode(primaryBarcode)
                .secondaryBarcode(secondaryBarcode)
                .unitOfMeasure("EA")
                .build();

        byte[] responseBody = RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/product-service/products")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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

    @Test
    @DisplayName("Should return error for invalid barcode format")
    void shouldReturnErrorForInvalidBarcodeFormat() {
        String invalidBarcode = "INVALID-BARCODE";

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/product-service/products/validate-barcode")
                                        .queryParam("barcode", invalidBarcode)
                                        .build())
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.valid").isEqualTo(false);
    }

    @Test
    @DisplayName("Should require authentication")
    void shouldRequireAuthentication() {
        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/product-service/products/validate-barcode")
                        .queryParam("barcode", "6001067104004")
                        .build())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return error for missing barcode parameter")
    void shouldReturnErrorForMissingBarcodeParameter() {
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri("/product-service/products/validate-barcode")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().is4xxClientError();
    }
}

