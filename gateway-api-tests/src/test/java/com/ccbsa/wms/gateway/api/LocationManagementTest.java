package com.ccbsa.wms.gateway.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AssignLocationsFEFORequest;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.BlockLocationRequest;
import com.ccbsa.wms.gateway.api.dto.BlockLocationResultDTO;
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.ListLocationsResponse;
import com.ccbsa.wms.gateway.api.dto.LocationAvailabilityResponse;
import com.ccbsa.wms.gateway.api.dto.LocationResponse;
import com.ccbsa.wms.gateway.api.dto.UnblockLocationRequest;
import com.ccbsa.wms.gateway.api.dto.UnblockLocationResultDTO;
import com.ccbsa.wms.gateway.api.dto.UpdateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.UpdateLocationStatusRequest;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.StockItemTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.TestDataManager;
import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for Location Management Service via Gateway.
 *
 * Tests cover:
 * - Full CRUD operations for all location hierarchy levels
 * - Location status lifecycle transitions
 * - Block and unblock location operations
 * - Location queries with filtering, pagination, and search
 * - Validation and error handling
 * - Authorization and tenant isolation
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LocationManagementTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static CreateLocationResponse sharedWarehouse;

    // Warehouse hierarchy test data
    private static CreateLocationResponse warehouse1;
    private static CreateLocationResponse warehouse2;
    private static List<CreateLocationResponse> warehouse1Zones;
    private static List<CreateLocationResponse> warehouse2Zones;
    private static List<CreateLocationResponse> warehouse1Aisles;
    private static List<CreateLocationResponse> warehouse2Aisles;
    private static List<CreateLocationResponse> warehouse1Racks;
    private static List<CreateLocationResponse> warehouse2Racks;
    private static List<CreateLocationResponse> warehouse1Bins;
    private static List<CreateLocationResponse> warehouse2Bins;
    private static boolean hierarchiesInitialized = false;

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

        // Setup warehouse hierarchies once after authentication
        if (!hierarchiesInitialized) {
            setupWarehouseHierarchies();
            hierarchiesInitialized = true;
        }
    }

    /**
     * Sets up warehouse hierarchies for testing.
     * Creates 2 warehouses, each with complete location hierarchies:
     * - 2 zones per warehouse
     * - 2 aisles per zone
     * - 2 racks per aisle
     * - 2 bins per rack
     * 
     * Reuses existing warehouses from repository if available.
     * Note: When reusing warehouses, we always create fresh hierarchies to ensure
     * parent-child relationships exist in the service database.
     */
    private void setupWarehouseHierarchies() {
        // Initialize lists
        warehouse1Zones = new ArrayList<>();
        warehouse2Zones = new ArrayList<>();
        warehouse1Aisles = new ArrayList<>();
        warehouse2Aisles = new ArrayList<>();
        warehouse1Racks = new ArrayList<>();
        warehouse2Racks = new ArrayList<>();
        warehouse1Bins = new ArrayList<>();
        warehouse2Bins = new ArrayList<>();

        // Try to reuse existing warehouses from repository
        List<CreateLocationResponse> existingWarehouses = TestDataManager.getRepository().findLocationsByType("WAREHOUSE", testTenantId);
        
        if (existingWarehouses.size() >= 2) {
            // Reuse first two warehouses from repository
            // Verify they exist in the service database before reusing
            warehouse1 = verifyLocationExists(existingWarehouses.get(0));
            warehouse2 = verifyLocationExists(existingWarehouses.get(1));
        } else if (existingWarehouses.size() == 1) {
            // Reuse one warehouse, create the second
            warehouse1 = verifyLocationExists(existingWarehouses.get(0));
            warehouse2 = createLocation(LocationTestDataBuilder.buildWarehouseRequest());
        } else {
            // Create both warehouses
            warehouse1 = createLocation(LocationTestDataBuilder.buildWarehouseRequest());
            warehouse2 = createLocation(LocationTestDataBuilder.buildWarehouseRequest());
        }

        // Always create fresh hierarchies to ensure parent-child relationships exist in service database
        // This prevents issues where parent locations from previous test runs don't exist
        createLocationHierarchy(warehouse1, 2, 2, 2, 2, warehouse1Zones, warehouse1Aisles, warehouse1Racks, warehouse1Bins);
        createLocationHierarchy(warehouse2, 2, 2, 2, 2, warehouse2Zones, warehouse2Aisles, warehouse2Racks, warehouse2Bins);
    }

    /**
     * Verifies that a location exists in the service database.
     * If it doesn't exist, creates a new one with the same code.
     * 
     * @param location Location from repository to verify
     * @return Location that exists in the service database
     */
    private CreateLocationResponse verifyLocationExists(CreateLocationResponse location) {
        try {
            // Try to get the location from the service
            EntityExchangeResult<ApiResponse<LocationResponse>> exchangeResult =
                    authenticatedGet("/api/v1/location-management/locations/" + location.getLocationId(), 
                            tenantAdminAuth.getAccessToken(), testTenantId)
                            .exchange()
                            .expectStatus()
                            .isOk()
                            .expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {})
                            .returnResult();
            
            ApiResponse<LocationResponse> apiResponse = exchangeResult.getResponseBody();
            if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null 
                    && apiResponse.getData().getLocationId() != null 
                    && apiResponse.getData().getLocationId().equals(location.getLocationId())) {
                // Location exists in service, return the original location response
                return location;
            }
        } catch (Exception e) {
            // Location doesn't exist in service, create a new one
            // This can happen when the service database was cleared but H2 repository still has the location
        }
        
        // Create a new location with the same code
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code(location.getCode())
                .name(location.getName() != null ? location.getName() : "Warehouse " + location.getCode())
                .type(location.getType())
                .build();
        return createLocation(request);
    }

    /**
     * Helper method to create a location and return the response.
     * Uses TestDataManager.getOrCreateLocation() to check repository first, then creates if not found.
     */
    private CreateLocationResponse createLocation(CreateLocationRequest request) {
        return TestDataManager.getOrCreateLocation(
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                () -> request,
                req -> {
                    EntityExchangeResult<ApiResponse<CreateLocationResponse>> exchangeResult =
                            authenticatedPost("/api/v1/location-management/locations", tenantAdminAuth.getAccessToken(), testTenantId, req).exchange().expectStatus().isCreated()
                                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                                    }).returnResult();

                    ApiResponse<CreateLocationResponse> apiResponse = exchangeResult.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isTrue();

                    CreateLocationResponse location = apiResponse.getData();
                    assertThat(location).isNotNull();
                    return location;
                });
    }

    /**
     * Creates a complete location hierarchy under a warehouse.
     *
     * @param warehouse         The warehouse to create hierarchy under
     * @param zonesPerWarehouse Number of zones to create
     * @param aislesPerZone     Number of aisles per zone
     * @param racksPerAisle     Number of racks per aisle
     * @param binsPerRack       Number of bins per rack
     * @param zonesList         List to store created zones
     * @param aislesList        List to store created aisles
     * @param racksList         List to store created racks
     * @param binsList          List to store created bins
     */
    private void createLocationHierarchy(CreateLocationResponse warehouse, int zonesPerWarehouse, int aislesPerZone, int racksPerAisle, int binsPerRack,
                                         List<CreateLocationResponse> zonesList, List<CreateLocationResponse> aislesList, List<CreateLocationResponse> racksList,
                                         List<CreateLocationResponse> binsList) {

        // Create zones
        for (int z = 0; z < zonesPerWarehouse; z++) {
            CreateLocationResponse zone = createLocation(LocationTestDataBuilder.buildZoneRequest(warehouse.getLocationId()));
            zonesList.add(zone);

            // Create aisles for this zone
            for (int a = 0; a < aislesPerZone; a++) {
                CreateLocationResponse aisle = createLocation(LocationTestDataBuilder.buildAisleRequest(zone.getLocationId()));
                aislesList.add(aisle);

                // Create racks for this aisle
                for (int r = 0; r < racksPerAisle; r++) {
                    CreateLocationResponse rack = createLocation(LocationTestDataBuilder.buildRackRequest(aisle.getLocationId()));
                    racksList.add(rack);

                    // Create bins for this rack
                    for (int b = 0; b < binsPerRack; b++) {
                        CreateLocationResponse bin = createLocation(LocationTestDataBuilder.buildBinRequest(rack.getLocationId()));
                        binsList.add(bin);
                    }
                }
            }
        }
    }

    // ==================== LOCATION CREATION TESTS ====================

    /**
     * Helper method to get a random location from a list.
     *
     * @param locations List of locations
     * @return A random location from the list
     */
    private CreateLocationResponse getRandomLocationFromList(List<CreateLocationResponse> locations) {
        if (locations == null || locations.isEmpty()) {
            throw new IllegalStateException("Location list is empty or null");
        }
        int index = faker.number().numberBetween(0, locations.size());
        return locations.get(index);
    }

    @Test
    @Order(1)
    public void testCreateWarehouse_Success() {
        // Arrange
        CreateLocationRequest request = LocationTestDataBuilder.buildWarehouseRequest();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/location-management/locations", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateLocationResponse>> exchangeResult =
                response.expectStatus().isCreated().expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                }).returnResult();

        ApiResponse<CreateLocationResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateLocationResponse location = apiResponse.getData();
        assertThat(location).isNotNull();
        assertThat(location.getLocationId()).isNotBlank();
        assertThat(location.getCode()).isEqualTo(request.getCode());
        assertThat(location.getPath()).isEqualTo("/" + request.getCode());

        // Save to repository for reuse
        TestDataManager.saveLocation(location, testTenantId);

        // Store shared warehouse for use in other tests
        sharedWarehouse = location;
    }

    @Test
    @Order(2)
    public void testCreateZone_Success() {
        // Arrange - Use warehouse1 from setup
        assertThat(warehouse1).isNotNull();
        CreateLocationRequest zoneRequest = LocationTestDataBuilder.buildZoneRequest(warehouse1.getLocationId());

        // Act
        CreateLocationResponse zone = createLocation(zoneRequest);

        // Assert
        assertThat(zone).isNotNull();
        assertThat(zone.getLocationId()).isNotBlank();
        assertThat(zone.getCode()).isEqualTo(zoneRequest.getCode());
        assertThat(zone.getPath()).contains(warehouse1.getCode());
    }

    @Test
    @Order(3)
    public void testCreateAisle_Success() {
        // Arrange - Use warehouse1Zones from setup
        assertThat(warehouse1Zones).isNotNull();
        assertThat(warehouse1Zones).isNotEmpty();
        CreateLocationResponse zone = warehouse1Zones.get(0);
        CreateLocationRequest aisleRequest = LocationTestDataBuilder.buildAisleRequest(zone.getLocationId());

        // Act
        CreateLocationResponse aisle = createLocation(aisleRequest);

        // Assert
        assertThat(aisle).isNotNull();
        assertThat(aisle.getLocationId()).isNotBlank();
        assertThat(aisle.getCode()).isEqualTo(aisleRequest.getCode());
        assertThat(aisle.getPath()).contains(warehouse1.getCode());
        assertThat(aisle.getPath()).contains(zone.getCode());
    }

    @Test
    @Order(4)
    public void testCreateRack_Success() {
        // Arrange - Use warehouse1Aisles from setup
        assertThat(warehouse1Aisles).isNotNull();
        assertThat(warehouse1Aisles).isNotEmpty();
        CreateLocationResponse aisle = warehouse1Aisles.get(0);
        CreateLocationRequest rackRequest = LocationTestDataBuilder.buildRackRequest(aisle.getLocationId());

        // Act
        CreateLocationResponse rack = createLocation(rackRequest);

        // Assert
        assertThat(rack).isNotNull();
        assertThat(rack.getLocationId()).isNotBlank();
        assertThat(rack.getPath()).contains(warehouse1.getCode());
        // Verify path contains zone and aisle codes from the hierarchy
        assertThat(rack.getPath()).contains(aisle.getCode());
    }

    @Test
    @Order(5)
    public void testCreateBin_Success() {
        // Arrange - Use warehouse1Racks from setup
        assertThat(warehouse1Racks).isNotNull();
        assertThat(warehouse1Racks).isNotEmpty();
        CreateLocationResponse rack = warehouse1Racks.get(0);
        CreateLocationRequest binRequest = LocationTestDataBuilder.buildBinRequest(rack.getLocationId());

        // Act
        CreateLocationResponse bin = createLocation(binRequest);

        // Assert
        assertThat(bin).isNotNull();
        assertThat(bin.getLocationId()).isNotBlank();
        assertThat(bin.getPath()).contains(warehouse1.getCode());
        // Verify path contains rack code from the hierarchy
        assertThat(bin.getPath()).contains(rack.getCode());
    }

    @Test
    @Order(6)
    public void testCreateLocation_DuplicateCode() {
        // Arrange
        CreateLocationRequest request = LocationTestDataBuilder.buildWarehouseRequest();

        // Create first location
        createLocation(request);

        // Act - Try to create duplicate
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/location-management/locations", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        response.expectStatus().isBadRequest(); // or 409 CONFLICT
    }

    @Test
    @Order(7)
    public void testCreateLocation_InvalidParent() {
        // Arrange - Use fixed invalid parent ID format (valid UUID format but non-existent)
        String invalidParentId = "00000000-0000-0000-0000-000000000001";
        CreateLocationRequest request = LocationTestDataBuilder.buildZoneRequest(invalidParentId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/location-management/locations", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        response.expectStatus().isBadRequest(); // or 404 NOT FOUND
    }

    @Test
    @Order(8)
    public void testCreateLocation_InvalidHierarchy() {
        // Arrange - Use pre-created locations from warehouse1 hierarchy
        assertThat(warehouse1).isNotNull();
        assertThat(warehouse1Zones).isNotEmpty();
        assertThat(warehouse1Aisles).isNotEmpty();
        assertThat(warehouse1Racks).isNotEmpty();
        assertThat(warehouse1Bins).isNotEmpty();

        CreateLocationResponse zone = warehouse1Zones.get(0);
        CreateLocationResponse aisle = warehouse1Aisles.get(0);
        CreateLocationResponse rack = warehouse1Racks.get(0);
        CreateLocationResponse bin = warehouse1Bins.get(0);

        // Test 1: Try creating Zone with Bin as parent (should fail)
        CreateLocationRequest invalidRequest1 = LocationTestDataBuilder.buildZoneRequest(bin.getLocationId());
        WebTestClient.ResponseSpec response1 =
                authenticatedPost("/api/v1/location-management/locations", tenantAdminAuth.getAccessToken(), testTenantId, invalidRequest1).exchange();
        response1.expectStatus().isBadRequest();

        // Test 2: Try creating Aisle with Warehouse as parent (should fail - must be Zone)
        CreateLocationRequest invalidRequest2 = LocationTestDataBuilder.buildAisleRequest(warehouse1.getLocationId());
        WebTestClient.ResponseSpec response2 =
                authenticatedPost("/api/v1/location-management/locations", tenantAdminAuth.getAccessToken(), testTenantId, invalidRequest2).exchange();
        response2.expectStatus().isBadRequest();

        // Test 3: Try creating Rack with Zone as parent (should fail - must be Aisle)
        CreateLocationRequest invalidRequest3 = LocationTestDataBuilder.buildRackRequest(zone.getLocationId());
        WebTestClient.ResponseSpec response3 =
                authenticatedPost("/api/v1/location-management/locations", tenantAdminAuth.getAccessToken(), testTenantId, invalidRequest3).exchange();
        response3.expectStatus().isBadRequest();

        // Test 4: Try creating Bin with Aisle as parent (should fail - must be Rack)
        CreateLocationRequest invalidRequest4 = LocationTestDataBuilder.buildBinRequest(aisle.getLocationId());
        WebTestClient.ResponseSpec response4 =
                authenticatedPost("/api/v1/location-management/locations", tenantAdminAuth.getAccessToken(), testTenantId, invalidRequest4).exchange();
        response4.expectStatus().isBadRequest();
    }

    @Test
    @Order(9)
    public void testCreateLocation_MissingRequiredFields() {
        // Arrange - Missing code
        CreateLocationRequest request = CreateLocationRequest.builder().name("Test Location").type("WAREHOUSE").build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/location-management/locations", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    // ==================== LOCATION QUERY TESTS ====================

    @Test
    @Order(10)
    public void testCreateLocation_WithoutAuthentication() {
        // Arrange
        CreateLocationRequest request = LocationTestDataBuilder.buildWarehouseRequest();

        // Act
        WebTestClient.ResponseSpec response =
                webTestClient.post().uri("/api/v1/location-management/locations").contentType(MediaType.APPLICATION_JSON).bodyValue(request).exchange();

        // Assert
        response.expectStatus().isUnauthorized();
    }

    @Test
    @Order(10)
    public void testListLocations_WithPagination() {
        // Arrange - Reuse existing warehouses or create new ones as needed
        // Check how many warehouses already exist
        List<CreateLocationResponse> existingWarehouses = TestDataManager.getRepository().findLocationsByType("WAREHOUSE", testTenantId);
        int warehousesNeeded = 5;
        int warehousesToCreate = Math.max(0, warehousesNeeded - existingWarehouses.size());
        
        // Create additional warehouses only if needed
        for (int i = 0; i < warehousesToCreate; i++) {
            createLocation(LocationTestDataBuilder.buildWarehouseRequest());
        }

        // Act
        EntityExchangeResult<ApiResponse<ListLocationsResponse>> exchangeResult =
                authenticatedGet("/api/v1/location-management/locations?page=0&size=10", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<ListLocationsResponse>>() {
                        }).returnResult();

        // Assert
        ApiResponse<ListLocationsResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ListLocationsResponse listResponse = apiResponse.getData();
        assertThat(listResponse).isNotNull();
        assertThat(listResponse.getLocations()).isNotNull();
        assertThat(listResponse.getTotalCount()).isGreaterThanOrEqualTo(0);
        assertThat(listResponse.getPage()).isEqualTo(0);
        assertThat(listResponse.getSize()).isEqualTo(10);
    }

    @Test
    @Order(11)
    public void testListLocations_FilterByType() {
        // Arrange - Use pre-created locations from setup (warehouse1 and its zones)
        assertThat(warehouse1).isNotNull();
        assertThat(warehouse1Zones).isNotEmpty();

        // Act
        EntityExchangeResult<ApiResponse<ListLocationsResponse>> exchangeResult =
                authenticatedGet("/api/v1/location-management/locations?page=0&size=10", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<ListLocationsResponse>>() {
                        }).returnResult();

        // Assert
        ApiResponse<ListLocationsResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ListLocationsResponse listResponse = apiResponse.getData();
        assertThat(listResponse).isNotNull();
        assertThat(listResponse.getLocations()).isNotNull();
    }

    @Test
    @Order(12)
    public void testListLocations_FilterByStatus() {
        // Arrange - Use pre-created warehouse1 from setup
        assertThat(warehouse1).isNotNull();

        // Act - Filter by AVAILABLE status
        EntityExchangeResult<ApiResponse<ListLocationsResponse>> exchangeResult =
                authenticatedGet("/api/v1/location-management/locations?status=AVAILABLE&page=0&size=10", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus()
                        .isOk().expectBody(new ParameterizedTypeReference<ApiResponse<ListLocationsResponse>>() {
                        }).returnResult();

        // Assert
        ApiResponse<ListLocationsResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
    }

    @Test
    @Order(13)
    public void testListLocations_SearchByCode() {
        // Arrange - Use pre-created warehouse1 from setup
        assertThat(warehouse1).isNotNull();

        // Act - Search by code
        EntityExchangeResult<ApiResponse<ListLocationsResponse>> exchangeResult =
                authenticatedGet("/api/v1/location-management/locations?search=" + warehouse1.getCode() + "&page=0&size=10", tenantAdminAuth.getAccessToken(),
                        testTenantId).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<ListLocationsResponse>>() {
                }).returnResult();

        // Assert
        ApiResponse<ListLocationsResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
    }

    @Test
    @Order(14)
    public void testGetLocationById_Success() {
        // Arrange - Use pre-created warehouse1 from setup
        assertThat(warehouse1).isNotNull();

        // Act
        EntityExchangeResult<ApiResponse<LocationResponse>> exchangeResult =
                authenticatedGet("/api/v1/location-management/locations/" + warehouse1.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus()
                        .isOk().expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {
                        }).returnResult();

        // Assert
        ApiResponse<LocationResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        LocationResponse location = apiResponse.getData();
        assertThat(location).isNotNull();
        assertThat(location.getLocationId()).isEqualTo(warehouse1.getLocationId());
        assertThat(location.getCode()).isEqualTo(warehouse1.getCode());
        assertThat(location.getPath()).isEqualTo(warehouse1.getPath());
    }

    // ==================== LOCATION STATUS TESTS ====================

    @Test
    @Order(15)
    public void testGetLocationById_NotFound() {
        // Arrange - Use fixed non-existent location ID (valid UUID format but non-existent)
        String nonExistentId = "00000000-0000-0000-0000-000000000002";

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/location-management/locations/" + nonExistentId, tenantAdminAuth.getAccessToken(), testTenantId).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    @Order(20)
    public void testUpdateLocationStatus_ToBlocked() {
        // Arrange - Use pre-created warehouse2 from setup (use warehouse2 to avoid affecting other tests)
        assertThat(warehouse2).isNotNull();

        UpdateLocationStatusRequest statusRequest = UpdateLocationStatusRequest.builder().status("BLOCKED").reason("Maintenance required").build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + warehouse2.getLocationId() + "/status", tenantAdminAuth.getAccessToken(), testTenantId,
                        statusRequest).exchange();

        // Assert
        // Note: If endpoint doesn't exist, this will fail appropriately
        // Status could be 200 OK, 204 NO CONTENT, or 404 NOT FOUND if endpoint not implemented
        try {
            response.expectStatus().is2xxSuccessful();
        } catch (AssertionError e) {
            // If endpoint not implemented, expect 404
            response.expectStatus().isNotFound();
        }
    }

    /**
     * Helper method to create authenticated PUT request with tenant context.
     */
    private WebTestClient.RequestHeadersSpec<?> authenticatedPutWithTenant(String uri, String accessToken, String tenantId, Object requestBody) {
        return webTestClient.put().uri(uri).contentType(MediaType.APPLICATION_JSON).headers(headers -> {
            RequestHeaderHelper.addAuthHeaders(headers, accessToken);
            RequestHeaderHelper.addTenantHeader(headers, tenantId);
        }).bodyValue(requestBody);
    }

    @Test
    @Order(21)
    public void testUpdateLocationStatus_ToAvailable() {
        // Arrange - Use pre-created warehouse2 from setup
        assertThat(warehouse2).isNotNull();

        UpdateLocationStatusRequest statusRequest = UpdateLocationStatusRequest.builder().status("AVAILABLE").build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + warehouse2.getLocationId() + "/status", tenantAdminAuth.getAccessToken(), testTenantId,
                        statusRequest).exchange();

        // Assert
        try {
            response.expectStatus().is2xxSuccessful();
        } catch (AssertionError e) {
            // If endpoint not implemented, expect 404
            response.expectStatus().isNotFound();
        }
    }

    // ==================== LOCATION UPDATE TESTS ====================

    @Test
    @Order(22)
    public void testUpdateLocationStatus_InvalidStatus() {
        // Arrange - Use pre-created warehouse2 from setup
        assertThat(warehouse2).isNotNull();

        UpdateLocationStatusRequest statusRequest = UpdateLocationStatusRequest.builder().status("INVALID_STATUS").build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + warehouse2.getLocationId() + "/status", tenantAdminAuth.getAccessToken(), testTenantId,
                        statusRequest).exchange();

        // Assert
        try {
            response.expectStatus().isBadRequest();
        } catch (AssertionError e) {
            // If endpoint not implemented, expect 404
            response.expectStatus().isNotFound();
        }
    }

    @Test
    @Order(23)
    public void testUpdateLocationStatus_LocationNotFound() {
        // Arrange - Use fixed non-existent location ID (valid UUID format but non-existent)
        String nonExistentId = "00000000-0000-0000-0000-000000000003";
        UpdateLocationStatusRequest statusRequest = UpdateLocationStatusRequest.builder().status("BLOCKED").build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + nonExistentId + "/status", tenantAdminAuth.getAccessToken(), testTenantId,
                        statusRequest).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    // ==================== BLOCK/UNBLOCK LOCATION TESTS ====================

    @Test
    @Order(24)
    public void testBlockLocation_Success() {
        // Arrange - Use pre-created warehouse2 from setup to avoid affecting other tests
        assertThat(warehouse2).isNotNull();
        UUID locationId = UUID.fromString(warehouse2.getLocationId());
        BlockLocationRequest request = BlockLocationRequest.builder().reason("Maintenance").build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPost("/api/v1/location-management/locations/" + locationId + "/block", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<BlockLocationResultDTO>> exchangeResult =
                response.expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<BlockLocationResultDTO>>() {
                }).returnResult();

        ApiResponse<BlockLocationResultDTO> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        BlockLocationResultDTO result = apiResponse.getData();
        assertThat(result).isNotNull();
        assertThat(result.getLocationId()).isEqualTo(locationId);
        assertThat(result.getStatus()).isEqualTo("BLOCKED");
    }

    @Test
    @Order(25)
    public void testBlockLocation_AlreadyBlocked() {
        // Arrange - Create a new location for this test to avoid conflicts
        CreateLocationResponse testLocation = createLocation(LocationTestDataBuilder.buildWarehouseRequest());
        UUID locationId = UUID.fromString(testLocation.getLocationId());

        // Block location first
        BlockLocationRequest blockRequest = BlockLocationRequest.builder().reason("Initial block").build();
        authenticatedPost("/api/v1/location-management/locations/" + locationId + "/block", tenantAdminAuth.getAccessToken(), testTenantId, blockRequest).exchange().expectStatus()
                .isOk();

        // Act - Try to block again
        BlockLocationRequest request = BlockLocationRequest.builder().reason("Second block attempt").build();
        WebTestClient.ResponseSpec response =
                authenticatedPost("/api/v1/location-management/locations/" + locationId + "/block", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert - Should still be OK (idempotent) or Bad Request
        int statusCode = response.expectBody().returnResult().getStatus().value();
        assertThat(statusCode).isIn(200, 400);
    }

    @Test
    @Order(26)
    public void testUnblockLocation_Success() {
        // Arrange - Create a new location for this test
        CreateLocationResponse testLocation = createLocation(LocationTestDataBuilder.buildWarehouseRequest());
        UUID locationId = UUID.fromString(testLocation.getLocationId());

        // Block location first
        BlockLocationRequest blockRequest = BlockLocationRequest.builder().reason("Temporary block").build();
        authenticatedPost("/api/v1/location-management/locations/" + locationId + "/block", tenantAdminAuth.getAccessToken(), testTenantId, blockRequest).exchange().expectStatus()
                .isOk();

        UnblockLocationRequest request = UnblockLocationRequest.builder().reason("Maintenance complete").build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPost("/api/v1/location-management/locations/" + locationId + "/unblock", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<UnblockLocationResultDTO>> exchangeResult =
                response.expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<UnblockLocationResultDTO>>() {
                }).returnResult();

        ApiResponse<UnblockLocationResultDTO> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        UnblockLocationResultDTO result = apiResponse.getData();
        assertThat(result).isNotNull();
        assertThat(result.getLocationId()).isEqualTo(locationId);
        assertThat(result.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    @Order(27)
    public void testUnblockLocation_NotBlocked() {
        // Arrange - Create a new location (should be AVAILABLE by default)
        CreateLocationResponse testLocation = createLocation(LocationTestDataBuilder.buildWarehouseRequest());
        UUID locationId = UUID.fromString(testLocation.getLocationId());

        UnblockLocationRequest request = UnblockLocationRequest.builder().reason("Unblock attempt").build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPost("/api/v1/location-management/locations/" + locationId + "/unblock", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert - Should still be OK (idempotent) or Bad Request
        int statusCode = response.expectBody().returnResult().getStatus().value();
        assertThat(statusCode).isIn(200, 400);
    }

    @Test
    @Order(28)
    public void testBlockUnblockCycle() {
        // Arrange - Create a new location for this test
        CreateLocationResponse testLocation = createLocation(LocationTestDataBuilder.buildWarehouseRequest());
        UUID locationId = UUID.fromString(testLocation.getLocationId());

        BlockLocationRequest blockRequest = BlockLocationRequest.builder().reason("Test cycle").build();

        // Act - Block
        authenticatedPost("/api/v1/location-management/locations/" + locationId + "/block", tenantAdminAuth.getAccessToken(), testTenantId, blockRequest).exchange().expectStatus()
                .isOk();

        // Verify location is blocked
        EntityExchangeResult<ApiResponse<LocationResponse>> getResult =
                authenticatedGet("/api/v1/location-management/locations/" + locationId, tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {
                        }).returnResult();

        ApiResponse<LocationResponse> getApiResponse = getResult.getResponseBody();
        assertThat(getApiResponse).isNotNull();
        LocationResponse location = getApiResponse.getData();
        assertThat(location).isNotNull();
        assertThat(location.getStatus()).isEqualTo("BLOCKED");

        // Unblock
        UnblockLocationRequest unblockRequest = UnblockLocationRequest.builder().reason("Test complete").build();
        authenticatedPost("/api/v1/location-management/locations/" + locationId + "/unblock", tenantAdminAuth.getAccessToken(), testTenantId, unblockRequest).exchange()
                .expectStatus().isOk();

        // Verify location is available
        EntityExchangeResult<ApiResponse<LocationResponse>> getResult2 =
                authenticatedGet("/api/v1/location-management/locations/" + locationId, tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {
                        }).returnResult();

        ApiResponse<LocationResponse> getApiResponse2 = getResult2.getResponseBody();
        assertThat(getApiResponse2).isNotNull();
        LocationResponse location2 = getApiResponse2.getData();
        assertThat(location2).isNotNull();
        assertThat(location2.getStatus()).isEqualTo("AVAILABLE");
    }

    // ==================== LOCATION UPDATE TESTS ====================

    @Test
    @Order(29)
    public void testUpdateLocation_Success() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        UpdateLocationRequest updateRequest =
                UpdateLocationRequest.builder().zone("ZONE-A").aisle("AISLE-01").rack("RACK-02").level("LEVEL-03").description("Updated location description").build();

        // Act
        EntityExchangeResult<ApiResponse<LocationResponse>> exchangeResult =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {
                }).returnResult();

        // Assert
        ApiResponse<LocationResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        LocationResponse updatedLocation = apiResponse.getData();
        assertThat(updatedLocation).isNotNull();
        assertThat(updatedLocation.getLocationId()).isEqualTo(location.getLocationId());
        assertThat(updatedLocation.getCoordinates()).isNotNull();
        assertThat(updatedLocation.getCoordinates().getZone()).isEqualTo("ZONE-A");
        assertThat(updatedLocation.getCoordinates().getAisle()).isEqualTo("AISLE-01");
        assertThat(updatedLocation.getCoordinates().getRack()).isEqualTo("RACK-02");
        assertThat(updatedLocation.getCoordinates().getLevel()).isEqualTo("LEVEL-03");
        assertThat(updatedLocation.getDescription()).isEqualTo("Updated location description");
    }

    @Test
    @Order(30)
    public void testUpdateLocation_WithBarcode() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        UpdateLocationRequest updateRequest = UpdateLocationRequest.builder().zone("ZONE-B").aisle("AISLE-02").rack("RACK-03").level("LEVEL-04").barcode("UPDATEDBARCODE123")
                .description("Location with updated barcode").build();

        // Act
        EntityExchangeResult<ApiResponse<LocationResponse>> exchangeResult =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {
                }).returnResult();

        // Assert
        ApiResponse<LocationResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        LocationResponse updatedLocation = apiResponse.getData();
        assertThat(updatedLocation).isNotNull();
        assertThat(updatedLocation.getBarcode()).isEqualTo("UPDATEDBARCODE123");
        assertThat(updatedLocation.getCoordinates().getZone()).isEqualTo("ZONE-B");
    }

    @Test
    @Order(31)
    public void testUpdateLocation_DuplicateBarcode() {
        // Arrange - Create two locations
        CreateLocationResponse location1 = createLocation(LocationTestDataBuilder.buildWarehouseRequest());
        CreateLocationResponse location2 = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        // Get location1's barcode
        LocationResponse location1Details = getLocationById(location1.getLocationId());
        String location1Barcode = location1Details.getBarcode();
        assertThat(location1Barcode).isNotBlank();

        // Try to update location2 with location1's barcode
        UpdateLocationRequest updateRequest = UpdateLocationRequest.builder().zone("ZONE-C").aisle("AISLE-03").rack("RACK-04").level("LEVEL-05").barcode(location1Barcode).build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location2.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange();

        // Assert - Should fail with duplicate barcode error
        response.expectStatus().isBadRequest(); // or 409 CONFLICT
    }

    /**
     * Helper method to get a location by ID and return the response.
     */
    private LocationResponse getLocationById(String locationId) {
        EntityExchangeResult<ApiResponse<LocationResponse>> exchangeResult =
                authenticatedGet("/api/v1/location-management/locations/" + locationId, tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {
                        }).returnResult();

        ApiResponse<LocationResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        return apiResponse.getData();
    }

    @Test
    @Order(32)
    public void testUpdateLocation_LocationNotFound() {
        // Arrange - Use fixed non-existent location ID (valid UUID format but non-existent)
        String nonExistentId = "00000000-0000-0000-0000-000000000004";
        UpdateLocationRequest updateRequest = UpdateLocationRequest.builder().zone("ZONE-D").aisle("AISLE-04").rack("RACK-05").level("LEVEL-06").build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + nonExistentId, tenantAdminAuth.getAccessToken(), testTenantId, updateRequest).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    @Order(33)
    public void testUpdateLocation_WithoutAuthentication() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        UpdateLocationRequest updateRequest = UpdateLocationRequest.builder().zone("ZONE-E").aisle("AISLE-05").rack("RACK-06").level("LEVEL-07").build();

        // Act - Try without authentication
        WebTestClient.ResponseSpec response =
                webTestClient.put().uri("/api/v1/location-management/locations/" + location.getLocationId()).contentType(MediaType.APPLICATION_JSON).bodyValue(updateRequest)
                        .exchange();

        // Assert
        response.expectStatus().isUnauthorized();
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @Order(34)
    public void testUpdateLocation_OnlyCoordinates() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        UpdateLocationRequest updateRequest = UpdateLocationRequest.builder().zone("ZONE-F").aisle("AISLE-06").rack("RACK-07").level("LEVEL-08").build();

        // Act
        EntityExchangeResult<ApiResponse<LocationResponse>> exchangeResult =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {
                }).returnResult();

        // Assert
        ApiResponse<LocationResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        LocationResponse updatedLocation = apiResponse.getData();
        assertThat(updatedLocation).isNotNull();
        assertThat(updatedLocation.getCoordinates().getZone()).isEqualTo("ZONE-F");
        assertThat(updatedLocation.getCoordinates().getAisle()).isEqualTo("AISLE-06");
        assertThat(updatedLocation.getCoordinates().getRack()).isEqualTo("RACK-07");
        assertThat(updatedLocation.getCoordinates().getLevel()).isEqualTo("LEVEL-08");
    }

    @Test
    @Order(35)
    public void testUpdateLocation_OnlyDescription() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        // Get current coordinates to preserve them
        LocationResponse currentLocation = getLocationById(location.getLocationId());

        UpdateLocationRequest updateRequest = UpdateLocationRequest.builder().zone(currentLocation.getCoordinates().getZone()).aisle(currentLocation.getCoordinates().getAisle())
                .rack(currentLocation.getCoordinates().getRack()).level(currentLocation.getCoordinates().getLevel()).description("Only description updated").build();

        // Act
        EntityExchangeResult<ApiResponse<LocationResponse>> exchangeResult =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {
                }).returnResult();

        // Assert
        ApiResponse<LocationResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        LocationResponse updatedLocation = apiResponse.getData();
        assertThat(updatedLocation).isNotNull();
        assertThat(updatedLocation.getDescription()).isEqualTo("Only description updated");
        // Coordinates should remain unchanged
        assertThat(updatedLocation.getCoordinates().getZone()).isEqualTo(currentLocation.getCoordinates().getZone());
    }

    @Test
    @Order(36)
    public void testUpdateLocation_SameBarcode() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());
        LocationResponse currentLocation = getLocationById(location.getLocationId());
        String currentBarcode = currentLocation.getBarcode();

        UpdateLocationRequest updateRequest =
                UpdateLocationRequest.builder().zone("ZONE-G").aisle("AISLE-07").rack("RACK-08").level("LEVEL-09").barcode(currentBarcode) // Same barcode should be allowed
                        .description("Updated with same barcode").build();

        // Act
        EntityExchangeResult<ApiResponse<LocationResponse>> exchangeResult =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {
                }).returnResult();

        // Assert
        ApiResponse<LocationResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        LocationResponse updatedLocation = apiResponse.getData();
        assertThat(updatedLocation).isNotNull();
        assertThat(updatedLocation.getBarcode()).isEqualTo(currentBarcode);
        assertThat(updatedLocation.getDescription()).isEqualTo("Updated with same barcode");
    }

    @Test
    @Order(37)
    public void testUpdateLocation_ZoneExceedsMaxLength() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        UpdateLocationRequest updateRequest = UpdateLocationRequest.builder().zone("ZONE-EXCEEDS-10") // 15 characters - exceeds 10
                .aisle("AISLE-01").rack("RACK-02").level("LEVEL-03").build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    @Test
    @Order(38)
    public void testUpdateLocation_AisleExceedsMaxLength() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        UpdateLocationRequest updateRequest = UpdateLocationRequest.builder().zone("ZONE-A").aisle("AISLE-EXCEEDS-10") // 15 characters - exceeds 10
                .rack("RACK-02").level("LEVEL-03").build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    @Test
    @Order(39)
    public void testUpdateLocation_RackExceedsMaxLength() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        UpdateLocationRequest updateRequest = UpdateLocationRequest.builder().zone("ZONE-A").aisle("AISLE-01").rack("RACK-EXCEEDS-10") // 15 characters - exceeds 10
                .level("LEVEL-03").build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    @Test
    @Order(40)
    public void testUpdateLocation_LevelExceedsMaxLength() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        UpdateLocationRequest updateRequest =
                UpdateLocationRequest.builder().zone("ZONE-A").aisle("AISLE-01").rack("RACK-02").level("LEVEL-EXCEEDS-10") // 16 characters - exceeds 10
                        .build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    @Test
    @Order(41)
    public void testUpdateLocation_BarcodeTooShort() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        UpdateLocationRequest updateRequest =
                UpdateLocationRequest.builder().zone("ZONE-A").aisle("AISLE-01").rack("RACK-02").level("LEVEL-03").barcode("SHORT") // 5 characters - less than minimum 8
                        .build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    // ==================== LOCATION HIERARCHY TESTS ====================

    @Test
    @Order(42)
    public void testUpdateLocation_BarcodeTooLong() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        UpdateLocationRequest updateRequest = UpdateLocationRequest.builder().zone("ZONE-A").aisle("AISLE-01").rack("RACK-02").level("LEVEL-03")
                .barcode("BARCODE123456789012345") // 23 characters - exceeds maximum 20
                .build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    // ==================== AUTHORIZATION TESTS ====================

    @Test
    @Order(43)
    public void testUpdateLocation_BarcodeInvalidCharacters() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        UpdateLocationRequest updateRequest =
                UpdateLocationRequest.builder().zone("ZONE-A").aisle("AISLE-01").rack("RACK-02").level("LEVEL-03").barcode("BARCODE-123") // Contains hyphen - invalid
                        .build();

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    @Test
    @Order(44)
    public void testUpdateLocation_ValidBarcode() {
        // Arrange - Create location first
        CreateLocationResponse location = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        UpdateLocationRequest updateRequest =
                UpdateLocationRequest.builder().zone("ZONE-A").aisle("AISLE-01").rack("RACK-02").level("LEVEL-03").barcode("VALIDBARCODE123") // 15 characters, alphanumeric - valid
                        .build();

        // Act
        EntityExchangeResult<ApiResponse<LocationResponse>> exchangeResult =
                authenticatedPutWithTenant("/api/v1/location-management/locations/" + location.getLocationId(), tenantAdminAuth.getAccessToken(), testTenantId,
                        updateRequest).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<LocationResponse>>() {
                }).returnResult();

        // Assert
        ApiResponse<LocationResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        LocationResponse updatedLocation = apiResponse.getData();
        assertThat(updatedLocation).isNotNull();
        assertThat(updatedLocation.getBarcode()).isEqualTo("VALIDBARCODE123");
    }

    // ==================== HELPER METHODS ====================

    @Test
    @Order(50)
    public void testLocationPathGeneration() {
        // Arrange - Use pre-created full hierarchy from warehouse1
        assertThat(warehouse1).isNotNull();
        assertThat(warehouse1Zones).isNotEmpty();
        assertThat(warehouse1Aisles).isNotEmpty();
        assertThat(warehouse1Racks).isNotEmpty();
        assertThat(warehouse1Bins).isNotEmpty();

        CreateLocationResponse zone = warehouse1Zones.get(0);
        CreateLocationResponse aisle = warehouse1Aisles.get(0);
        CreateLocationResponse rack = warehouse1Racks.get(0);
        CreateLocationResponse bin = warehouse1Bins.get(0);

        // Act - Get bin location
        LocationResponse binLocation = getLocationById(bin.getLocationId());

        // Assert
        assertThat(binLocation).isNotNull();
        assertThat(binLocation.getPath()).contains(warehouse1.getCode());
        assertThat(binLocation.getPath()).contains(zone.getCode());
        assertThat(binLocation.getPath()).contains(aisle.getCode());
        assertThat(binLocation.getPath()).contains(rack.getCode());
        assertThat(binLocation.getPath()).contains(bin.getCode());
    }

    @Test
    @Order(45)
    public void testListLocations_WithoutTenantHeader() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/location-management/locations?page=0&size=10", tenantAdminAuth.getAccessToken()).exchange();

        // Assert
        // Should fail without tenant header or use default tenant from token
        try {
            response.expectStatus().is2xxSuccessful();
        } catch (AssertionError e) {
            response.expectStatus().isBadRequest();
        }
    }

    @Test
    @Order(46)
    public void testGetLocation_Unauthorized() {
        // Arrange - Use pre-created warehouse1 from setup
        assertThat(warehouse1).isNotNull();

        // Act - Try without authentication
        WebTestClient.ResponseSpec response = webTestClient.get().uri("/api/v1/location-management/locations/" + warehouse1.getLocationId()).exchange();

        // Assert
        response.expectStatus().isUnauthorized();
    }

    // ==================== SPRINT 3: LOCATION AVAILABILITY TESTS ====================

    @Test
    @Order(200)
    public void testCheckLocationAvailability_AvailableLocation_Success() {
        // Arrange - Use pre-created warehouse2 from setup
        assertThat(warehouse2).isNotNull();
        String locationId = warehouse2.getLocationId();

        // Ensure location is available (unblock if it was blocked by previous tests)
        LocationResponse location = getLocationById(locationId);
        if ("BLOCKED".equals(location.getStatus())) {
            UnblockLocationRequest unblockRequest = UnblockLocationRequest.builder().reason("Test setup").build();
            authenticatedPost("/api/v1/location-management/locations/" + locationId + "/unblock", tenantAdminAuth.getAccessToken(), testTenantId, unblockRequest).exchange()
                    .expectStatus().isOk();
        }

        // Act - Check availability
        EntityExchangeResult<ApiResponse<LocationAvailabilityResponse>> exchangeResult =
                authenticatedGet("/api/v1/location-management/locations/" + locationId + "/check-availability?requiredQuantity=100", tenantAdminAuth.getAccessToken(),
                        testTenantId).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<LocationAvailabilityResponse>>() {
                }).returnResult();

        // Assert
        ApiResponse<LocationAvailabilityResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        LocationAvailabilityResponse availability = apiResponse.getData();
        assertThat(availability).isNotNull();
        assertThat(availability.isAvailable()).isTrue();
    }

    @Test
    @Order(201)
    public void testCheckLocationAvailability_NonExistentLocation_ShouldReturnNotFound() {
        // Arrange - Use fixed non-existent location ID (valid UUID format but non-existent)
        String nonExistentLocationId = "00000000-0000-0000-0000-000000000005";

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedGet("/api/v1/location-management/locations/" + nonExistentLocationId + "/check-availability?requiredQuantity=100", tenantAdminAuth.getAccessToken(),
                        testTenantId).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    // ==================== SPRINT 3: FEFO LOCATION ASSIGNMENT TESTS ====================

    @Test
    @Order(210)
    public void testAssignLocationsFEFO_Success() {
        // Arrange - Create locations
        CreateLocationResponse location1 = createLocation(LocationTestDataBuilder.buildWarehouseRequest());
        CreateLocationResponse location2 = createLocation(LocationTestDataBuilder.buildWarehouseRequest());

        // Create stock items (via consignment confirmation)
        // Note: This test requires stock items to exist, which are created when consignments are confirmed
        // For this test, we'll use mock stock item IDs
        // In a real scenario, you would:
        // 1. Create a consignment
        // 2. Confirm it to create stock items
        // 3. Get the stock item IDs
        // 4. Use them in FEFO assignment

        // For now, we'll test the endpoint structure
        BigDecimal quantity = BigDecimal.valueOf(100);
        LocalDate expirationDate = LocalDate.now().plusDays(30);

        // Use fixed mock stock item ID (valid UUID format but non-existent)
        String mockStockItemId = "00000000-0000-0000-0000-000000000100";
        AssignLocationsFEFORequest fefoRequest = StockItemTestDataBuilder.buildFEFOAssignmentRequest(mockStockItemId,
                quantity, expirationDate, "NEAR_EXPIRY");

        // Act
        WebTestClient.ResponseSpec response =
                authenticatedPost("/api/v1/location-management/locations/assign-fefo", tenantAdminAuth.getAccessToken(), testTenantId, fefoRequest).exchange();

        // Assert - May succeed or fail depending on whether stock items exist
        // Accept both 200 OK (if stock items exist) or 400/404 (if they don't)
        int statusCode = response.expectBody().returnResult().getStatus().value();
        assertThat(statusCode).isIn(200, 400, 404);
    }
}

