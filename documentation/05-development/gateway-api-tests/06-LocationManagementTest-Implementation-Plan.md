# LocationManagementTest Implementation Plan

## Overview

`LocationManagementTest` validates location management functionality through the gateway service. Tests authenticate as TENANT_ADMIN and verify location CRUD operations, location
hierarchy, capacity management, status transitions, and tenant-scoped access control.

---

## Objectives

1. **Location Creation**: Test location creation with hierarchy (zone, aisle, rack, bin)
2. **Location Hierarchy**: Test parent-child relationships and path generation
3. **Location Capacity**: Test capacity validation and enforcement
4. **Location Status**: Test location status transitions (ACTIVE, INACTIVE, MAINTENANCE)
5. **Location Queries**: Test list locations with pagination, search, and filtering
6. **Location Updates**: Test location updates with hierarchy changes
7. **Location Deletion**: Test location deletion and cascade rules
8. **Tenant Isolation**: Verify TENANT_ADMIN can only manage locations in own tenant
9. **Authorization Checks**: Verify role-based access control
10. **Validation Rules**: Test location data validation (code, type, capacity)

---

## Test Scenarios

### 1. Location Creation Tests

#### Test: Create Warehouse Location (Root Level)

- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/location-management/locations`
- **Request Body**:
  ```json
  {
    "code": "WH-01",
    "name": "Main Warehouse",
    "type": "WAREHOUSE",
    "parentLocationId": null,
    "capacity": 10000,
    "dimensions": {
      "length": 100.0,
      "width": 80.0,
      "height": 10.0,
      "unit": "M"
    }
  }
  ```
- **Assertions**:
    - Status: 201 CREATED
    - Response contains `locationId`, `code`, `name`, `type`, `path`
    - Location path: `/WH-01`
    - LocationCreatedEvent published
    - Location created in correct tenant schema

#### Test: Create Zone Location (Second Level)

- **Setup**: Login as TENANT_ADMIN, create warehouse "WH-01"
- **Action**: POST `/api/v1/location-management/locations`
- **Request Body**:
  ```json
  {
    "code": "ZONE-A",
    "name": "Zone A",
    "type": "ZONE",
    "parentLocationId": "{warehouseId}",
    "capacity": 2000
  }
  ```
- **Assertions**:
    - Status: 201 CREATED
    - Location path: `/WH-01/ZONE-A`
    - Parent relationship established

#### Test: Create Aisle Location (Third Level)

- **Setup**: Login as TENANT_ADMIN, create warehouse and zone
- **Action**: POST `/api/v1/location-management/locations`
- **Request Body**:
  ```json
  {
    "code": "AISLE-01",
    "name": "Aisle 01",
    "type": "AISLE",
    "parentLocationId": "{zoneId}",
    "capacity": 500
  }
  ```
- **Assertions**:
    - Status: 201 CREATED
    - Location path: `/WH-01/ZONE-A/AISLE-01`

#### Test: Create Rack Location (Fourth Level)

- **Setup**: Login as TENANT_ADMIN, create warehouse, zone, aisle
- **Action**: POST `/api/v1/location-management/locations`
- **Request Body**:
  ```json
  {
    "code": "RACK-A1",
    "name": "Rack A1",
    "type": "RACK",
    "parentLocationId": "{aisleId}",
    "capacity": 100
  }
  ```
- **Assertions**:
    - Status: 201 CREATED
    - Location path: `/WH-01/ZONE-A/AISLE-01/RACK-A1`

#### Test: Create Bin Location (Fifth Level)

- **Setup**: Login as TENANT_ADMIN, create full hierarchy
- **Action**: POST `/api/v1/location-management/locations`
- **Request Body**:
  ```json
  {
    "code": "BIN-01",
    "name": "Bin 01",
    "type": "BIN",
    "parentLocationId": "{rackId}",
    "capacity": 10
  }
  ```
- **Assertions**:
    - Status: 201 CREATED
    - Location path: `/WH-01/ZONE-A/AISLE-01/RACK-A1/BIN-01`

#### Test: Create Location with Duplicate Code

- **Setup**: Login as TENANT_ADMIN, create location with code "WH-01"
- **Action**: POST `/api/v1/location-management/locations` with same code
- **Assertions**:
    - Status: 400 BAD REQUEST or 409 CONFLICT
    - Error message indicates duplicate location code

#### Test: Create Location with Invalid Parent

- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/location-management/locations` with non-existent parentLocationId
- **Assertions**:
    - Status: 404 NOT FOUND or 400 BAD REQUEST
    - Error message indicates parent location not found

