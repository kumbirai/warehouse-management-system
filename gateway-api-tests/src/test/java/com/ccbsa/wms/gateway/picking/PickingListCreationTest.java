package com.ccbsa.wms.gateway.picking;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.EntityExchangeResult;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.BaseIntegrationTest;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreatePickingListRequest;
import com.ccbsa.wms.gateway.api.dto.CreatePickingListResponse;
import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gateway API tests for Picking List Creation endpoints.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PickingListCreationTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static String testProductCode;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
    }

    @BeforeEach
    public void setUpPickingListCreationTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();

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
    public void testCreatePickingList_ValidRequest_Success() {
        // Arrange
        CreatePickingListRequest request = PickingListTestDataBuilder.buildCreatePickingListRequest();
        // Update product code to use test product
        request.getLoads().get(0).getOrders().get(0).getLineItems().get(0).setProductCode(testProductCode);

        // Act
        EntityExchangeResult<ApiResponse<CreatePickingListResponse>> result = authenticatedPost(
                "/api/v1/picking/picking-lists",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreatePickingListResponse>>() {
                })
                .returnResult();

        // Assert
        ApiResponse<CreatePickingListResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreatePickingListResponse response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getPickingListId()).isNotBlank();
        assertThat(response.getStatus()).isEqualTo("RECEIVED");
        assertThat(response.getLoadCount()).isGreaterThan(0);
    }

    @Test
    @Order(2)
    public void testCreatePickingList_InvalidRequest_ValidationError() {
        // Arrange
        CreatePickingListRequest request = PickingListTestDataBuilder.buildInvalidPickingListRequest();

        // Act & Assert
        authenticatedPost(
                "/api/v1/picking/picking-lists",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(3)
    public void testCreatePickingList_InvalidProduct_Error() {
        // Arrange
        CreatePickingListRequest request = PickingListTestDataBuilder.buildCreatePickingListRequest();
        request.getLoads().get(0).getOrders().get(0).getLineItems().get(0).setProductCode("INVALID-PRODUCT");

        // Act & Assert
        authenticatedPost(
                "/api/v1/picking/picking-lists",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(4)
    public void testCreatePickingList_MultipleOrders_Success() {
        // Arrange
        CreatePickingListRequest request = PickingListTestDataBuilder.buildCreatePickingListRequestWithMultipleOrders();
        // Update product codes
        request.getLoads().forEach(load ->
                load.getOrders().forEach(order ->
                        order.getLineItems().forEach(item ->
                                item.setProductCode(testProductCode))));

        // Act
        EntityExchangeResult<ApiResponse<CreatePickingListResponse>> result = authenticatedPost(
                "/api/v1/picking/picking-lists",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreatePickingListResponse>>() {
                })
                .returnResult();

        // Assert
        ApiResponse<CreatePickingListResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreatePickingListResponse response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getPickingListId()).isNotBlank();
        assertThat(response.getLoadCount()).isEqualTo(2);
    }
}
