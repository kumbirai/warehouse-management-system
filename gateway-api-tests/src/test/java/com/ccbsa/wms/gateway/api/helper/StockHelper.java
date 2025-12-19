package com.ccbsa.wms.gateway.api.helper;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest;
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentResponse;
import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper for stock-related test operations.
 */
public class StockHelper {

    private final WebTestClient webTestClient;

    public StockHelper(WebTestClient webTestClient) {
        this.webTestClient = webTestClient;
    }

    /**
     * Create consignment and return consignment ID.
     */
    public String createConsignment(AuthenticationResult auth, String tenantId, String productId, String locationId) {
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequest(productId, locationId);

        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> exchangeResult = webTestClient.post()
                .uri("/api/v1/stock-management/consignments")
                .headers(headers -> {
                    headers.setBearerAuth(auth.getAccessToken());
                    headers.set("X-Tenant-Id", tenantId);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateConsignmentResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse)
                .as("API response should not be null")
                .isNotNull();
        assertThat(apiResponse.isSuccess())
                .as("API response should be successful")
                .isTrue();

        CreateConsignmentResponse response = apiResponse.getData();
        assertThat(response)
                .as("Create consignment response data should not be null")
                .isNotNull();

        return response.getConsignmentId();
    }

    /**
     * Create consignment with specific quantity.
     */
    public String createConsignmentWithQuantity(
            AuthenticationResult auth, String tenantId, String productId, String locationId, int quantity) {
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestWithQuantity(
                productId, locationId, quantity);

        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> exchangeResult = webTestClient.post()
                .uri("/api/v1/stock-management/consignments")
                .headers(headers -> {
                    headers.setBearerAuth(auth.getAccessToken());
                    headers.set("X-Tenant-Id", tenantId);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateConsignmentResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse)
                .as("API response should not be null")
                .isNotNull();
        assertThat(apiResponse.isSuccess())
                .as("API response should be successful")
                .isTrue();

        CreateConsignmentResponse response = apiResponse.getData();
        assertThat(response)
                .as("Create consignment response data should not be null")
                .isNotNull();

        return response.getConsignmentId();
    }
}

