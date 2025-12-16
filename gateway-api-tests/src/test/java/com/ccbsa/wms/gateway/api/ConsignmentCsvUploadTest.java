package com.ccbsa.wms.gateway.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;
import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gateway API integration tests for Consignment CSV Upload functionality.
 * <p>
 * These tests validate CSV file upload through the gateway to the Stock Management Service.
 */
@DisplayName("Consignment CSV Upload API Tests")
class ConsignmentCsvUploadTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should require authentication for CSV upload")
    void shouldRequireAuthenticationForCsvUpload() {
        String csvContent = ConsignmentTestDataBuilder.createCsvContent(
                "CONS-TEST-001",
                "PROD-TEST-001",
                100,
                "WH-001");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "consignments.csv";
            }
        });

        webTestClient
                .post()
                .uri("/stock-management-service/consignments/upload-csv")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should reject request without file")
    void shouldRejectRequestWithoutFile() {
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/stock-management-service/consignments/upload-csv")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.MULTIPART_FORM_DATA),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @DisplayName("Should upload valid CSV and create consignments")
    void shouldUploadValidCsvAndCreateConsignments() {
        // First, create a product that will be referenced in the CSV
        String productId = createTestProduct("PROD-CSV-001", "6001067101234");

        // Create CSV content with valid data
        String consignmentRef = "CONS-CSV-" + System.currentTimeMillis();
        String csvContent = ConsignmentTestDataBuilder.createCsvContent(
                consignmentRef,
                "PROD-CSV-001",
                100,
                "WH-001");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "consignments.csv";
            }
        });

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/stock-management-service/consignments/upload-csv")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(body)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.totalRows").exists()
                .jsonPath("$.data.createdCount").exists()
                .jsonPath("$.data.errorCount").exists();
    }

    /**
     * Helper method to create a test product and return its ID.
     *
     * @param productCode Product code
     * @param barcode     Barcode
     * @return Product ID
     */
    private String createTestProduct(String productCode, String barcode) {
        Map<String, Object> request = com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder.builder()
                .productCode(productCode)
                .description("Test Product for CSV Upload")
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
    @DisplayName("Should handle CSV with multiple line items for same consignment")
    void shouldHandleCsvWithMultipleLineItems() {
        // Create products
        String productId1 = createTestProduct("PROD-CSV-002", "6001067101235");
        String productId2 = createTestProduct("PROD-CSV-003", "6001067101236");

        // Create CSV with multiple line items
        String consignmentRef = "CONS-CSV-MULTI-" + System.currentTimeMillis();
        List<Map<String, Object>> lineItems = List.of(
                createLineItemMap("PROD-CSV-002", 100, "WH-001"),
                createLineItemMap("PROD-CSV-003", 150, "WH-001"));

        String csvContent = ConsignmentTestDataBuilder.createCsvContentWithLineItems(
                consignmentRef,
                lineItems);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "consignments.csv";
            }
        });

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/stock-management-service/consignments/upload-csv")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(body)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.createdCount").isEqualTo(1);
    }

    /**
     * Creates a line item map for CSV content.
     */
    private Map<String, Object> createLineItemMap(String productCode, Integer quantity, String warehouseId) {
        Map<String, Object> item = new HashMap<>();
        item.put("productCode", productCode);
        item.put("quantity", quantity);
        item.put("warehouseId", warehouseId);
        return item;
    }

    @Test
    @DisplayName("Should return errors for invalid product codes in CSV")
    void shouldReturnErrorsForInvalidProductCodes() {
        String consignmentRef = "CONS-CSV-ERROR-" + System.currentTimeMillis();
        String csvContent = ConsignmentTestDataBuilder.createCsvContent(
                consignmentRef,
                "PROD-NONEXISTENT-001",
                100,
                "WH-001");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "consignments.csv";
            }
        });

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/stock-management-service/consignments/upload-csv")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(body)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.errorCount").value(count -> {
                    assertThat(count).isNotNull();
                    assertThat(((Number) count).intValue()).isGreaterThan(0);
                })
                .jsonPath("$.data.errors").isArray();
    }
}