#### Test: Create Location with Negative Capacity

- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/location-management/locations` with capacity `-10`
- **Assertions**:
    - Status: 400 BAD REQUEST
    - Validation error for negative capacity

#### Test: Create Location with Missing Required Fields

- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/location-management/locations` with missing `code` or `type`
- **Assertions**:
    - Status: 400 BAD REQUEST
    - Validation errors for missing fields

#### Test: Create Location Without Authentication

- **Setup**: No authentication
- **Action**: POST `/api/v1/location-management/locations` without Bearer token
- **Assertions**:
    - Status: 401 UNAUTHORIZED

---

### 2. Location Hierarchy Tests

#### Test: Verify Location Path Generation

- **Setup**: Login as TENANT_ADMIN, create nested locations
- **Action**: GET `/api/v1/location-management/locations/{binId}`
- **Assertions**:
    - Location path: `/WH-01/ZONE-A/AISLE-01/RACK-A1/BIN-01`
    - Path automatically generated from hierarchy

#### Test: List Child Locations

- **Setup**: Login as TENANT_ADMIN, create warehouse with 3 zones
- **Action**: GET `/api/v1/location-management/locations/{warehouseId}/children`
- **Assertions**:
    - Status: 200 OK
    - Response contains 3 child zones

#### Test: Get Location Ancestors

- **Setup**: Login as TENANT_ADMIN, create full hierarchy
- **Action**: GET `/api/v1/location-management/locations/{binId}/ancestors`
- **Assertions**:
    - Status: 200 OK
    - Response contains ancestors: [Warehouse, Zone, Aisle, Rack]

#### Test: Invalid Hierarchy (Bin as Parent of Rack)

- **Setup**: Login as TENANT_ADMIN, create bin location
- **Action**: Create rack with bin as parent
- **Assertions**:
    - Status: 400 BAD REQUEST
    - Error message indicates invalid hierarchy

---

### 3. Location Capacity Tests

#### Test: Verify Location Capacity Enforcement

- **Setup**: Login as TENANT_ADMIN, create bin with capacity 10
- **Action**: Attempt to assign 15 units to location
- **Assertions**:
    - Status: 400 BAD REQUEST (if capacity check enforced)
    - Error message indicates capacity exceeded

#### Test: Update Location Capacity

- **Setup**: Login as TENANT_ADMIN, create location with capacity 100
- **Action**: PUT `/api/v1/location-management/locations/{locationId}`
- **Request Body**:
  ```json
  {
    "capacity": 200
  }
  ```
- **Assertions**:
    - Status: 200 OK
    - Capacity updated to 200

---

### 4. Location Status Tests

#### Test: Activate Location

- **Setup**: Login as TENANT_ADMIN, create location (default status ACTIVE)
- **Action**: PUT `/api/v1/location-management/locations/{locationId}/status`
- **Request Body**:
  ```json
  {
    "status": "ACTIVE"
  }
  ```
- **Assertions**:
    - Status: 200 OK
    - Location status set to ACTIVE

#### Test: Deactivate Location

- **Setup**: Login as TENANT_ADMIN, create location
- **Action**: PUT `/api/v1/location-management/locations/{locationId}/status`
- **Request Body**:
  ```json
  {
    "status": "INACTIVE"
  }
  ```
- **Assertions**:
    - Status: 200 OK
    - Location status set to INACTIVE
    - Location cannot accept new stock

#### Test: Set Location to Maintenance

- **Setup**: Login as TENANT_ADMIN, create location
- **Action**: PUT `/api/v1/location-management/locations/{locationId}/status`
- **Request Body**:
  ```json
  {
    "status": "MAINTENANCE"
  }
  ```
- **Assertions**:
    - Status: 200 OK
    - Location status set to MAINTENANCE
    - Location flagged for maintenance operations

