package com.ccbsa.wms.gateway.api;

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
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.LocationResponse;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LocationManagementTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        // Note: This will be set up in first test
    }

    @BeforeEach
    public void setUpLocationTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();
        }

        // Note: Service availability check removed - tests will run and show actual errors
        // if service is not properly configured
    }

    // ==================== LOCATION CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreateWarehouse_Success() {
        // Arrange
        CreateLocationRequest request = LocationTestDataBuilder.buildWarehouseRequest();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/location-management/locations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateLocationResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateLocationResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateLocationResponse location = apiResponse.getData();
        assertThat(location).isNotNull();
        assertThat(location.getLocationId()).isNotBlank();
        assertThat(location.getCode()).isEqualTo(request.getCode());
        assertThat(location.getPath()).isEqualTo("/" + request.getCode());
    }

    @Test
    @Order(2)
    public void testCreateZone_Success() {
        // Arrange - Create warehouse first
        CreateLocationRequest warehouseRequest = LocationTestDataBuilder.buildWarehouseRequest();
        EntityExchangeResult<ApiResponse<CreateLocationResponse>> warehouseExchangeResult = authenticatedPost(
                "/api/v1/location-management/locations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                warehouseRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateLocationResponse> warehouseApiResponse = warehouseExchangeResult.getResponseBody();
        assertThat(warehouseApiResponse).isNotNull();
        assertThat(warehouseApiResponse.isSuccess()).isTrue();

        CreateLocationResponse warehouse = warehouseApiResponse.getData();
        assertThat(warehouse).isNotNull();

        CreateLocationRequest zoneRequest = LocationTestDataBuilder.buildZoneRequest(warehouse.getLocationId());

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/location-management/locations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                zoneRequest
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateLocationResponse>> zoneExchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateLocationResponse> zoneApiResponse = zoneExchangeResult.getResponseBody();
        assertThat(zoneApiResponse).isNotNull();
        assertThat(zoneApiResponse.isSuccess()).isTrue();

        CreateLocationResponse zone = zoneApiResponse.getData();
        assertThat(zone).isNotNull();
        assertThat(zone.getPath()).contains(warehouse.getCode());
    }

    @Test
    @Order(3)
    public void testCreateLocation_DuplicateCode() {
        // Arrange
        CreateLocationRequest request = LocationTestDataBuilder.buildWarehouseRequest();

        // Create first location
        authenticatedPost("/api/v1/location-management/locations", tenantAdminAuth.getAccessToken(), testTenantId, request)
                .exchange()
                .expectStatus().isCreated();

        // Act - Try to create duplicate
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/location-management/locations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest(); // or 409 CONFLICT
    }

    // ==================== LOCATION QUERY TESTS ====================

    @Test
    @Order(10)
    public void testListLocations_Success() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/location-management/locations?page=0&size=10",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    @Order(11)
    public void testGetLocationById_Success() {
        // Arrange - Create location first
        CreateLocationRequest request = LocationTestDataBuilder.buildWarehouseRequest();
        EntityExchangeResult<ApiResponse<CreateLocationResponse>> createExchangeResult = authenticatedPost(
                "/api/v1/location-management/locations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateLocationResponse> createApiResponse = createExchangeResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        assertThat(createApiResponse.isSuccess()).isTrue();

        CreateLocationResponse createdLocation = createApiResponse.getData();
        assertThat(createdLocation).isNotNull();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/location-management/locations/" + createdLocation.getLocationId(),
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<LocationResponse>> getExchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {
                })
                .returnResult();

        ApiResponse<LocationResponse> getApiResponse = getExchangeResult.getResponseBody();
        assertThat(getApiResponse).isNotNull();
        assertThat(getApiResponse.isSuccess()).isTrue();

        LocationResponse location = getApiResponse.getData();
        assertThat(location).isNotNull();
        assertThat(location.getLocationId()).isEqualTo(createdLocation.getLocationId());
    }
}

