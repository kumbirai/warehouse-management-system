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
import com.ccbsa.wms.gateway.api.dto.PickingListQueryResult;
import com.ccbsa.wms.gateway.api.dto.ListPickingListsQueryResult;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gateway API tests for Picking List Query endpoints.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PickingListQueryTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static String testProductCode;
    private static String testPickingListId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
    }

    @BeforeEach
    public void setUpPickingListQueryTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();

            // Create product
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
            testProductCode = productRequest.getProductCode();

            // Create picking list for query tests
            CreatePickingListRequest request = PickingListTestDataBuilder.buildCreatePickingListRequest();
            request.getLoads().get(0).getOrders().get(0).getLineItems().get(0).setProductCode(testProductCode);

            EntityExchangeResult<ApiResponse<CreatePickingListResponse>> pickingListResult = authenticatedPost(
                    "/api/v1/picking/picking-lists",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    request
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreatePickingListResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreatePickingListResponse> pickingListApiResponse = pickingListResult.getResponseBody();
            assertThat(pickingListApiResponse).isNotNull();
            assertThat(pickingListApiResponse.isSuccess()).isTrue();
            testPickingListId = pickingListApiResponse.getData().getPickingListId();
        }
    }

    @Test
    @Order(1)
    public void testGetPickingList_ValidId_Success() {
        // Act
        EntityExchangeResult<ApiResponse<PickingListQueryResult>> result = authenticatedGet(
                "/api/v1/picking/picking-lists/" + testPickingListId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<PickingListQueryResult>>() {
                })
                .returnResult();

        // Assert
        ApiResponse<PickingListQueryResult> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        PickingListQueryResult response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testPickingListId);
        assertThat(response.getLoadCount()).isGreaterThan(0);
    }

    @Test
    @Order(2)
    public void testGetPickingList_InvalidId_NotFound() {
        // Act & Assert
        authenticatedGet(
                "/api/v1/picking/picking-lists/invalid-id",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(3)
    public void testListPickingLists_NoFilters_Success() {
        // Act
        EntityExchangeResult<ApiResponse<ListPickingListsQueryResult>> result = authenticatedGet(
                "/api/v1/picking/picking-lists?page=0&size=20",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<ListPickingListsQueryResult>>() {
                })
                .returnResult();

        // Assert
        ApiResponse<ListPickingListsQueryResult> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ListPickingListsQueryResult response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getPickingLists()).isNotEmpty();
    }

    @Test
    @Order(4)
    public void testListPickingLists_WithStatusFilter_Success() {
        // Act
        EntityExchangeResult<ApiResponse<ListPickingListsQueryResult>> result = authenticatedGet(
                "/api/v1/picking/picking-lists?page=0&size=20&status=RECEIVED",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<ListPickingListsQueryResult>>() {
                })
                .returnResult();

        // Assert
        ApiResponse<ListPickingListsQueryResult> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ListPickingListsQueryResult response = apiResponse.getData();
        assertThat(response).isNotNull();
        // All returned picking lists should have RECEIVED status
        response.getPickingLists().forEach(list ->
                assertThat(list.getStatus()).isEqualTo("RECEIVED"));
    }

    @Test
    @Order(5)
    public void testListPickingLists_WithPagination_Success() {
        // Act
        EntityExchangeResult<ApiResponse<ListPickingListsQueryResult>> result = authenticatedGet(
                "/api/v1/picking/picking-lists?page=0&size=10",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<ListPickingListsQueryResult>>() {
                })
                .returnResult();

        // Assert
        ApiResponse<ListPickingListsQueryResult> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ListPickingListsQueryResult response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getPickingLists().size()).isLessThanOrEqualTo(10);
    }
}