---

### 5. Location Query Tests

#### Test: List All Locations with Pagination

- **Setup**: Login as TENANT_ADMIN, create 15 locations
- **Action**: GET `/api/v1/location-management/locations?page=0&size=10`
- **Assertions**:
    - Status: 200 OK
    - Response contains:
        - `locations: [...]` (10 items)
        - Pagination metadata (page, size, totalElements, totalPages)

#### Test: List Locations with Type Filter

- **Setup**: Login as TENANT_ADMIN, create locations of different types
- **Action**: GET `/api/v1/location-management/locations?type=BIN`
- **Assertions**:
    - Status: 200 OK
    - Response contains only BIN type locations

#### Test: List Locations with Status Filter

- **Setup**: Login as TENANT_ADMIN, create 3 ACTIVE and 2 INACTIVE locations
- **Action**: GET `/api/v1/location-management/locations?status=ACTIVE`
- **Assertions**:
    - Status: 200 OK
    - Response contains only ACTIVE locations

#### Test: Search Locations by Code

- **Setup**: Login as TENANT_ADMIN, create locations
- **Action**: GET `/api/v1/location-management/locations?search=ZONE-A`
- **Assertions**:
    - Status: 200 OK
    - Response contains locations matching "ZONE-A"

#### Test: Get Location by ID

- **Setup**: Login as TENANT_ADMIN, create location
- **Action**: GET `/api/v1/location-management/locations/{locationId}`
- **Assertions**:
    - Status: 200 OK
    - Response contains location details (id, code, name, type, path, capacity)

#### Test: Get Non-Existent Location

- **Setup**: Login as TENANT_ADMIN
- **Action**: GET `/api/v1/location-management/locations/{randomUUID}`
- **Assertions**:
    - Status: 404 NOT FOUND

---

### 6. Location Update Tests

#### Test: Update Location Name and Description

- **Setup**: Login as TENANT_ADMIN, create location
- **Action**: PUT `/api/v1/location-management/locations/{locationId}`
- **Request Body**:
  ```json
  {
    "name": "Updated Warehouse Name",
    "description": "Updated description"
  }
  ```
- **Assertions**:
    - Status: 200 OK
    - Location name and description updated

#### Test: Update Location Parent (Move Location)

- **Setup**: Login as TENANT_ADMIN, create warehouse with 2 zones
- **Action**: PUT `/api/v1/location-management/locations/{aisleId}`
- **Request Body**:
  ```json
  {
    "parentLocationId": "{newZoneId}"
  }
  ```
- **Assertions**:
    - Status: 200 OK
    - Location moved to new parent
    - Location path updated: `/WH-01/ZONE-B/AISLE-01`

#### Test: Update Location with Invalid Parent

- **Setup**: Login as TENANT_ADMIN, create location
- **Action**: Update location with non-existent parentLocationId
- **Assertions**:
    - Status: 404 NOT FOUND or 400 BAD REQUEST

---

### 7. Location Deletion Tests

#### Test: Delete Empty Location

- **Setup**: Login as TENANT_ADMIN, create location with no stock
- **Action**: DELETE `/api/v1/location-management/locations/{locationId}`
- **Assertions**:
    - Status: 200 OK or 204 NO CONTENT
    - Location marked as deleted (soft delete) or removed

#### Test: Delete Location with Stock (Prevented)

- **Setup**: Login as TENANT_ADMIN, create location, assign stock
- **Action**: DELETE `/api/v1/location-management/locations/{locationId}`
- **Assertions**:
    - Status: 400 BAD REQUEST or 409 CONFLICT
    - Error message indicates location has stock

#### Test: Delete Location with Children (Cascade or Prevent)

- **Setup**: Login as TENANT_ADMIN, create warehouse with child zones
- **Action**: DELETE `/api/v1/location-management/locations/{warehouseId}`
- **Assertions**:
    - Status: 400 BAD REQUEST (if cascade not allowed)
    - Or 200 OK with cascade delete of all children

#### Test: Delete Non-Existent Location

- **Setup**: Login as TENANT_ADMIN
- **Action**: DELETE `/api/v1/location-management/locations/{randomUUID}`
- **Assertions**:
    - Status: 404 NOT FOUND

