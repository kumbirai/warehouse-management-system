# TenantManagementTest Implementation Plan

## Overview

`TenantManagementTest` validates all tenant management functionality through the gateway service. Tests authenticate as SYSTEM_ADMIN and verify tenant CRUD operations, lifecycle
management (activate, deactivate, suspend), and multi-tenant isolation.

---

## Objectives

1. **Tenant Creation**: Test tenant creation with Keycloak realm and schema provisioning
2. **Tenant Lifecycle Management**: Test activation, deactivation, and suspension workflows
3. **Tenant Configuration Updates**: Test tenant configuration changes
4. **Tenant Queries**: Test list tenants with pagination, filtering, and search
5. **Tenant Status Management**: Validate status transitions and business rules
6. **Authorization Checks**: Verify SYSTEM_ADMIN-only access for tenant operations
7. **Event Publishing**: Validate tenant events (TenantCreatedEvent, TenantActivatedEvent, etc.)
8. **Tenant Isolation**: Verify tenant data isolation and cross-tenant access prevention

---

## Test Scenarios

### 1. Tenant Creation Tests

#### Test: Create Tenant Successfully (SYSTEM_ADMIN)

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: POST `/api/v1/tenants` with valid tenant data
- **Assertions**:
    - Status: 201 CREATED
    - Response contains `tenantId`, `realmName`, `status=ACTIVE`
    - TenantCreatedEvent published
    - Keycloak realm created
    - Database schema provisioned
    - Tenant appears in list tenants query

#### Test: Create Tenant with Duplicate Name

- **Setup**: Login as SYSTEM_ADMIN, create tenant with name "Acme Corp"
- **Action**: POST `/api/v1/tenants` with same name "Acme Corp"
- **Assertions**:
    - Status: 400 BAD REQUEST or 409 CONFLICT
    - Error message indicates duplicate tenant name

#### Test: Create Tenant with Invalid Data

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: POST `/api/v1/tenants` with missing required fields
- **Assertions**:
    - Status: 400 BAD REQUEST
    - Validation errors for missing fields (name, email)

#### Test: Create Tenant Forbidden for Non-Admin

- **Setup**: Login as TENANT_ADMIN (if available) or create regular user
- **Action**: POST `/api/v1/tenants` with valid data
- **Assertions**:
    - Status: 403 FORBIDDEN
    - Error message indicates insufficient permissions

#### Test: Create Tenant Unauthorized Without Token

- **Setup**: No authentication
- **Action**: POST `/api/v1/tenants` without Bearer token
- **Assertions**:
    - Status: 401 UNAUTHORIZED

---

### 2. Tenant Activation Tests

#### Test: Activate Tenant Successfully

- **Setup**: Login as SYSTEM_ADMIN, create tenant (default status ACTIVE)
- **Action**:
    - Deactivate tenant first: PUT `/api/v1/tenants/{id}/deactivate`
    - Then activate: PUT `/api/v1/tenants/{id}/activate`
- **Assertions**:
    - Status: 200 OK
    - Tenant status changed to ACTIVE
    - TenantActivatedEvent published
    - Keycloak realm enabled

#### Test: Activate Already Active Tenant

- **Setup**: Login as SYSTEM_ADMIN, create tenant (status ACTIVE)
- **Action**: PUT `/api/v1/tenants/{id}/activate`
- **Assertions**:
    - Status: 200 OK or 400 BAD REQUEST
    - Error message if state transition invalid

#### Test: Activate Non-Existent Tenant

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: PUT `/api/v1/tenants/{randomUUID}/activate`
- **Assertions**:
    - Status: 404 NOT FOUND

---

### 3. Tenant Deactivation Tests

#### Test: Deactivate Tenant Successfully

- **Setup**: Login as SYSTEM_ADMIN, create tenant (status ACTIVE)
- **Action**: PUT `/api/v1/tenants/{id}/deactivate`
- **Assertions**:
    - Status: 200 OK
    - Tenant status changed to INACTIVE
    - TenantDeactivatedEvent published
    - Keycloak realm disabled (users cannot login)

#### Test: Deactivate Already Inactive Tenant

- **Setup**: Login as SYSTEM_ADMIN, create and deactivate tenant
- **Action**: PUT `/api/v1/tenants/{id}/deactivate`
- **Assertions**:
    - Status: 200 OK or 400 BAD REQUEST (idempotent or error)

#### Test: Deactivate Tenant with Active Users

- **Setup**:
    - Login as SYSTEM_ADMIN
    - Create tenant
    - Create active user in tenant
- **Action**: PUT `/api/v1/tenants/{id}/deactivate`
- **Assertions**:
    - Status: 200 OK (deactivation proceeds, users cannot login)
    - Or 400 BAD REQUEST if business rule prevents deactivation with active users

---

### 4. Tenant Suspension Tests

