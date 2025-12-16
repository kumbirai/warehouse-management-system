package com.ccbsa.wms.gateway.api;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;

import reactor.core.publisher.Mono;

/**
 * Gateway API integration tests for Product Management Service.
 * <p>
 * These tests validate end-to-end flow from gateway through to backend services,
 * ensuring proper routing, authentication, authorization, and data flow.
 */
@DisplayName("Product Management API Tests")
class ProductManagementTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should create product with valid data")
    void shouldCreateProduct() {
        Map<String, Object> createProductRequest = ProductTestDataBuilder.builder()
                .productCode("PROD-TEST-001")
                .description("Test Product Description")
                .primaryBarcode("6001067101234")
                .unitOfMeasure("EA")
                .category("Test Category")
                .brand("Test Brand")
                .build();

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/product-service/products")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(createProductRequest), Map.class),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.productId").exists()
                .jsonPath("$.data.productCode").isEqualTo("PROD-TEST-001")
                .jsonPath("$.data.description").isEqualTo("Test Product Description")
                .jsonPath("$.data.primaryBarcode").isEqualTo("6001067101234");
    }

    @Test
    @DisplayName("Should create product with secondary barcodes")
    void shouldCreateProductWithSecondaryBarcodes() {
        Map<String, Object> createProductRequest = ProductTestDataBuilder.builder()
                .productCode("PROD-TEST-002")
                .description("Test Product with Secondary Barcodes")
                .primaryBarcode("6001067101235")
                .unitOfMeasure("EA")
                .secondaryBarcode("6001067101236")
                .secondaryBarcode("6001067101237")
                .build();

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/product-service/products")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(createProductRequest), Map.class),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.productId").exists();
    }

    @Test
    @DisplayName("Should get product by ID")
    void shouldGetProductById() {
        // First create a product
        String productId = createTestProduct();

        // Then retrieve it
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri(String.format("/product-service/products/%s", productId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.productId").isEqualTo(productId)
                .jsonPath("$.data.productCode").exists()
                .jsonPath("$.data.description").exists()
                .jsonPath("$.data.primaryBarcode").exists();
    }

    /**
     * Helper method to create a test product and return its ID.
     *
     * @return Product ID
     */
    private String createTestProduct() {
        Map<String, Object> request = ProductTestDataBuilder.createDefault();

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
    @DisplayName("Should return 404 for non-existent product")
    void shouldReturn404ForNonExistentProduct() {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri(String.format("/product-service/products/%s", nonExistentId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should check product code uniqueness")
    void shouldCheckProductCodeUniqueness() {
        String productCode = "PROD-UNIQUE-" + System.currentTimeMillis();

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/product-service/products/check-uniqueness")
                                        .queryParam("productCode", productCode)
                                        .build())
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.productCode").isEqualTo(productCode)
                .jsonPath("$.data.isUnique").isEqualTo(true);
    }

    @Test
    @DisplayName("Should update product")
    void shouldUpdateProduct() {
        // First create a product
        String productId = createTestProduct();

        // Then update it
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("description", "Updated Description");
        updateRequest.put("primaryBarcode", "6001067109999");
        updateRequest.put("unitOfMeasure", "CS");

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .put()
                                .uri(String.format("/product-service/products/%s", productId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(updateRequest), Map.class),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.productId").isEqualTo(productId);
    }

    @Test
    @DisplayName("Should require authentication")
    void shouldRequireAuthentication() {
        Map<String, Object> createProductRequest = new HashMap<>();

        webTestClient
                .post()
                .uri("/product-service/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(createProductRequest), Map.class)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}

