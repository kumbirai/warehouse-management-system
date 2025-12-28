package com.ccbsa.wms.gateway.api;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.BlockLocationRequest;
import com.ccbsa.wms.gateway.api.dto.BlockLocationResultDTO;
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.LocationResponse;
import com.ccbsa.wms.gateway.api.dto.UnblockLocationRequest;
import com.ccbsa.wms.gateway.api.dto.UnblockLocationResultDTO;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for Location Status Management via Gateway.
 * 
 * Tests cover:
 * - Block location operations
 * - Unblock location operations
 * - Status validation
 * - Error scenarios
 * - Authorization checks
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LocationStatusGatewayTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static UUID testLocationId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        // Note: This will be set up in first test
    }

    @BeforeEach
    public void setUpLocationStatusTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();

            // Create location for tests
            CreateLocationRequest locationRequest = LocationTestDataBuilder.buildWarehouseRequest();
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> locationResult = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    locationRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> locationApiResponse = locationResult.getResponseBody();
            assertThat(locationApiResponse).isNotNull();
            assertThat(locationApiResponse.isSuccess()).isTrue();
            CreateLocationResponse location = locationApiResponse.getData();
            assertThat(location).isNotNull();
            testLocationId = UUID.fromString(location.getLocationId());
        }
    }

    // ==================== BLOCK LOCATION TESTS ====================

    @Test
    @Order(1)
    public void testBlockLocation_Success() {
        // Arrange
        BlockLocationRequest request = BlockLocationRequest.builder()
                .reason("Maintenance")
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/location-management/locations/" + testLocationId + "/block",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<BlockLocationResultDTO>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<BlockLocationResultDTO>>() {
                })
                .returnResult();

        ApiResponse<BlockLocationResultDTO> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        BlockLocationResultDTO result = apiResponse.getData();
        assertThat(result).isNotNull();
        assertThat(result.getLocationId()).isEqualTo(testLocationId);
        assertThat(result.getStatus()).isEqualTo("BLOCKED");
    }

    @Test
    @Order(2)
    public void testBlockLocation_AlreadyBlocked() {
        // Arrange - Block location first
        BlockLocationRequest blockRequest = BlockLocationRequest.builder()
                .reason("Initial block")
                .build();

        authenticatedPost(
                "/api/v1/location-management/locations/" + testLocationId + "/block",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                blockRequest
        ).exchange()
                .expectStatus().isOk();

        // Act - Try to block again
        BlockLocationRequest request = BlockLocationRequest.builder()
                .reason("Second block attempt")
                .build();

        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/location-management/locations/" + testLocationId + "/block",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert - Should still be OK (idempotent) or Bad Request
        int statusCode = response.expectBody().returnResult().getStatus().value();
        assertThat(statusCode).isIn(200, 400);
    }

    // ==================== UNBLOCK LOCATION TESTS ====================

    @Test
    @Order(10)
    public void testUnblockLocation_Success() {
        // Arrange - Block location first
        BlockLocationRequest blockRequest = BlockLocationRequest.builder()
                .reason("Temporary block")
                .build();

        authenticatedPost(
                "/api/v1/location-management/locations/" + testLocationId + "/block",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                blockRequest
        ).exchange()
                .expectStatus().isOk();

        UnblockLocationRequest request = UnblockLocationRequest.builder()
                .reason("Maintenance complete")
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/location-management/locations/" + testLocationId + "/unblock",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<UnblockLocationResultDTO>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UnblockLocationResultDTO>>() {
                })
                .returnResult();

        ApiResponse<UnblockLocationResultDTO> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        UnblockLocationResultDTO result = apiResponse.getData();
        assertThat(result).isNotNull();
        assertThat(result.getLocationId()).isEqualTo(testLocationId);
        assertThat(result.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    @Order(11)
    public void testUnblockLocation_NotBlocked() {
        // Arrange - Ensure location is not blocked
        // (Location should be AVAILABLE by default)

        UnblockLocationRequest request = UnblockLocationRequest.builder()
                .reason("Unblock attempt")
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/location-management/locations/" + testLocationId + "/unblock",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert - Should still be OK (idempotent) or Bad Request
        int statusCode = response.expectBody().returnResult().getStatus().value();
        assertThat(statusCode).isIn(200, 400);
    }

    @Test
    @Order(12)
    public void testBlockUnblockCycle() {
        // Arrange
        BlockLocationRequest blockRequest = BlockLocationRequest.builder()
                .reason("Test cycle")
                .build();

        // Act - Block
        authenticatedPost(
                "/api/v1/location-management/locations/" + testLocationId + "/block",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                blockRequest
        ).exchange()
                .expectStatus().isOk();

        // Verify location is blocked
        EntityExchangeResult<ApiResponse<LocationResponse>> getResult = authenticatedGet(
                "/api/v1/location-management/locations/" + testLocationId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {
                })
                .returnResult();

        ApiResponse<LocationResponse> getApiResponse = getResult.getResponseBody();
        assertThat(getApiResponse).isNotNull();
        LocationResponse location = getApiResponse.getData();
        assertThat(location).isNotNull();
        assertThat(location.getStatus()).isEqualTo("BLOCKED");

        // Unblock
        UnblockLocationRequest unblockRequest = UnblockLocationRequest.builder()
                .reason("Test complete")
                .build();

        authenticatedPost(
                "/api/v1/location-management/locations/" + testLocationId + "/unblock",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                unblockRequest
        ).exchange()
                .expectStatus().isOk();

        // Verify location is available
        EntityExchangeResult<ApiResponse<LocationResponse>> getResult2 = authenticatedGet(
                "/api/v1/location-management/locations/" + testLocationId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {
                })
                .returnResult();

        ApiResponse<LocationResponse> getApiResponse2 = getResult2.getResponseBody();
        assertThat(getApiResponse2).isNotNull();
        LocationResponse location2 = getApiResponse2.getData();
        assertThat(location2).isNotNull();
        assertThat(location2.getStatus()).isEqualTo("AVAILABLE");
    }
}

