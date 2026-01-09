package com.ccbsa.wms.gateway.api;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.CsvUploadResponse;
import com.ccbsa.wms.gateway.api.dto.ProductResponse;
import com.ccbsa.wms.gateway.api.dto.UpdateProductRequest;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.TestData;
import com.ccbsa.wms.gateway.api.util.BarcodeGenerator;
import com.ccbsa.wms.gateway.api.util.CsvTestDataGenerator;
import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductManagementTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        // Note: This will be set up in first test
    }

    @BeforeEach
    public void setUpProductTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();
        }
    }

    // ==================== PRODUCT CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreateProduct_Success() {
        // Arrange
        CreateProductRequest request = ProductTestDataBuilder.buildCreateProductRequest();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateProductResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateProductResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateProductResponse product = apiResponse.getData();
        assertThat(product).isNotNull();
        assertThat(product.getProductId()).isNotBlank();
        assertThat(product.getProductCode()).isEqualTo(request.getProductCode());
        assertThat(product.getPrimaryBarcode()).isEqualTo(request.getPrimaryBarcode());
    }

    @Test
    @Order(2)
    public void testCreateProduct_WithMultipleSecondaryBarcodes() {
        // Arrange
        CreateProductRequest request = ProductTestDataBuilder.buildCreateProductRequestWithSecondaryBarcodes(3);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateProductResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateProductResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateProductResponse product = apiResponse.getData();
        assertThat(product).isNotNull();
        assertThat(product.getPrimaryBarcode()).isEqualTo(request.getPrimaryBarcode());
    }

    @Test
    @Order(3)
    public void testCreateProduct_DuplicateBarcode() {
        // Arrange
        String barcode = BarcodeGenerator.generateEAN13();
        CreateProductRequest request1 = ProductTestDataBuilder.buildCreateProductRequestWithBarcode(barcode);
        CreateProductRequest request2 = ProductTestDataBuilder.buildCreateProductRequestWithBarcode(barcode);

        // Create first product
        authenticatedPost("/api/v1/products", tenantAdminAuth.getAccessToken(), testTenantId, request1)
                .exchange()
                .expectStatus().isCreated();

        // Act - Try to create duplicate
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request2
        ).exchange();

        // Assert - Service correctly returns 409 CONFLICT for duplicate barcode
        response.expectStatus().isEqualTo(409);
    }

    @Test
    @Order(4)
    public void testCreateProduct_InvalidData() {
        // Arrange
        CreateProductRequest request = CreateProductRequest.builder()
                .productCode("") // Empty product code
                .description("Test Product Description")
                .primaryBarcode("INVALID") // Invalid barcode
                .unitOfMeasure("EA")
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    // ==================== PRODUCT QUERY TESTS ====================

    @Test
    @Order(10)
    public void testListProducts_Success() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/products?page=0&size=10",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    @Order(11)
    public void testGetProductById_Success() {
        // Arrange - Create product first
        CreateProductRequest request = ProductTestDataBuilder.buildCreateProductRequest();
        EntityExchangeResult<ApiResponse<CreateProductResponse>> createExchangeResult = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateProductResponse> createApiResponse = createExchangeResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        assertThat(createApiResponse.isSuccess()).isTrue();

        CreateProductResponse createdProduct = createApiResponse.getData();
        assertThat(createdProduct).isNotNull();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/products/" + createdProduct.getProductId(),
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<ProductResponse>> getExchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<ProductResponse>>() {
                })
                .returnResult();

        ApiResponse<ProductResponse> getApiResponse = getExchangeResult.getResponseBody();
        assertThat(getApiResponse).isNotNull();
        assertThat(getApiResponse.isSuccess()).isTrue();

        ProductResponse product = getApiResponse.getData();
        assertThat(product).isNotNull();
        assertThat(product.getProductId()).isEqualTo(createdProduct.getProductId());
    }

    @Test
    @Order(12)
    public void testGetProductByBarcode_Success() {
        // Arrange - Create product first
        String barcode = BarcodeGenerator.generateEAN13();
        CreateProductRequest request = ProductTestDataBuilder.buildCreateProductRequestWithBarcode(barcode);
        authenticatedPost("/api/v1/products", tenantAdminAuth.getAccessToken(), testTenantId, request)
                .exchange()
                .expectStatus().isCreated();

        // Act - Use validate-barcode endpoint with query parameter
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/products/validate-barcode?barcode=" + barcode,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    @Order(13)
    public void testGetProductByCode_Success() {
        // Arrange - Create product first
        CreateProductRequest request = ProductTestDataBuilder.buildCreateProductRequest();
        EntityExchangeResult<ApiResponse<CreateProductResponse>> createExchangeResult = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                })
                .returnResult();

        CreateProductResponse createdProduct = createExchangeResult.getResponseBody().getData();
        assertThat(createdProduct).isNotNull();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/products/by-code/" + createdProduct.getProductCode(),
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<ProductResponse>> getExchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<ProductResponse>>() {
                })
                .returnResult();

        ApiResponse<ProductResponse> getApiResponse = getExchangeResult.getResponseBody();
        assertThat(getApiResponse).isNotNull();
        assertThat(getApiResponse.isSuccess()).isTrue();

        ProductResponse product = getApiResponse.getData();
        assertThat(product).isNotNull();
        assertThat(product.getProductId()).isEqualTo(createdProduct.getProductId());
    }

    @Test
    @Order(14)
    public void testGetProduct_NotFound() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/products/" + UUID.randomUUID(),
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    @Order(15)
    public void testListProducts_WithPagination() {
        // Arrange - Create multiple products
        for (int i = 0; i < 15; i++) {
            CreateProductRequest request = ProductTestDataBuilder.buildCreateProductRequest();
            authenticatedPost("/api/v1/products", tenantAdminAuth.getAccessToken(), testTenantId, request)
                    .exchange()
                    .expectStatus().isCreated();
        }

        // Act - Request first page
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/products?page=0&size=10",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
        // Note: Pagination metadata validation would require ListProductsQueryResultDTO
    }

    @Test
    @Order(16)
    public void testListProducts_WithSearch() {
        // Arrange - Create products with specific names
        CreateProductRequest request1 = ProductTestDataBuilder.buildCreateProductRequest();
        CreateProductRequest request2 = ProductTestDataBuilder.buildCreateProductRequest();
        authenticatedPost("/api/v1/products", tenantAdminAuth.getAccessToken(), testTenantId, request1)
                .exchange()
                .expectStatus().isCreated();
        authenticatedPost("/api/v1/products", tenantAdminAuth.getAccessToken(), testTenantId, request2)
                .exchange()
                .expectStatus().isCreated();

        // Act - Search with search parameter
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/products?search=" + request1.getProductCode(),
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    @Order(17)
    public void testListProducts_WithCategoryFilter() {
        // Arrange - Create products with category
        CreateProductRequest request = ProductTestDataBuilder.buildCreateProductRequest();
        authenticatedPost("/api/v1/products", tenantAdminAuth.getAccessToken(), testTenantId, request)
                .exchange()
                .expectStatus().isCreated();

        // Act - Filter by category
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/products?category=" + request.getCategory(),
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    // ==================== PRODUCT UPDATE TESTS ====================

    @Test
    @Order(20)
    public void testUpdateProduct_Success() {
        // Arrange - Create product first
        CreateProductRequest createRequest = ProductTestDataBuilder.buildCreateProductRequest();
        EntityExchangeResult<ApiResponse<CreateProductResponse>> createExchangeResult = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                createRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                })
                .returnResult();

        CreateProductResponse createdProduct = createExchangeResult.getResponseBody().getData();
        assertThat(createdProduct).isNotNull();

        // Prepare update request
        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .description("Updated Description")
                .primaryBarcode(createdProduct.getPrimaryBarcode())
                .unitOfMeasure("EA")
                .category("BEVERAGES")
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/products/" + createdProduct.getProductId(),
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                updateRequest
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    @Order(21)
    public void testUpdateProduct_AddSecondaryBarcodes() {
        // Arrange - Create product without secondary barcodes
        CreateProductRequest createRequest = ProductTestDataBuilder.buildCreateProductRequest();
        EntityExchangeResult<ApiResponse<CreateProductResponse>> createExchangeResult = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                createRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                })
                .returnResult();

        CreateProductResponse createdProduct = createExchangeResult.getResponseBody().getData();
        assertThat(createdProduct).isNotNull();

        // Prepare update request with secondary barcodes
        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .description(createRequest.getDescription())
                .primaryBarcode(createdProduct.getPrimaryBarcode())
                .unitOfMeasure(createRequest.getUnitOfMeasure())
                .secondaryBarcodes(List.of(
                        BarcodeGenerator.generateEAN13(),
                        BarcodeGenerator.generateEAN13()
                ))
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/products/" + createdProduct.getProductId(),
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                updateRequest
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    @Order(22)
    public void testUpdateProduct_ChangePrimaryBarcode() {
        // Arrange - Create product
        CreateProductRequest createRequest = ProductTestDataBuilder.buildCreateProductRequest();
        EntityExchangeResult<ApiResponse<CreateProductResponse>> createExchangeResult = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                createRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                })
                .returnResult();

        CreateProductResponse createdProduct = createExchangeResult.getResponseBody().getData();
        assertThat(createdProduct).isNotNull();

        // Prepare update request with new primary barcode
        String newBarcode = BarcodeGenerator.generateEAN13();
        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .description(createRequest.getDescription())
                .primaryBarcode(newBarcode)
                .unitOfMeasure(createRequest.getUnitOfMeasure())
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/products/" + createdProduct.getProductId(),
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                updateRequest
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    @Order(23)
    public void testUpdateProduct_DuplicateBarcode() {
        // Arrange - Create two products
        String barcode1 = BarcodeGenerator.generateEAN13();
        String barcode2 = BarcodeGenerator.generateEAN13();

        CreateProductRequest request1 = ProductTestDataBuilder.buildCreateProductRequestWithBarcode(barcode1);
        CreateProductRequest request2 = ProductTestDataBuilder.buildCreateProductRequestWithBarcode(barcode2);

        // Create first product (we only need it to exist, don't need the response)
        authenticatedPost("/api/v1/products", tenantAdminAuth.getAccessToken(), testTenantId, request1)
                .exchange()
                .expectStatus().isCreated();

        EntityExchangeResult<ApiResponse<CreateProductResponse>> create2 = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request2
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                })
                .returnResult();

        CreateProductResponse product2 = create2.getResponseBody().getData();

        // Try to update product2 with product1's barcode (duplicate)
        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .description(request2.getDescription())
                .primaryBarcode(barcode1) // Duplicate barcode from product1
                .unitOfMeasure(request2.getUnitOfMeasure())
                .build();

        // Act - This should fail
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/products/" + product2.getProductId(),
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                updateRequest
        ).exchange();

        // Assert - Should fail with 409 CONFLICT for duplicate barcode
        response.expectStatus().isEqualTo(409);
    }

    @Test
    @Order(24)
    public void testUpdateProduct_NotFound() {
        // Arrange
        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .description("Updated Description")
                .primaryBarcode(BarcodeGenerator.generateEAN13())
                .unitOfMeasure("EA")
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/products/" + UUID.randomUUID(),
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                updateRequest
        ).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    @Order(25)
    public void testUpdateProduct_InvalidData() {
        // Arrange - Create product first
        CreateProductRequest createRequest = ProductTestDataBuilder.buildCreateProductRequest();
        EntityExchangeResult<ApiResponse<CreateProductResponse>> createExchangeResult = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                createRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                })
                .returnResult();

        CreateProductResponse createdProduct = createExchangeResult.getResponseBody().getData();

        // Prepare invalid update request
        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .description("") // Empty description
                .primaryBarcode("INVALID") // Invalid barcode
                .unitOfMeasure("EA")
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/products/" + createdProduct.getProductId(),
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                updateRequest
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    // ==================== CSV UPLOAD TESTS ====================

    @Test
    @Order(30)
    public void testUploadProductsCsv_Success() throws Exception {
        // Arrange - Create CSV file
        Path tempDir = Files.createTempDirectory("product-csv-test");
        File csvFile = CsvTestDataGenerator.generateProductCsv(tempDir, 3);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(csvFile));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/products/upload-csv")
                .headers(headers -> {
                    RequestHeaderHelper.addAuthHeaders(headers, tenantAdminAuth.getAccessToken());
                    RequestHeaderHelper.addTenantHeader(headers, testTenantId);
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CsvUploadResponse>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CsvUploadResponse>>() {
                })
                .returnResult();

        ApiResponse<CsvUploadResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CsvUploadResponse uploadResponse = apiResponse.getData();
        assertThat(uploadResponse).isNotNull();
        assertThat(uploadResponse.getTotalRows()).isGreaterThan(0);
        // Success count = created + updated
        int successCount = (uploadResponse.getCreatedCount() != null ? uploadResponse.getCreatedCount() : 0) +
                          (uploadResponse.getUpdatedCount() != null ? uploadResponse.getUpdatedCount() : 0);
        assertThat(successCount).isGreaterThan(0);

        // Cleanup
        Files.deleteIfExists(csvFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    @Order(31)
    public void testUploadProductsCsv_WithDuplicateBarcodes() throws Exception {
        // Arrange - Create CSV file with duplicate barcodes
        Path tempDir = Files.createTempDirectory("product-csv-test");
        File csvFile = tempDir.resolve("products-duplicate.csv").toFile();

        try (java.io.FileWriter writer = new java.io.FileWriter(csvFile)) {
            writer.write("product_code,description,primary_barcode,unit_of_measure,category\n");
            String duplicateBarcode = BarcodeGenerator.generateEAN13();
            writer.write(String.format("SKU-001,Product 1,%s,EA,BEVERAGES\n", duplicateBarcode));
            writer.write(String.format("SKU-002,Product 2,%s,EA,BEVERAGES\n", duplicateBarcode));
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(csvFile));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/products/upload-csv")
                .headers(headers -> {
                    RequestHeaderHelper.addAuthHeaders(headers, tenantAdminAuth.getAccessToken());
                    RequestHeaderHelper.addTenantHeader(headers, testTenantId);
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CsvUploadResponse>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CsvUploadResponse>>() {
                })
                .returnResult();

        ApiResponse<CsvUploadResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        
        CsvUploadResponse uploadResponse = apiResponse.getData();
        assertThat(uploadResponse).isNotNull();
        // Should have errors for duplicate barcode
        assertThat(uploadResponse.getFailureCount()).isGreaterThan(0);
        assertThat(uploadResponse.getErrors()).isNotEmpty();

        // Cleanup
        Files.deleteIfExists(csvFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    @Order(32)
    public void testUploadProductsCsv_WithInvalidData() throws Exception {
        // Arrange - Create CSV file with invalid data
        // Note: The parser throws an exception when it encounters invalid rows during parsing,
        // causing the entire request to fail with 400 BAD_REQUEST
        Path tempDir = Files.createTempDirectory("product-csv-test");
        File csvFile = tempDir.resolve("products-invalid.csv").toFile();

        try (java.io.FileWriter writer = new java.io.FileWriter(csvFile)) {
            writer.write("product_code,description,primary_barcode,unit_of_measure,category\n");
            // First row has empty product_code - parser will fail on this row
            writer.write(",Missing ProductCode,7891234567890,EA,BEVERAGES\n");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(csvFile));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/products/upload-csv")
                .headers(headers -> {
                    RequestHeaderHelper.addAuthHeaders(headers, tenantAdminAuth.getAccessToken());
                    RequestHeaderHelper.addTenantHeader(headers, testTenantId);
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .exchange();

        // Assert - Parser throws exception for invalid rows, causing 400 BAD_REQUEST
        response.expectStatus().isBadRequest();

        // Cleanup
        Files.deleteIfExists(csvFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    // ==================== BARCODE VALIDATION TESTS ====================

    @Test
    @Order(40)
    public void testValidateBarcode_ValidEAN13() {
        // Arrange - Create product with EAN-13 barcode
        String barcode = BarcodeGenerator.generateEAN13();
        CreateProductRequest request = ProductTestDataBuilder.buildCreateProductRequestWithBarcode(barcode);
        authenticatedPost("/api/v1/products", tenantAdminAuth.getAccessToken(), testTenantId, request)
                .exchange()
                .expectStatus().isCreated();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/products/validate-barcode?barcode=" + barcode,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    @Order(41)
    public void testValidateBarcode_NonExistent() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/products/validate-barcode?barcode=9999999999999",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert - Should return 404 or indicate barcode not found
        // Note: Service may return 404 or 200 with empty result
        int status = response.returnResult(Void.class).getStatus().value();
        assertThat(status).isIn(200, 404);
    }

    @Test
    @Order(42)
    public void testCreateProduct_InvalidBarcodeFormat() {
        // Arrange
        CreateProductRequest request = CreateProductRequest.builder()
                .productCode("SKU-INVALID-BARCODE")
                .description("Test Product")
                .primaryBarcode("12345") // Too short
                .unitOfMeasure("EA")
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    // ==================== TENANT ISOLATION TESTS ====================

    @Test
    @Order(50)
    public void testTenantIsolation_ListProducts() {
        // Arrange - Create products in current tenant
        CreateProductRequest request = ProductTestDataBuilder.buildCreateProductRequest();
        authenticatedPost("/api/v1/products", tenantAdminAuth.getAccessToken(), testTenantId, request)
                .exchange()
                .expectStatus().isCreated();

        // Act - List products should only return products from current tenant
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
        // Note: Full tenant isolation verification would require creating products in different tenants
    }

    // ==================== AUTHORIZATION TESTS ====================

    @Test
    @Order(60)
    public void testCreateProduct_WithoutAuthentication() {
        // Arrange
        CreateProductRequest request = ProductTestDataBuilder.buildCreateProductRequest();

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isUnauthorized();
    }

    @Test
    @Order(61)
    public void testListProducts_WithoutAuthentication() {
        // Act
        WebTestClient.ResponseSpec response = webTestClient.get()
                .uri("/api/v1/products")
                .exchange();

        // Assert
        response.expectStatus().isUnauthorized();
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @Order(70)
    public void testCreateProduct_WithSpecialCharacters() {
        // Arrange
        CreateProductRequest request = CreateProductRequest.builder()
                .productCode(TestData.productSKU()) // Use unique product code
                .description("Coca-Cola® 500ml - Special Characters Test")
                .primaryBarcode(BarcodeGenerator.generateEAN13())
                .unitOfMeasure("EA")
                .category("BEVERAGES")
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isCreated();
    }

    @Test
    @Order(71)
    public void testCreateProduct_WithVeryLongDescription() {
        // Arrange
        String longDescription = "A".repeat(501); // Exceeds 500 character limit
        CreateProductRequest request = CreateProductRequest.builder()
                .productCode("SKU-LONG-DESC")
                .description(longDescription)
                .primaryBarcode(BarcodeGenerator.generateEAN13())
                .unitOfMeasure("EA")
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert - Should fail validation
        response.expectStatus().isBadRequest();
    }

    @Test
    @Order(72)
    public void testCreateProduct_WithEmptySecondaryBarcodes() {
        // Arrange
        CreateProductRequest request = CreateProductRequest.builder()
                .productCode(TestData.productSKU()) // Use unique product code
                .description("Product with empty secondary barcodes")
                .primaryBarcode(BarcodeGenerator.generateEAN13())
                .unitOfMeasure("EA")
                .secondaryBarcodes(List.of())
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isCreated();
    }

    @Test
    @Order(73)
    public void testCreateProduct_DuplicateSecondaryBarcode() {
        // Arrange
        String secondaryBarcode = BarcodeGenerator.generateEAN13();
        CreateProductRequest request1 = CreateProductRequest.builder()
                .productCode(TestData.productSKU()) // Use unique product code
                .description("Product 1")
                .primaryBarcode(BarcodeGenerator.generateEAN13())
                .unitOfMeasure("EA")
                .secondaryBarcodes(List.of(secondaryBarcode))
                .build();

        CreateProductRequest request2 = CreateProductRequest.builder()
                .productCode(TestData.productSKU()) // Use unique product code
                .description("Product 2")
                .primaryBarcode(secondaryBarcode) // Using secondary barcode as primary
                .unitOfMeasure("EA")
                .build();

        // Create first product
        authenticatedPost("/api/v1/products", tenantAdminAuth.getAccessToken(), testTenantId, request1)
                .exchange()
                .expectStatus().isCreated();

        // Act - Try to create second product with duplicate barcode
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request2
        ).exchange();

        // Assert - Should fail with duplicate barcode error
        int status = response.returnResult(Void.class).getStatus().value();
        assertThat(status).isGreaterThanOrEqualTo(400);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Create authenticated PUT request with Bearer token and tenant context.
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedPut(String uri, String accessToken, String tenantId, Object requestBody) {
        return webTestClient.put()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    com.ccbsa.wms.gateway.api.util.RequestHeaderHelper.addAuthHeaders(headers, accessToken);
                    com.ccbsa.wms.gateway.api.util.RequestHeaderHelper.addTenantHeader(headers, tenantId);
                })
                .bodyValue(requestBody);
    }
}

