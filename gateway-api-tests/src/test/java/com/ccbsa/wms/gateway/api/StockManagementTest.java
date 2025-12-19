package com.ccbsa.wms.gateway.api;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest;
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentResponse;
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockManagementTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static String testProductId;
    private static String testLocationId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        // Note: This will be set up in first test
    }

    @BeforeEach
    public void setUpStockTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();

            // Create product and location for tests
            CreateProductRequest productRequest = ProductTestDataBuilder.buildCreateProductRequest();
            EntityExchangeResult<ApiResponse<CreateProductResponse>> productExchangeResult = authenticatedPost(
                    "/api/v1/products",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    productRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {})
                    .returnResult();
            
            ApiResponse<CreateProductResponse> productApiResponse = productExchangeResult.getResponseBody();
            assertThat(productApiResponse).isNotNull();
            assertThat(productApiResponse.isSuccess()).isTrue();
            CreateProductResponse product = productApiResponse.getData();
            assertThat(product).isNotNull();
            testProductId = product.getProductId();

            CreateLocationRequest locationRequest = LocationTestDataBuilder.buildWarehouseRequest();
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> locationExchangeResult = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    locationRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {})
                    .returnResult();
            
            ApiResponse<CreateLocationResponse> locationApiResponse = locationExchangeResult.getResponseBody();
            assertThat(locationApiResponse).isNotNull();
            assertThat(locationApiResponse.isSuccess()).isTrue();
            CreateLocationResponse location = locationApiResponse.getData();
            assertThat(location).isNotNull();
            testLocationId = location.getLocationId();
        }
    }

    // ==================== CONSIGNMENT RECEIPT TESTS ====================

    @Test
    @Order(1)
    public void testCreateConsignment_Success() {
        // Arrange
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequest(
                testProductId, testLocationId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {})
                .returnResult();
        
        ApiResponse<CreateConsignmentResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        
        CreateConsignmentResponse consignment = apiResponse.getData();
        assertThat(consignment).isNotNull();
        assertThat(consignment.getConsignmentId()).isNotBlank();
        assertThat(consignment.getQuantity()).isEqualTo(request.getQuantity());
    }

    @Test
    @Order(2)
    public void testCreateConsignment_InvalidProduct() {
        // Arrange
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequest(
                randomUUID(), testLocationId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest(); // or 404 NOT FOUND
    }

    @Test
    @Order(3)
    public void testCreateConsignment_NegativeQuantity() {
        // Arrange
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestWithQuantity(
                testProductId, testLocationId, -10);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    // ==================== STOCK QUERY TESTS ====================

    @Test
    @Order(10)
    public void testListConsignments_Success() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/stock-management/consignments?page=0&size=10",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }
}