---

### 8. Tenant Isolation Tests

#### Test: TENANT_ADMIN Lists Only Own Tenant Locations

- **Setup**:
    - Login as SYSTEM_ADMIN, create Tenant A and Tenant B
    - Create 3 locations in Tenant A
    - Create 2 locations in Tenant B
    - Login as TENANT_ADMIN (Tenant A)
- **Action**: GET `/api/v1/location-management/locations`
- **Assertions**:
    - Status: 200 OK
    - Response contains only Tenant A locations (3 locations)
    - Tenant B locations not visible

#### Test: TENANT_ADMIN Cannot Access Location from Different Tenant

- **Setup**:
    - Login as SYSTEM_ADMIN, create Tenant A and Tenant B
    - Create location in Tenant B
    - Login as TENANT_ADMIN (Tenant A)
- **Action**: GET `/api/v1/location-management/locations/{tenantBLocationId}`
- **Assertions**:
    - Status: 403 FORBIDDEN or 404 NOT FOUND

---

### 9. Authorization Tests

#### Test: WAREHOUSE_MANAGER Can Manage Locations

- **Setup**: Create user with WAREHOUSE_MANAGER role, login
- **Action**: POST `/api/v1/location-management/locations` with valid data
- **Assertions**:
    - Status: 201 CREATED
    - WAREHOUSE_MANAGER has full access

#### Test: LOCATION_MANAGER Can Manage Locations

- **Setup**: Create user with LOCATION_MANAGER role, login
- **Action**: POST `/api/v1/location-management/locations` with valid data
- **Assertions**:
    - Status: 201 CREATED

#### Test: STOCK_MANAGER Can Read Locations

- **Setup**: Create user with STOCK_MANAGER role, login
- **Action**: GET `/api/v1/location-management/locations`
- **Assertions**:
    - Status: 200 OK (read-only access)

#### Test: STOCK_MANAGER Cannot Create Locations

- **Setup**: Login as STOCK_MANAGER
- **Action**: POST `/api/v1/location-management/locations` with valid data
- **Assertions**:
    - Status: 403 FORBIDDEN

#### Test: VIEWER Can Read Locations

- **Setup**: Create user with VIEWER role, login
- **Action**: GET `/api/v1/location-management/locations`
- **Assertions**:
    - Status: 200 OK

#### Test: VIEWER Cannot Modify Locations

- **Setup**: Login as VIEWER
- **Action**: POST `/api/v1/location-management/locations` with valid data
- **Assertions**:
    - Status: 403 FORBIDDEN

---

### 10. Edge Case Tests

#### Test: Create Location with Very Long Code

- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/location-management/locations` with 500-character code
- **Assertions**:
    - Status: 400 BAD REQUEST
    - Validation error for code length

#### Test: Create Deep Nested Hierarchy (7+ Levels)

- **Setup**: Login as TENANT_ADMIN
- **Action**: Create nested locations beyond 5 levels
- **Assertions**:
    - Status: 400 BAD REQUEST (if depth limit enforced)
    - Or 201 CREATED (if no depth limit)

#### Test: Concurrent Location Creation

- **Setup**: Login as TENANT_ADMIN
- **Action**: Send 5 concurrent POST requests to create locations
- **Assertions**:
    - All requests succeed (201 CREATED)
    - Each location has unique ID and code

---

## Test Data Strategy

### Faker Data Generation

```java
private CreateLocationRequest createRandomLocationRequest(String type, String parentLocationId) {
    return CreateLocationRequest.builder()
            .code(generateLocationCode(type))
            .name(faker.address().streetName())
            .type(type)
            .parentLocationId(parentLocationId)
            .capacity(faker.number().numberBetween(10, 1000))
            .build();
}

