# Gateway API Integration Tests - Implementation Plans

## Overview

This directory contains comprehensive implementation plans for the gateway API integration tests for the Warehouse Management System. These tests simulate frontend application behavior by authenticating with JWT tokens, managing httpOnly cookies, and testing all microservice functionality through the gateway.

---

## Document Structure

### 1. [BaseIntegrationTest Implementation Plan](01-BaseIntegrationTest-Implementation-Plan.md)
**Purpose**: Foundation for all integration tests

**Key Features**:
- WebTestClient configuration for reactive API testing
- JWT token management (access + refresh tokens)
- HttpOnly cookie handling for refresh tokens
- Authentication helpers (login, logout, refresh)
- Common assertion utilities
- Faker integration for test data generation

**Technologies**:
- Spring Boot Test
- WebFlux WebTestClient
- JUnit 5
- AssertJ
- Datafaker

---

### 2. [TenantManagementTest Implementation Plan](02-TenantManagementTest-Implementation-Plan.md)
**Purpose**: Test tenant CRUD operations and lifecycle management

**Test Coverage**:
- Tenant creation with Keycloak realm provisioning
- Tenant activation, deactivation, suspension
- Tenant configuration updates
- List tenants with pagination and filtering
- Authorization checks (SYSTEM_ADMIN only)
- Tenant isolation validation

**Key DTOs**:
- CreateTenantRequest/Response
- TenantConfiguration
- UpdateTenantConfigurationRequest

---

### 3. [UserManagementTest Implementation Plan](03-UserManagementTest-Implementation-Plan.md)
**Purpose**: Test user CRUD, role assignment, and lifecycle management

**Test Coverage**:
- User creation by SYSTEM_ADMIN (cross-tenant)
- User creation by TENANT_ADMIN (own tenant only)
- Role assignment (15 roles across hierarchy)
- User status lifecycle (ACTIVE, SUSPENDED, INACTIVE)
- Profile updates (self-service and admin)
- Tenant isolation validation

**Key Features**:
- Two-phase testing (SYSTEM_ADMIN → TENANT_ADMIN)
- Wait for user input for TENANT_ADMIN credentials
- Multi-role support testing
- JWT token role claim validation

---

### 4. [AuthenticationTest Implementation Plan](04-AuthenticationTest-Implementation-Plan.md)
**Purpose**: Test complete authentication flow matching frontend behavior

**Test Coverage**:
- Login with username/password
- Access token extraction from response body
- Refresh token extraction from httpOnly cookies
- Token refresh with automatic rotation
- Logout with cookie invalidation
- Concurrent token refresh (race condition prevention)
- JWT token validation (claims, signature)
- Cookie security (httpOnly, SameSite, Secure flags)

**Key Features**:
- Correlation ID generation and tracking
- CORS header validation
- Rate limiting tests (500 req/min on BFF endpoints)
- Token expiration detection and handling

---

### 5. [ProductManagementTest Implementation Plan](05-ProductManagementTest-Implementation-Plan.md)
**Purpose**: Test product management with barcode validation

**Test Coverage**:
- Manual product creation (single and multiple barcodes)
- CSV bulk upload with validation
- Product queries (list, search, filter)
- Barcode validation (EAN-13, UPC-A)
- Product updates (add/remove secondary barcodes)
- Tenant isolation validation

**Key Features**:
- Barcode checksum generation (EAN-13)
- CSV upload with error handling
- Pagination and filtering tests

---

### 6. [LocationManagementTest Implementation Plan](06-LocationManagementTest-Implementation-Plan.md)
**Purpose**: Test location hierarchy and capacity management

**Test Coverage**:
- Location hierarchy creation (Warehouse → Zone → Aisle → Rack → Bin)
- Location path auto-generation
- Parent-child relationship validation
- Capacity management and enforcement
- Location status transitions (ACTIVE, INACTIVE, MAINTENANCE)
- List child locations and ancestors

**Key Features**:
- 5-level location hierarchy testing
- Capacity validation per location type
- Location movement (parent changes)

---

### 7. [StockManagementTest Implementation Plan](07-StockManagementTest-Implementation-Plan.md)
**Purpose**: Test stock receipt, allocation, and movement

**Test Coverage**:
- Manual consignment receipt
- CSV bulk consignment upload
- Stock allocation with FEFO logic
- Stock movement between locations
- Stock adjustments (increase/decrease)
- Expiration date tracking and alerts

