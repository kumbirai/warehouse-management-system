package com.ccbsa.wms.gateway.picking;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.BaseIntegrationTest;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.UploadPickingListCsvResponse;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gateway API tests for Picking List CSV Upload endpoints.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PickingListCsvUploadTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static String testProductCode;
    private static String testWarehouseId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
    }

    @BeforeEach
    public void setUpPickingListCsvUploadTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();

            // Create warehouse for tests
            CreateLocationResponse warehouse = createLocation(
                    tenantAdminAuth.getAccessToken(),
                    testTenantId
            );
            testWarehouseId = warehouse.getLocationId();

            // Create product for tests
            CreateProductRequest productRequest = ProductTestDataBuilder.buildCreateProductRequest();
            EntityExchangeResult<ApiResponse<CreateProductResponse>> productResult = authenticatedPost(
                    "/api/v1/products",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    productRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateProductResponse> productApiResponse = productResult.getResponseBody();
            assertThat(productApiResponse).isNotNull();
            assertThat(productApiResponse.isSuccess()).isTrue();
            testProductCode = productRequest.getProductCode();
        }
    }

    @Test
    @Order(1)
    public void testUploadPickingListCsv_ValidCsv_Success() throws Exception {
        // Arrange - Create CSV content with valid data (all required fields)
        String csvContent = String.format(
                "LoadNumber,OrderNumber,OrderLineNumber,CustomerCode,CustomerName,Priority,ProductCode,Quantity,WarehouseId,Notes\n" +
                "LOAD-001,ORD-001,1,CUST-001,Acme Corp,HIGH,%s,10,%s,Urgent order\n" +
                "LOAD-001,ORD-001,2,CUST-001,Acme Corp,HIGH,%s,5,%s,",
                testProductCode, testWarehouseId, testProductCode, testWarehouseId);

        Path tempDir = Files.createTempDirectory("picking-list-csv-test");
        File csvFile = createCsvFile(csvContent, tempDir, "test-picking-list.csv");

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", new FileSystemResource(csvFile));

        // Act
        EntityExchangeResult<ApiResponse<UploadPickingListCsvResponse>> result = webTestClient
                .post()
                .uri("/api/v1/picking/picking-lists/upload-csv")
                .header("Authorization", "Bearer " + tenantAdminAuth.getAccessToken())
                .header("X-Tenant-Id", testTenantId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(formData))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UploadPickingListCsvResponse>>() {
                })
                .returnResult();

        // Assert
        ApiResponse<UploadPickingListCsvResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        UploadPickingListCsvResponse response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getTotalRows()).isGreaterThan(0);
        assertThat(response.getSuccessfulRows()).isGreaterThan(0);
        assertThat(response.getErrorRows()).isEqualTo(0);

        // Cleanup
        Files.deleteIfExists(csvFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    @Order(2)
    public void testUploadPickingListCsv_InvalidFormat_Error() throws Exception {
        // Arrange - Invalid CSV format (missing required headers)
        String csvContent = "Invalid,CSV,Format\nRow1,Row2";

        Path tempDir = Files.createTempDirectory("picking-list-csv-test");
        File csvFile = createCsvFile(csvContent, tempDir, "invalid.csv");

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", new FileSystemResource(csvFile));

        // Act & Assert
        webTestClient
                .post()
                .uri("/api/v1/picking/picking-lists/upload-csv")
                .header("Authorization", "Bearer " + tenantAdminAuth.getAccessToken())
                .header("X-Tenant-Id", testTenantId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(formData))
                .exchange()
                .expectStatus().isBadRequest();

        // Cleanup
        Files.deleteIfExists(csvFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    @Order(3)
    public void testUploadPickingListCsv_InvalidProduct_Error() throws Exception {
        // Arrange - CSV with invalid product code (all required fields present)
        String csvContent = String.format(
                "LoadNumber,OrderNumber,OrderLineNumber,CustomerCode,CustomerName,Priority,ProductCode,Quantity,WarehouseId,Notes\n" +
                "LOAD-001,ORD-001,1,CUST-001,Acme Corp,HIGH,INVALID-PRODUCT,10,%s,",
                testWarehouseId);

        Path tempDir = Files.createTempDirectory("picking-list-csv-test");
        File csvFile = createCsvFile(csvContent, tempDir, "invalid-product.csv");

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", new FileSystemResource(csvFile));

        // Act
        EntityExchangeResult<ApiResponse<UploadPickingListCsvResponse>> result = webTestClient
                .post()
                .uri("/api/v1/picking/picking-lists/upload-csv")
                .header("Authorization", "Bearer " + tenantAdminAuth.getAccessToken())
                .header("X-Tenant-Id", testTenantId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(formData))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UploadPickingListCsvResponse>>() {
                })
                .returnResult();

        // Assert - Should have errors
        ApiResponse<UploadPickingListCsvResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        UploadPickingListCsvResponse response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getErrorRows()).isGreaterThan(0);
        assertThat(response.getErrors()).isNotEmpty();

        // Cleanup
        Files.deleteIfExists(csvFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    /**
     * Creates a CSV file with the given content in a temporary directory.
     *
     * @param content  the CSV content to write
     * @param tempDir  the temporary directory to create the file in
     * @param filename the name of the CSV file
     * @return the created File
     * @throws Exception if file creation fails
     */
    private File createCsvFile(String content, Path tempDir, String filename) throws Exception {
        File csvFile = tempDir.resolve(filename).toFile();
        try (java.io.FileWriter writer = new java.io.FileWriter(csvFile)) {
            writer.write(content);
        }
        return csvFile;
    }
}