private String generateLocationCode(String type) {
    switch (type) {
        case "WAREHOUSE":
            return "WH-" + faker.number().digits(2);
        case "ZONE":
            return "ZONE-" + faker.bothify("?");
        case "AISLE":
            return "AISLE-" + faker.number().digits(2);
        case "RACK":
            return "RACK-" + faker.bothify("?#");
        case "BIN":
            return "BIN-" + faker.number().digits(2);
        default:
            return "LOC-" + faker.number().digits(3);
    }
}
```

---

## Test Class Structure

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import org.junit.jupiter.api.*;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LocationManagementTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        tenantAdminAuth = loginAsTenantAdmin();
        testTenantId = tenantAdminAuth.getTenantId();
    }

    // ==================== LOCATION CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreateWarehouse_Success() {
        // Arrange
        CreateLocationRequest request = LocationTestDataBuilder.buildWarehouseRequest(faker);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/location-management/locations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isCreated();

        CreateLocationResponse location = response.expectBody(CreateLocationResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(location).isNotNull();
        assertThat(location.getLocationId()).isNotBlank();
        assertThat(location.getCode()).isEqualTo(request.getCode());
        assertThat(location.getPath()).isEqualTo("/" + request.getCode());
    }

    // ... Additional test methods
}
```

---

## Test Fixtures

### LocationTestDataBuilder

Create `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/fixture/LocationTestDataBuilder.java`:

```java
package com.ccbsa.wms.gateway.api.fixture;

import net.datafaker.Faker;

public class LocationTestDataBuilder {

    public static CreateLocationRequest buildWarehouseRequest(Faker faker) {
        return CreateLocationRequest.builder()
                .code("WH-" + faker.number().digits(2))
                .name(faker.company().name() + " Warehouse")
                .type("WAREHOUSE")
                .parentLocationId(null)
                .capacity(10000)
                .build();
    }

    public static CreateLocationRequest buildZoneRequest(String parentLocationId, Faker faker) {
        return CreateLocationRequest.builder()
                .code("ZONE-" + faker.bothify("?"))
                .name("Zone " + faker.bothify("?"))
                .type("ZONE")
                .parentLocationId(parentLocationId)
                .capacity(2000)
                .build();
    }

    // Similar methods for AISLE, RACK, BIN
}
```

---

## DTOs Required

### CreateLocationRequest

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLocationRequest {
    private String code;
    private String name;
    private String description;
    private String type;
    private String parentLocationId;
    private Integer capacity;
    private LocationDimensions dimensions;
}
```

### CreateLocationResponse

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLocationResponse {
    private String locationId;
    private String code;
    private String name;
    private String type;
    private String path;
}
```

---

## Environment Variables

```bash
TEST_TENANT_ADMIN_USERNAME=<user-input>
TEST_TENANT_ADMIN_PASSWORD=Password123@
```

---

## Testing Checklist

- [ ] Warehouse location created successfully
- [ ] Zone location created with parent
- [ ] Aisle location created with parent
- [ ] Rack location created with parent
- [ ] Bin location created with parent
- [ ] Location path generated correctly from hierarchy
- [ ] Location creation fails with duplicate code
- [ ] Location creation fails with invalid parent
- [ ] List child locations works
- [ ] Get location ancestors works
- [ ] Update location capacity succeeds
- [ ] Activate/deactivate location succeeds
- [ ] Set location to maintenance succeeds
- [ ] List locations with pagination works
- [ ] Filter locations by type/status works
- [ ] Delete empty location succeeds
- [ ] Delete location with stock prevented
- [ ] Tenant isolation verified
- [ ] Authorization checks prevent unauthorized access
- [ ] LocationCreatedEvent published on creation

---

## Next Steps

1. **Implement LocationManagementTest** with all test scenarios
2. **Create LocationTestDataBuilder** for test data generation
3. **Create DTO classes** for location requests/responses
4. **Test hierarchy validation** (prevent invalid parent-child relationships)
5. **Validate event publishing** (LocationCreatedEvent, LocationUpdatedEvent)
6. **Document test results** and edge cases discovered

---

## Notes

- **Location Hierarchy**: Warehouse → Zone → Aisle → Rack → Bin
- **Location Path**: Auto-generated from hierarchy (e.g., `/WH-01/ZONE-A/AISLE-01`)
- **Capacity Enforcement**: May be enforced at stock assignment time
- **Rate Limiting**: Location service has 100 req/min limit
- **Tenant Isolation**: Locations scoped to tenant schema via X-Tenant-Id header
