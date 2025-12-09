# Gateway API Tests

Integration tests for Gateway Service API endpoints that simulate the frontend app's authentication flow.

## Overview

This module contains integration tests that verify the gateway-service API endpoints work correctly. The tests simulate the frontend app's behavior by:

1. Logging in with user credentials to obtain JWT tokens
2. Extracting access tokens from response bodies
3. Extracting refresh tokens from httpOnly cookies
4. Using tokens for subsequent authenticated API calls

## Prerequisites

Before running the tests, ensure the following services are running:

- **Gateway Service** - Running on `https://localhost:8080` (or configured URL)
- **User Service** - Registered with Eureka and accessible via gateway
- **Tenant Service** - Registered with Eureka and accessible via gateway
- **Keycloak** - Running and configured with test users
- **Eureka Server** - Service discovery running
- **Redis** - For rate limiting (optional, gateway will degrade gracefully)

## Configuration

### Environment Variables

The tests can be configured using environment variables:

```bash
# Gateway base URL (default: https://localhost:8080/api/v1)
export GATEWAY_BASE_URL=https://localhost:8080/api/v1

# Test user credentials (default: admin/admin)
export TEST_USERNAME=sysadmin
export TEST_PASSWORD=Password123@

# Alternative test users
export TEST_SYSTEM_ADMIN_USERNAME=sysadmin
export TEST_SYSTEM_ADMIN_PASSWORD=Password123@
export TEST_TENANT_ADMIN_USERNAME=testuser1@cm-sol.co.za
export TEST_TENANT_ADMIN_PASSWORD=Password123@
export TEST_REGULAR_USER_USERNAME=testuser2@cm-sol.co.za
export TEST_REGULAR_USER_PASSWORD=Password123@
```

### Test Configuration File

Configuration can also be set in `src/test/resources/application-test.yml`:

```yaml
gateway:
  base-url: https://localhost:8080/api/v1
  timeout-seconds: 30

test:
  users:
    system-admin:
      username: admin
      password: admin
```

## Running Tests

### Run All Tests

```bash
# From project root
mvn test -pl gateway-api-tests

# Or from gateway-api-tests directory
cd gateway-api-tests
mvn test
```

### Run Specific Test Class

```bash
# Run authentication tests only
mvn test -pl gateway-api-tests -Dtest=AuthenticationTest

# Run user management tests only
mvn test -pl gateway-api-tests -Dtest=UserManagementTest

# Run tenant management tests only
mvn test -pl gateway-api-tests -Dtest=TenantManagementTest

# Run security tests only
mvn test -pl gateway-api-tests -Dtest=SecurityTest
```

### Run Specific Test Method

```bash
mvn test -pl gateway-api-tests -Dtest=AuthenticationTest#shouldLoginWithValidCredentials
```

### With Custom Configuration

```bash
GATEWAY_BASE_URL=https://gateway.example.com/api/v1 \
TEST_USERNAME=myuser \
TEST_PASSWORD=mypassword \
mvn test -pl gateway-api-tests
```

## Test Structure

### Test Classes

- **`AuthenticationTest`** - Tests login, token refresh, get current user, and logout
- **`UserManagementTest`** - Tests user CRUD operations (list, get, create, update, activate, deactivate, suspend, roles)
- **`TenantManagementTest`** - Tests tenant CRUD operations (list, get, create, activate, deactivate, suspend)
- **`SecurityTest`** - Tests security requirements (public endpoints, protected endpoints, token validation)

### Helper Classes

- **`AuthenticationHelper`** - Utility class for authentication operations (login, refresh, logout, getCurrentUser)
- **`WebTestClientConfig`** - Configuration utility for WebTestClient with SSL support

### DTOs

- **`LoginRequest`** - Login request DTO
- **`LoginResponse`** - Login response DTO with access token and user context
- **`UserContext`** - User information DTO
- **`AuthenticationResult`** - Result containing tokens and user context

## Test Flow

### Authentication Flow

1. **Login**: POST `/api/v1/bff/auth/login` with username/password
    - Extracts `accessToken` from response body (`data.accessToken`)
    - Extracts `refreshToken` from Set-Cookie header (`refreshToken`)
    - Returns `AuthenticationResult` with tokens and user context

2. **Authenticated Requests**: Include `Authorization: Bearer {accessToken}` header
    - All protected endpoints require this header
    - Gateway validates JWT token with Keycloak

3. **Token Refresh**: POST `/api/v1/bff/auth/refresh` with refresh token cookie
    - Refresh token is sent via httpOnly cookie
    - Returns new access token

4. **Get Current User**: GET `/api/v1/bff/auth/me` with access token
    - Returns user context for authenticated user

5. **Logout**: POST `/api/v1/bff/auth/logout` with access token and refresh token cookie
    - Clears tokens on server side

## SSL Configuration

The tests are configured to work with self-signed SSL certificates (common in development). The `WebTestClientConfig` uses relaxed SSL validation (`InsecureTrustManagerFactory`) to
allow testing against HTTPS endpoints with self-signed certificates.

**Warning**: This configuration is for testing only. Never use relaxed SSL validation in production code.

## Troubleshooting

### Tests Fail with Connection Errors

- Verify gateway-service is running: `curl -k https://localhost:8080/actuator/health`
- Check gateway logs: `tail -f logs/gateway-service.log`
- Verify Eureka registration: Check Eureka dashboard at `http://localhost:8761`

### Authentication Failures

- Verify Keycloak is running and accessible
- Check test user credentials in Keycloak
- Verify Keycloak realm and client configuration
- Check gateway JWT decoder configuration

### SSL Certificate Errors

- Ensure gateway is using HTTPS (port 8080 with SSL enabled)
- Tests use relaxed SSL validation, so certificate errors should not occur
- If issues persist, verify keystore configuration in gateway

### 401 Unauthorized Errors

- Verify access token is being included in Authorization header
- Check token expiration (tokens may expire during long test runs)
- Verify JWT token is valid and signed by Keycloak
- Check gateway security configuration

## Integration with CI/CD

These tests can be integrated into CI/CD pipelines:

```yaml
# Example GitHub Actions step
- name: Run Gateway API Tests
  env:
    GATEWAY_BASE_URL: https://gateway-test.example.com/api/v1
    TEST_USERNAME: ${{ secrets.TEST_USERNAME }}
    TEST_PASSWORD: ${{ secrets.TEST_PASSWORD }}
  run: mvn test -pl gateway-api-tests
```

## Notes

- Tests assume services are already running (no service startup in tests)
- Tests use real Keycloak authentication (not mocked)
- Tests properly handle httpOnly cookies like a browser would
- Tests validate both success and error scenarios
- Some tests may return 404 if test data doesn't exist, but still validate endpoint accessibility

