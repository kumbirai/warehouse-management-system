package com.ccbsa.wms.gateway.api;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.CreateReturnOrderRequest;
import com.ccbsa.wms.gateway.api.dto.CreateReturnOrderResponse;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ReturnsTestDataBuilder;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReturnsServiceTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static String testProductId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        // Note: This will be set up in first test
    }

    @BeforeEach
    public void setUpReturnsTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();

            // Create product for tests
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
        }
    }

    // ==================== RETURN ORDER CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreateReturnOrder_Success() {
        // Arrange
        CreateReturnOrderRequest request = ReturnsTestDataBuilder.buildCreateReturnOrderRequest(testProductId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/returns/orders",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateReturnOrderResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateReturnOrderResponse>>() {})
                .returnResult();
        
        ApiResponse<CreateReturnOrderResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        
        CreateReturnOrderResponse returnOrder = apiResponse.getData();
        assertThat(returnOrder).isNotNull();
        assertThat(returnOrder.getReturnOrderId()).isNotBlank();
        assertThat(returnOrder.getStatus()).isEqualTo("PENDING_AUTHORIZATION");
    }

    @Test
    @Order(2)
    public void testListReturnOrders_Success() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/returns/orders?page=0&size=10",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }
}