**Key Features**:
- FEFO (First-Expired-First-Out) allocation testing
- Batch number management
- Expiration date validation
- Stock level queries

---

### 8. [Other Microservices Test Plans](08-OtherMicroservices-Test-Implementation-Plans.md)
**Purpose**: Test picking, returns, and reconciliation services

**Test Coverage**:

#### Picking Service
- Picking task creation and assignment
- Task lifecycle (PENDING → ASSIGNED → IN_PROGRESS → COMPLETED)
- Partial picks (short picks)
- Task cancellation

#### Returns Service
- Return order creation and authorization
- Return processing (receive, restock, dispose)
- Return reasons and conditions
- Restocking logic based on condition

#### Reconciliation Service
- Cycle count creation and execution
- Variance detection and approval
- Stock adjustment based on physical count
- Reconciliation history tracking

---

### 9. [Test Data Builders and Helper Classes](09-TestData-Builders-and-Helpers-Plan.md)
**Purpose**: Reusable test data generation and helper utilities

**Components**:

#### Test Data Builders
- TenantTestDataBuilder
- UserTestDataBuilder
- ProductTestDataBuilder
- LocationTestDataBuilder
- ConsignmentTestDataBuilder
- PickingTestDataBuilder
- ReturnsTestDataBuilder
- ReconciliationTestDataBuilder

#### Helper Classes
- AuthenticationHelper
- TenantHelper
- UserHelper
- StockHelper

#### Utilities
- BarcodeGenerator (EAN-13, UPC-A with checksum)
- CsvTestDataGenerator
- RequestHeaderHelper
- CookieExtractor

**Key Features**:
- Faker integration for realistic test data
- Valid barcode generation with checksums
- CSV file generation for bulk upload tests
- Helper classes reduce boilerplate code

---

### 10. [Test Execution Strategy and CI/CD Integration](10-Test-Execution-Strategy-and-CI-CD-Integration.md)
**Purpose**: Test execution, reporting, and continuous integration

**Key Features**:

#### Test Execution
- Maven Failsafe configuration
- Test profiles (smoke, regression, ci)
- Test execution order and phases
- Parallel execution considerations

#### CI/CD Integration
- GitHub Actions workflow
- Docker Compose for local testing
- Service health checks
- Test result publishing

#### Test Reporting
- JUnit HTML reports
- Allure report integration
- Test coverage metrics
- SonarQube integration

#### Test Data Management
- Cleanup strategies
- Test isolation techniques
- Database reset options

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
1. Implement BaseIntegrationTest
2. Create utility classes (WebTestClientConfig, RequestHeaderHelper, CookieExtractor)
3. Create DTO classes (LoginRequest, LoginResponse, UserContext, AuthenticationResult)
4. Implement AuthenticationHelper
5. Write AuthenticationTest

### Phase 2: Core Services (Week 3-4)
1. Implement TenantManagementTest
2. Implement UserManagementTest
3. Create TenantTestDataBuilder and UserTestDataBuilder
4. Verify tenant isolation and authorization

### Phase 3: Business Services (Week 5-6)
1. Implement ProductManagementTest
2. Implement LocationManagementTest
3. Implement StockManagementTest
4. Create ProductTestDataBuilder, LocationTestDataBuilder, ConsignmentTestDataBuilder
5. Implement BarcodeGenerator utility

### Phase 4: Workflow Services (Week 7-8)
1. Implement PickingServiceTest
2. Implement ReturnsServiceTest
3. Implement ReconciliationServiceTest
4. Create corresponding test data builders
5. Test end-to-end workflows

### Phase 5: CI/CD Integration (Week 9-10)
1. Configure Maven Failsafe plugin
2. Create test execution profiles
3. Set up GitHub Actions workflow
4. Configure Docker Compose for local testing
5. Integrate Allure reporting
6. Set up SonarQube analysis

---

## Test Environment Setup

### Prerequisites

1. **Services Running**:
   - Gateway Service (port 8080)
   - All microservices (Eureka discovery)
   - Keycloak (port 8180)
   - PostgreSQL (port 5432)
   - Kafka (port 9092)
   - Redis (port 6379)

2. **Environment Variables**:
   ```bash
   TEST_SYSTEM_ADMIN_USERNAME=sysadmin
   TEST_SYSTEM_ADMIN_PASSWORD=Password123@
   TEST_TENANT_ADMIN_USERNAME=<user-input>
   TEST_TENANT_ADMIN_PASSWORD=Password123@
   GATEWAY_BASE_URL=http://localhost:8080
   ```