#### Test: Suspend Tenant Successfully

- **Setup**: Login as SYSTEM_ADMIN, create tenant
- **Action**: PUT `/api/v1/tenants/{id}/suspend`
- **Assertions**:
    - Status: 200 OK
    - Tenant status changed to SUSPENDED
    - TenantSuspendedEvent published
    - Users in suspended tenant cannot perform operations (403 FORBIDDEN)

#### Test: Suspend Already Suspended Tenant

- **Setup**: Login as SYSTEM_ADMIN, create and suspend tenant
- **Action**: PUT `/api/v1/tenants/{id}/suspend`
- **Assertions**:
    - Status: 200 OK or 400 BAD REQUEST

#### Test: Reactivate Suspended Tenant

- **Setup**: Login as SYSTEM_ADMIN, create and suspend tenant
- **Action**: PUT `/api/v1/tenants/{id}/activate`
- **Assertions**:
    - Status: 200 OK
    - Tenant status changed from SUSPENDED to ACTIVE
    - TenantActivatedEvent published

---

### 5. Tenant Status Lifecycle Tests

#### Test: Valid Status Transitions

- **Setup**: Login as SYSTEM_ADMIN, create tenant
- **Action**: Perform state transitions:
    1. ACTIVE → SUSPENDED
    2. SUSPENDED → ACTIVE
    3. ACTIVE → INACTIVE
    4. INACTIVE → ACTIVE
- **Assertions**:
    - All valid transitions succeed (200 OK)
    - Appropriate events published for each transition

#### Test: Invalid Status Transitions

- **Setup**: Login as SYSTEM_ADMIN, create tenant
- **Action**: Attempt invalid transitions (e.g., INACTIVE → SUSPENDED directly)
- **Assertions**:
    - Status: 400 BAD REQUEST
    - Error message indicates invalid state transition

---

### 6. Tenant Configuration Update Tests

#### Test: Update Tenant Configuration Successfully

- **Setup**: Login as SYSTEM_ADMIN, create tenant
- **Action**: PUT `/api/v1/tenants/{id}/configuration` with new configuration
- **Request Body**:
  ```json
  {
    "maxUsers": 100,
    "storageQuotaMB": 5120,
    "features": ["STOCK_MANAGEMENT", "PICKING", "RETURNS"],
    "settings": {
      "defaultLanguage": "en",
      "timezone": "UTC"
    }
  }
  ```
- **Assertions**:
    - Status: 200 OK
    - Configuration updated in database
    - TenantConfigurationUpdatedEvent published

#### Test: Update Configuration for Non-Existent Tenant

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: PUT `/api/v1/tenants/{randomUUID}/configuration`
- **Assertions**:
    - Status: 404 NOT FOUND

---

### 7. Tenant Query Tests

#### Test: List All Tenants (SYSTEM_ADMIN)

- **Setup**:
    - Login as SYSTEM_ADMIN
    - Create 5 tenants with Faker data
- **Action**: GET `/api/v1/tenants?page=0&size=10`
- **Assertions**:
    - Status: 200 OK
    - Response contains all 5 tenants (plus any existing)
    - Pagination metadata present (page, size, totalElements, totalPages)

#### Test: List Tenants with Status Filter

- **Setup**:
    - Login as SYSTEM_ADMIN
    - Create 3 ACTIVE tenants
    - Create 2 SUSPENDED tenants
- **Action**: GET `/api/v1/tenants?status=ACTIVE`
- **Assertions**:
    - Status: 200 OK
    - Response contains only ACTIVE tenants (3 results)

#### Test: List Tenants with Search Filter

- **Setup**:
    - Login as SYSTEM_ADMIN
    - Create tenant with name "Acme Corporation"
    - Create tenant with name "Beta Industries"
- **Action**: GET `/api/v1/tenants?search=Acme`
- **Assertions**:
    - Status: 200 OK
    - Response contains only "Acme Corporation"

#### Test: Get Tenant by ID

- **Setup**: Login as SYSTEM_ADMIN, create tenant
- **Action**: GET `/api/v1/tenants/{tenantId}`
- **Assertions**:
    - Status: 200 OK
    - Response contains tenant details (id, name, email, status, configuration)

#### Test: Get Non-Existent Tenant

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: GET `/api/v1/tenants/{randomUUID}`
- **Assertions**:
    - Status: 404 NOT FOUND

#### Test: Get Tenant Realm

- **Setup**: Login as SYSTEM_ADMIN, create tenant
- **Action**: GET `/api/v1/tenants/{tenantId}/realm`
- **Assertions**:
    - Status: 200 OK
    - Response contains `tenantId` and `realmName`

#### Test: Get Tenant Status

- **Setup**: Login as SYSTEM_ADMIN, create tenant
- **Action**: GET `/api/v1/tenants/{tenantId}/status`
- **Assertions**:
    - Status: 200 OK
    - Response contains tenant status (ACTIVE, INACTIVE, SUSPENDED)

---

### 8. Authorization Tests

#### Test: TENANT_ADMIN Cannot Access Tenant Management

- **Setup**: Login as TENANT_ADMIN
- **Action**: GET `/api/v1/tenants`
- **Assertions**:
    - Status: 403 FORBIDDEN

#### Test: TENANT_ADMIN Cannot Create Tenants

- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/tenants` with valid data
- **Assertions**:
    - Status: 403 FORBIDDEN

#### Test: SYSTEM_ADMIN Can Access All Tenant Operations

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: Perform all CRUD operations
- **Assertions**:
    - All operations succeed (200 OK or 201 CREATED)

---

### 9. Tenant Isolation Tests

#### Test: Tenant Data Isolated by Schema

- **Setup**:
    - Login as SYSTEM_ADMIN
    - Create Tenant A
    - Create Tenant B
    - Create user in Tenant A
    - Create user in Tenant B
- **Action**:
    - Login as Tenant A user
    - Query users: GET `/api/v1/users`
- **Assertions**:
    - Response contains only Tenant A users
    - Tenant B users not visible

#### Test: Cross-Tenant Access Denied

- **Setup**:
    - Create Tenant A
    - Create Tenant B
    - Login as Tenant A admin
- **Action**: GET `/api/v1/tenants/{tenantBId}`
- **Assertions**:
    - Status: 403 FORBIDDEN (tenant context mismatch)

---

### 10. Edge Case Tests

#### Test: Concurrent Tenant Creation

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: Send 5 concurrent POST requests to create tenants
- **Assertions**:
    - All requests succeed (201 CREATED)
    - Each tenant has unique ID and realm name

#### Test: Tenant Creation with Special Characters in Name

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: POST `/api/v1/tenants` with name "Acme & Co. (Pty) Ltd."
- **Assertions**:
    - Status: 201 CREATED or 400 BAD REQUEST (depending on validation rules)

#### Test: Tenant Creation with Very Long Name

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: POST `/api/v1/tenants` with 500-character name
- **Assertions**:
    - Status: 400 BAD REQUEST
    - Validation error for name length

---

## Test Data Strategy

### Faker Data Generation

Use Faker to generate realistic tenant data:

```java
private CreateTenantRequest createRandomTenantRequest() {
    return CreateTenantRequest.builder()
            .name(faker.company().name())
            .email(faker.internet().emailAddress())
            .configuration(TenantConfiguration.builder()
                    .maxUsers(faker.number().numberBetween(10, 500))
                    .storageQuotaMB(faker.number().numberBetween(1024, 10240))
                    .build())
            .build();
}
```

### Tenant Naming Convention

Prefix tenant names with test identifier for easy cleanup:

- `TEST_TENANT_CREATION_001`
- `TEST_TENANT_LIFECYCLE_002`
- `TEST_TENANT_ISOLATION_003`

---

## Test Class Structure

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.fixture.TenantTestDataBuilder;
import org.junit.jupiter.api.*;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TenantManagementTest extends BaseIntegrationTest {

    private AuthenticationResult systemAdminAuth;

    @BeforeEach
    public void setUpTenantTest() {
        // Login as SYSTEM_ADMIN before each test
        systemAdminAuth = loginAsSystemAdmin();
    }

    // ==================== TENANT CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreateTenant_Success() {
        // Arrange
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest(faker);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/tenants",
                systemAdminAuth.getAccessToken(),
                request
        ).exchange();

        // Assert
        response.expectStatus().isCreated();

        CreateTenantResponse tenant = response.expectBody(CreateTenantResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(tenant).isNotNull();
        assertThat(tenant.getTenantId()).isNotBlank();
        assertThat(tenant.getRealmName()).isNotBlank();
        assertThat(tenant.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @Order(2)
    public void testCreateTenant_DuplicateName() {
        // Arrange
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest(faker);

        // Create first tenant
        authenticatedPost("/api/v1/tenants", systemAdminAuth.getAccessToken(), request)
                .exchange()
                .expectStatus().isCreated();

        // Act - Try to create duplicate
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/tenants",
                systemAdminAuth.getAccessToken(),
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest(); // or 409 CONFLICT
    }

    // ... Additional test methods
}
```

---

## Test Fixtures

### TenantTestDataBuilder

Create `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/fixture/TenantTestDataBuilder.java`:

```java
package com.ccbsa.wms.gateway.api.fixture;

import net.datafaker.Faker;

/**
 * Builder for tenant test data.
 */
public class TenantTestDataBuilder {

    public static CreateTenantRequest buildCreateTenantRequest(Faker faker) {
        return CreateTenantRequest.builder()
                .name(faker.company().name())
                .email(faker.internet().emailAddress())
                .configuration(TenantConfiguration.builder()
                        .maxUsers(100)
                        .storageQuotaMB(5120)
                        .build())
                .build();
    }

    public static CreateTenantRequest buildCreateTenantRequestWithName(String name, Faker faker) {
        return CreateTenantRequest.builder()
                .name(name)
                .email(faker.internet().emailAddress())
                .configuration(TenantConfiguration.builder()
                        .maxUsers(100)
                        .storageQuotaMB(5120)
                        .build())
                .build();
    }
}
```

---

## DTOs Required

### CreateTenantRequest

```java
package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRequest {
    private String name;
    private String email;
    private TenantConfiguration configuration;
}
```

### TenantConfiguration

```java
package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfiguration {
    private Integer maxUsers;
    private Integer storageQuotaMB;
    private List<String> features;
    private Map<String, String> settings;
}
```

### CreateTenantResponse

```java
package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantResponse {
    private String tenantId;
    private String realmName;
    private String status;
}
```

### TenantResponse

```java
package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {
    private String tenantId;
    private String name;
    private String email;
    private String status;
    private TenantConfiguration configuration;
    private String createdAt;
    private String updatedAt;
}
```

### UpdateTenantConfigurationRequest

```java
package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantConfigurationRequest {
    private Integer maxUsers;
    private Integer storageQuotaMB;
    private List<String> features;
    private Map<String, String> settings;
}
```

---

## Test Execution Flow

### Pre-Test Setup

1. Start gateway service (port 8080)
2. Start tenant service (Eureka discovery)
3. Start Keycloak (for realm creation)
4. Start PostgreSQL (for schema provisioning)
5. Load test credentials from environment

### Test Execution Order

1. Tenant creation tests
2. Tenant activation tests
3. Tenant deactivation tests
4. Tenant suspension tests
5. Status lifecycle tests
6. Configuration update tests
7. Query tests (list, get, filter)
8. Authorization tests
9. Isolation tests
10. Edge case tests

### Post-Test Cleanup

- **Option 1**: Leave test tenants for manual inspection
- **Option 2**: Auto-cleanup by deactivating/deleting test tenants
- **Option 3**: Use `@AfterAll` to bulk delete test tenants

---

## Environment Variables

```bash
# System Admin Credentials
TEST_SYSTEM_ADMIN_USERNAME=sysadmin
TEST_SYSTEM_ADMIN_PASSWORD=Password123@

# Gateway URL
GATEWAY_BASE_URL=http://localhost:8080

# Keycloak Configuration
KEYCLOAK_BASE_URL=http://localhost:8180
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin
```

---

## Testing Checklist

- [ ] Tenant creation succeeds with valid data
- [ ] Tenant creation fails with duplicate name
- [ ] Tenant creation fails with invalid data
- [ ] Tenant creation forbidden for non-SYSTEM_ADMIN
- [ ] Tenant activation changes status to ACTIVE
- [ ] Tenant deactivation changes status to INACTIVE
- [ ] Tenant suspension changes status to SUSPENDED
- [ ] Valid status transitions succeed
- [ ] Invalid status transitions fail
- [ ] Tenant configuration updates persist
- [ ] List tenants with pagination works
- [ ] List tenants with status filter works
- [ ] List tenants with search filter works
- [ ] Get tenant by ID returns correct data
- [ ] Get non-existent tenant returns 404
- [ ] Authorization checks prevent non-admin access
- [ ] Tenant isolation verified (schema separation)
- [ ] Concurrent tenant creation handled correctly
- [ ] TenantCreatedEvent published on creation
- [ ] TenantActivatedEvent published on activation
- [ ] TenantDeactivatedEvent published on deactivation
- [ ] TenantSuspendedEvent published on suspension
- [ ] Keycloak realm created on tenant creation
- [ ] Database schema provisioned on tenant creation

---

## Next Steps

1. **Implement TenantManagementTest** with all test scenarios
2. **Create TenantTestDataBuilder** for test data generation
3. **Create DTO classes** for tenant requests/responses
4. **Validate event publishing** (use Kafka consumer or mock)
5. **Test Keycloak integration** (verify realm creation)
6. **Test database schema provisioning** (verify schema exists)
7. **Document test results** and edge cases discovered

---

## Notes

- **Tenant Cleanup**: Consider using `@AfterEach` or `@AfterAll` to clean up test tenants
- **Keycloak Dependency**: Tests require running Keycloak instance for realm creation
- **Database Schema**: Each tenant gets isolated PostgreSQL schema
- **Event Validation**: Consider using embedded Kafka or test containers for event verification
- **Rate Limiting**: Tenant service has 50 req/min limit (consider delays in concurrent tests)
- **Idempotency**: Some operations may be idempotent (e.g., activate already active tenant)