3. **Dependencies**:
   - JDK 17+
   - Maven 3.8+
   - Docker & Docker Compose (optional, for containerized testing)

---

## Quick Start

### Run All Tests

```bash
cd gateway-api-tests
mvn clean verify
```

### Run Smoke Tests

```bash
mvn clean verify -Psmoke
```

### Run Specific Test Class

```bash
mvn clean verify -Dit.test=AuthenticationTest
```

### Run with Custom Gateway URL

```bash
mvn clean verify -Dgateway.base.url=http://192.168.1.100:8080
```

---

## Test Execution Flow

```
1. Start all services (gateway, microservices, Keycloak, PostgreSQL, Kafka)
   ↓
2. Run AuthenticationTest (verify login, token refresh, logout)
   ↓
3. Run TenantManagementTest (SYSTEM_ADMIN creates tenants)
   ↓
4. Run UserManagementTest Phase 1 (SYSTEM_ADMIN creates users)
   ↓
5. Wait for TEST_TENANT_ADMIN credentials (user input)
   ↓
6. Run UserManagementTest Phase 2 (TENANT_ADMIN tests)
   ↓
7. Run ProductManagementTest (create products)
   ↓
8. Run LocationManagementTest (create locations)
   ↓
9. Run StockManagementTest (create consignments)
   ↓
10. Run PickingServiceTest, ReturnsServiceTest, ReconciliationServiceTest
   ↓
11. Generate test reports (JUnit, Allure)
```

---

## Key Design Principles

1. **Simulate Frontend Behavior**:
   - Extract access tokens from response body
   - Extract refresh tokens from httpOnly cookies
   - Use Bearer tokens for authenticated requests
   - Handle token refresh automatically

2. **Realistic Test Data**:
   - Use Faker for data generation
   - Generate valid barcodes with checksums
   - Create realistic hierarchies (locations, users, roles)

3. **Avoid DRY Violations**:
   - BaseIntegrationTest provides common functionality
   - Test data builders eliminate boilerplate
   - Helper classes simplify complex setups

4. **Test Isolation**:
   - Each test is independent
   - Tenant isolation verified in all tests
   - Cleanup strategies prevent data pollution

5. **Authorization Testing**:
   - Verify SYSTEM_ADMIN vs TENANT_ADMIN access
   - Test role-based access control (15 roles)
   - Validate tenant context enforcement

---

## Test Coverage Summary

| Service | Test Class | Key Areas |
|---------|-----------|-----------|
| Authentication | AuthenticationTest | Login, refresh, logout, JWT validation |
| Tenant Service | TenantManagementTest | CRUD, lifecycle, isolation |
| User Service | UserManagementTest | CRUD, roles, lifecycle, isolation |
| Product Service | ProductManagementTest | CRUD, barcodes, CSV upload |
| Location Service | LocationManagementTest | Hierarchy, capacity, status |
| Stock Service | StockManagementTest | Receipt, FEFO, movement, adjustments |
| Picking Service | PickingServiceTest | Task lifecycle, allocation |
| Returns Service | ReturnsServiceTest | Authorization, processing, restocking |
| Reconciliation Service | ReconciliationServiceTest | Cycle counts, variance, adjustments |

**Total Test Classes**: 9
**Estimated Test Methods**: 200+
**Test Execution Time**: ~15-20 minutes (full suite)

---

## Technology Stack

- **Testing Framework**: JUnit 5
- **HTTP Client**: Spring WebFlux WebTestClient
- **Assertions**: AssertJ
- **Test Data**: Datafaker (formerly JavaFaker)
- **JWT Parsing**: Nimbus JOSE JWT
- **Build Tool**: Maven
- **CI/CD**: GitHub Actions
- **Reporting**: JUnit HTML, Allure
- **Code Quality**: SonarQube

---

## Contributing

When adding new tests:

1. Extend `BaseIntegrationTest`
2. Use test data builders from `fixture` package
3. Follow naming conventions (`test<Action>_<ExpectedResult>`)
4. Add appropriate `@Order` annotations for test execution order
5. Verify tenant isolation in all tests
6. Update this README with new test coverage

---

## Support

For questions or issues:
- Review implementation plans in this directory
- Check `BaseIntegrationTest` for common utilities
- Consult test data builders for realistic data generation
- Review CI/CD integration for pipeline setup

---

## License

Internal Coca-Cola Beverages South Africa (CCBSA) - Warehouse Management System
