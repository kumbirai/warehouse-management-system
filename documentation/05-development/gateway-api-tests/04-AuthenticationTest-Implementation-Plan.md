# AuthenticationTest Implementation Plan

## Overview
`AuthenticationTest` validates the complete authentication flow through the gateway BFF (Backend-For-Frontend) endpoints. Tests verify login, token refresh, logout, JWT token handling, httpOnly cookie management, token expiration, and security controls that mirror the frontend application's authentication behavior.

---

## Objectives

1. **Login Flow**: Test successful and failed login attempts
2. **Token Management**: Validate access token extraction from response body
3. **Cookie Management**: Validate refresh token extraction from httpOnly cookies
4. **Token Refresh**: Test token refresh with automatic token rotation
5. **Token Expiration**: Test expired token detection and automatic refresh
6. **Logout Flow**: Test logout with cookie invalidation
7. **Concurrent Token Refresh**: Test race condition prevention for simultaneous refresh requests
8. **User Context**: Validate user context returned in login response
9. **Security Controls**: Test token validation, correlation ID, and CORS headers
10. **Error Handling**: Test authentication errors (invalid credentials, expired tokens)

---

## Test Scenarios

### 1. Login Tests

#### Test: Login Successfully with Valid Credentials
- **Setup**: Use SYSTEM_ADMIN credentials
- **Action**: POST `/api/v1/bff/auth/login`
- **Request Body**:
  ```json
  {
    "username": "sysadmin",
    "password": "Password123@"
  }
  ```
- **Assertions**:
  - Status: 200 OK
  - Response body contains:
    - `accessToken` (JWT string, not empty)
    - `userContext` (userId, username, email, tenantId, roles)
    - `expiresIn` (token expiration time in seconds)
  - Response headers contain `Set-Cookie` with `refresh_token`
  - Refresh token cookie properties:
    - `httpOnly=true`
    - `SameSite=Strict`
    - `Secure=true` (if HTTPS)
    - `MaxAge > 0`
  - Access token is valid JWT (can be decoded)
  - JWT claims include:
    - `sub` (subject/user ID)
    - `tenant_id` (tenant context)
    - `realm_access.roles` (user roles)
    - `iat` (issued at timestamp)
    - `exp` (expiration timestamp)
    - `iss` (issuer)

#### Test: Login Fails with Invalid Username
- **Setup**: Use invalid username
- **Action**: POST `/api/v1/bff/auth/login`
- **Request Body**:
  ```json
  {
    "username": "nonexistent",
    "password": "Password123@"
  }
  ```
- **Assertions**:
  - Status: 401 UNAUTHORIZED
  - Error message indicates invalid credentials
  - No access token returned
  - No refresh token cookie set

#### Test: Login Fails with Invalid Password
- **Setup**: Use valid username, invalid password
- **Action**: POST `/api/v1/bff/auth/login`
- **Request Body**:
  ```json
  {
    "username": "sysadmin",
    "password": "WrongPassword"
  }
  ```
- **Assertions**:
  - Status: 401 UNAUTHORIZED
  - Error message indicates invalid credentials

#### Test: Login Fails with Missing Username
- **Setup**: Missing username field
- **Action**: POST `/api/v1/bff/auth/login`
- **Request Body**:
  ```json
  {
    "password": "Password123@"
  }
  ```
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Validation error for missing username

#### Test: Login Fails with Missing Password
- **Setup**: Missing password field
- **Action**: POST `/api/v1/bff/auth/login`
- **Request Body**:
  ```json
  {
    "username": "sysadmin"
  }
  ```
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Validation error for missing password

#### Test: Login Fails with Empty Credentials
- **Setup**: Empty username and password
- **Action**: POST `/api/v1/bff/auth/login`
- **Request Body**:
  ```json
  {
    "username": "",
    "password": ""
  }
  ```
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Validation errors for empty fields

#### Test: Login for Deactivated User
- **Setup**:
  - Login as SYSTEM_ADMIN
  - Create user
  - Deactivate user
- **Action**: POST `/api/v1/bff/auth/login` with deactivated user credentials
- **Assertions**:
  - Status: 401 UNAUTHORIZED or 403 FORBIDDEN
  - Error message indicates account disabled

#### Test: Login for Suspended User
- **Setup**:
  - Login as SYSTEM_ADMIN
  - Create user
  - Suspend user
- **Action**: POST `/api/v1/bff/auth/login` with suspended user credentials
- **Assertions**:
  - Status: 403 FORBIDDEN
  - Error message indicates account suspended

---

### 2. Token Refresh Tests

#### Test: Refresh Token Successfully
- **Setup**:
  - Login to get access token and refresh token cookie
- **Action**: POST `/api/v1/bff/auth/refresh`
  - Include refresh token cookie in request
- **Assertions**:
  - Status: 200 OK
  - Response contains new `accessToken`
  - Response headers contain new `refresh_token` cookie (token rotation)
  - New access token is different from original
  - New refresh token cookie is different from original
  - Old refresh token invalid after rotation

#### Test: Refresh Token Fails with Missing Cookie
- **Setup**: No authentication
- **Action**: POST `/api/v1/bff/auth/refresh` without refresh token cookie
- **Assertions**:
  - Status: 401 UNAUTHORIZED
  - Error message indicates missing or invalid refresh token

#### Test: Refresh Token Fails with Invalid Cookie
- **Setup**: Use fake/random refresh token
- **Action**: POST `/api/v1/bff/auth/refresh` with invalid cookie
- **Assertions**:
  - Status: 401 UNAUTHORIZED
  - Error message indicates invalid refresh token

#### Test: Refresh Token Fails with Expired Cookie
- **Setup**:
  - Login to get refresh token
  - Wait for refresh token to expire (or mock expiration)
- **Action**: POST `/api/v1/bff/auth/refresh` with expired cookie
- **Assertions**:
  - Status: 401 UNAUTHORIZED
  - Error message indicates expired refresh token
  - User must re-login

#### Test: Refresh Token Rotation (Old Token Invalidated)
- **Setup**:
  - Login to get refresh token (token1)
  - Refresh to get new refresh token (token2)
- **Action**: POST `/api/v1/bff/auth/refresh` with token1 (old token)
- **Assertions**:
  - Status: 401 UNAUTHORIZED
  - Old refresh token no longer valid after rotation

---

### 3. Token Expiration Tests

#### Test: Access Token Expiration Detection
- **Setup**:
  - Login to get access token
  - Wait for token expiration (or mock expiration)
- **Action**: GET `/api/v1/users` with expired access token
- **Assertions**:
  - Status: 401 UNAUTHORIZED
  - Error message indicates token expired

#### Test: Automatic Token Refresh on 401
- **Setup**:
  - Login to get tokens
  - Make API call with expired access token
  - Frontend automatically triggers refresh
- **Action**:
  1. GET `/api/v1/users` with expired token (returns 401)
  2. POST `/api/v1/bff/auth/refresh` with refresh token cookie
  3. Retry GET `/api/v1/users` with new access token
- **Assertions**:
  - First request: 401 UNAUTHORIZED
  - Refresh: 200 OK with new access token
  - Retry: 200 OK with user data

#### Test: Proactive Token Refresh at 80% Lifetime
- **Setup**:
  - Login to get access token with 1000s expiration
  - Wait until 800s elapsed (80% of lifetime)
- **Action**: Proactively refresh token before expiration
- **Assertions**:
  - Refresh succeeds before token actually expires
  - Prevents 401 errors during active session

---

### 4. Logout Tests

#### Test: Logout Successfully
- **Setup**: Login to get tokens
- **Action**: POST `/api/v1/bff/auth/logout` with refresh token cookie
- **Assertions**:
  - Status: 200 OK
  - Response headers contain `Set-Cookie` with:
    - `refresh_token` cookie with `MaxAge=0` (deleted)
    - Or empty cookie value
  - Subsequent requests with old access token fail (401)
  - Subsequent refresh attempts fail (401)

#### Test: Logout Clears Client-Side Tokens
- **Setup**: Login to get tokens
- **Action**:
  1. Store access token in memory
  2. Logout
  3. Verify access token cleared from memory
- **Assertions**:
  - Access token no longer in memory
  - Refresh token cookie cleared
  - User cannot make authenticated requests

#### Test: Logout Without Refresh Token Cookie
- **Setup**: No authentication
- **Action**: POST `/api/v1/bff/auth/logout` without cookie
- **Assertions**:
  - Status: 200 OK or 401 UNAUTHORIZED (graceful handling)

---

### 5. User Context Tests

#### Test: Get Current User Context
- **Setup**: Login to get access token
- **Action**: GET `/api/v1/bff/auth/me` with Bearer token
- **Assertions**:
  - Status: 200 OK
  - Response contains user context:
    - `userId`
    - `username`
    - `email`
    - `firstName`
    - `lastName`
    - `tenantId`
    - `roles` (array of role names)

#### Test: Get User Context Without Token
- **Setup**: No authentication
- **Action**: GET `/api/v1/bff/auth/me` without token
- **Assertions**:
  - Status: 401 UNAUTHORIZED

#### Test: Get User Context with Invalid Token
- **Setup**: Use fake/malformed JWT token
- **Action**: GET `/api/v1/bff/auth/me` with invalid token
- **Assertions**:
  - Status: 401 UNAUTHORIZED
  - Error message indicates invalid token

---

### 6. Concurrent Token Refresh Tests

#### Test: Prevent Race Condition on Concurrent Refresh
- **Setup**:
  - Login to get tokens
  - Simulate frontend scenario: Multiple API calls return 401 simultaneously
- **Action**:
  - Send 5 concurrent POST requests to `/api/v1/bff/auth/refresh`
- **Assertions**:
  - Only one refresh request completes successfully (200 OK)
  - Other requests either:
    - Wait for shared refresh promise and use new token
    - Or return 409 CONFLICT indicating refresh in progress
  - All subsequent API calls use the same new access token
  - No multiple token refreshes triggered

#### Test: Concurrent Requests Share Refreshed Token
- **Setup**:
  - Login
  - Trigger token expiration
  - Make multiple concurrent API calls (all get 401)
- **Action**:
  1. All requests detect 401
  2. Single refresh request triggered
  3. All waiting requests use new token
- **Assertions**:
  - Only one refresh request sent to backend
  - All concurrent requests eventually succeed with new token
  - Race condition prevented by shared promise mechanism

---

### 7. JWT Token Validation Tests

#### Test: Access Token Contains Valid JWT Claims
- **Setup**: Login to get access token
- **Action**: Decode JWT token (without verification, just parsing)
- **Assertions**:
  - JWT has 3 parts (header.payload.signature)
  - Payload contains:
    - `sub` (user ID)
    - `tenant_id` (tenant context)
    - `realm_access.roles` (user roles array)
    - `iat` (issued at timestamp)
    - `exp` (expiration timestamp > iat)
    - `iss` (issuer URL)

#### Test: Access Token Signature Valid
- **Setup**: Login to get access token
- **Action**: Verify JWT signature using Keycloak public key
- **Assertions**:
  - Signature validation succeeds
  - Token not tampered with

#### Test: Access Token Contains Correct User Roles
- **Setup**: Login as user with specific roles (e.g., WAREHOUSE_MANAGER, PICKER)
- **Action**: Decode JWT token and extract roles
- **Assertions**:
  - `realm_access.roles` contains all assigned roles
  - Roles match user's assigned roles in database

#### Test: Access Token Contains Correct Tenant Context
- **Setup**: Login as user in specific tenant
- **Action**: Decode JWT token and extract tenant_id
- **Assertions**:
  - `tenant_id` claim matches user's tenant
  - Tenant context used for request isolation

---

### 8. Cookie Security Tests

#### Test: Refresh Token Cookie Has HttpOnly Flag
- **Setup**: Login to get refresh token cookie
- **Action**: Inspect `Set-Cookie` header
- **Assertions**:
  - Cookie contains `HttpOnly` attribute
  - JavaScript cannot access cookie (XSS protection)

#### Test: Refresh Token Cookie Has SameSite Flag
- **Setup**: Login to get refresh token cookie
- **Action**: Inspect `Set-Cookie` header
- **Assertions**:
  - Cookie contains `SameSite=Strict` or `SameSite=Lax`
  - CSRF protection enabled

#### Test: Refresh Token Cookie Has Secure Flag (HTTPS)
- **Setup**: Login via HTTPS endpoint
- **Action**: Inspect `Set-Cookie` header
- **Assertions**:
  - Cookie contains `Secure` attribute
  - Cookie only sent over HTTPS

#### Test: Refresh Token Cookie Has Proper MaxAge
- **Setup**: Login to get refresh token cookie
- **Action**: Inspect `Set-Cookie` header
- **Assertions**:
  - `MaxAge` value is positive (e.g., 7 days = 604800 seconds)
  - Cookie persists across browser sessions

---

### 9. Correlation ID Tests

#### Test: Correlation ID Generated for Each Request
- **Setup**: Login
- **Action**: Make multiple API calls with access token
- **Assertions**:
  - Each request includes `X-Correlation-Id` header
  - Correlation IDs are unique per request
  - Response includes same correlation ID for tracing

#### Test: Correlation ID Persists Through Token Refresh
- **Setup**: Login and get correlation ID
- **Action**: Refresh token with same correlation ID
- **Assertions**:
  - Refresh request includes original correlation ID
  - Response includes same correlation ID

---

### 10. CORS Tests

#### Test: CORS Headers Present on Login
- **Setup**: Send OPTIONS preflight request to `/api/v1/bff/auth/login`
- **Action**: OPTIONS `/api/v1/bff/auth/login`
- **Assertions**:
  - Status: 200 OK
  - Headers include:
    - `Access-Control-Allow-Origin` (matches allowed origins)
    - `Access-Control-Allow-Methods` (includes POST)
    - `Access-Control-Allow-Credentials: true`
    - `Access-Control-Max-Age: 3600`

#### Test: CORS Headers Present on Refresh
- **Setup**: Send OPTIONS preflight request to `/api/v1/bff/auth/refresh`
- **Action**: OPTIONS `/api/v1/bff/auth/refresh`
- **Assertions**:
  - CORS headers present

---

### 11. Rate Limiting Tests

#### Test: Rate Limit on Login Endpoint
- **Setup**: None
- **Action**: Send 600 login requests within 1 minute (exceeds 500 req/min limit)
- **Assertions**:
  - First 500 requests: 200 OK or 401 UNAUTHORIZED (auth errors)
  - Requests 501-600: 429 TOO MANY REQUESTS
  - Response headers include:
    - `X-RateLimit-Remaining: 0`
    - `Retry-After: <seconds>`

#### Test: Rate Limit Resets After 1 Minute
- **Setup**: Trigger rate limit (send 500 requests)
- **Action**:
  1. Wait 60 seconds
  2. Send new login request
- **Assertions**:
  - After waiting, request succeeds (200 OK)
  - Rate limit reset

---

### 12. Error Handling Tests

#### Test: Login with Malformed JSON
- **Setup**: Send invalid JSON to login endpoint
- **Action**: POST `/api/v1/bff/auth/login` with malformed JSON
- **Request Body**: `{"username": "sysadmin", "password": }`
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Error message indicates JSON parse error

#### Test: Login with Content-Type Mismatch
- **Setup**: Send login request without `Content-Type: application/json`
- **Action**: POST `/api/v1/bff/auth/login` with `Content-Type: text/plain`
- **Assertions**:
  - Status: 415 UNSUPPORTED MEDIA TYPE

#### Test: Refresh with Malformed Cookie
- **Setup**: Send refresh request with invalid cookie format
- **Action**: POST `/api/v1/bff/auth/refresh` with malformed cookie
- **Assertions**:
  - Status: 401 UNAUTHORIZED or 400 BAD REQUEST

---

## Test Class Structure

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.LoginRequest;
import com.ccbsa.wms.gateway.api.dto.LoginResponse;
import com.ccbsa.wms.gateway.api.dto.UserContext;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthenticationTest extends BaseIntegrationTest {

    // ==================== LOGIN TESTS ====================

    @Test
    @Order(1)
    public void testLogin_Success() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .username(systemAdminUsername)
                .password(systemAdminPassword)
                .build();

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/bff/auth/login")
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk();

        LoginResponse loginResponse = response.expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getAccessToken()).isNotBlank();
        assertThat(loginResponse.getUserContext()).isNotNull();
        assertThat(loginResponse.getExpiresIn()).isGreaterThan(0);

        // Validate refresh token cookie
        HttpHeaders headers = response.returnResult(LoginResponse.class).getResponseHeaders();
        ResponseCookie refreshTokenCookie = extractRefreshTokenCookie(headers);
        assertRefreshTokenCookieValid(refreshTokenCookie);
    }

    @Test
    @Order(2)
    public void testLogin_InvalidUsername() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .username("nonexistent")
                .password(systemAdminPassword)
                .build();

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/bff/auth/login")
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isUnauthorized();
    }

    // ==================== TOKEN REFRESH TESTS ====================

    @Test
    @Order(10)
    public void testRefreshToken_Success() {
        // Arrange
        AuthenticationResult auth = loginAsSystemAdmin();

        // Act - Refresh token
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/bff/auth/refresh")
                .cookie(auth.getRefreshTokenCookie().getName(), auth.getRefreshTokenCookie().getValue())
                .exchange();

        // Assert
        response.expectStatus().isOk();

        LoginResponse refreshResponse = response.expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(refreshResponse).isNotNull();
        assertThat(refreshResponse.getAccessToken()).isNotBlank();
        assertThat(refreshResponse.getAccessToken()).isNotEqualTo(auth.getAccessToken());

        // Verify new refresh token (token rotation)
        HttpHeaders headers = response.returnResult(LoginResponse.class).getResponseHeaders();
        ResponseCookie newRefreshToken = extractRefreshTokenCookie(headers);
        assertThat(newRefreshToken.getValue()).isNotEqualTo(auth.getRefreshTokenCookie().getValue());
    }

    @Test
    @Order(11)
    public void testRefreshToken_MissingCookie() {
        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/bff/auth/refresh")
                .exchange();

        // Assert
        response.expectStatus().isUnauthorized();
    }

    // ==================== CONCURRENT REFRESH TESTS ====================

    @Test
    @Order(20)
    public void testConcurrentRefresh_PreventRaceCondition() throws Exception {
        // Arrange
        AuthenticationResult auth = loginAsSystemAdmin();
        ExecutorService executor = Executors.newFixedThreadPool(5);

        // Act - Send 5 concurrent refresh requests
        List<CompletableFuture<WebTestClient.ResponseSpec>> futures = List.of(
                CompletableFuture.supplyAsync(() -> refreshTokenRequest(auth.getRefreshTokenCookie()), executor),
                CompletableFuture.supplyAsync(() -> refreshTokenRequest(auth.getRefreshTokenCookie()), executor),
                CompletableFuture.supplyAsync(() -> refreshTokenRequest(auth.getRefreshTokenCookie()), executor),
                CompletableFuture.supplyAsync(() -> refreshTokenRequest(auth.getRefreshTokenCookie()), executor),
                CompletableFuture.supplyAsync(() -> refreshTokenRequest(auth.getRefreshTokenCookie()), executor)
        );

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Assert - Only one request should succeed, others should fail or wait
        long successCount = futures.stream()
                .map(CompletableFuture::join)
                .filter(r -> r.returnResult(LoginResponse.class).getStatus().is2xxSuccessful())
                .count();

        assertThat(successCount).isEqualTo(1);

        executor.shutdown();
    }

    // ==================== JWT TOKEN VALIDATION TESTS ====================

    @Test
    @Order(30)
    public void testJwtToken_ValidClaims() throws Exception {
        // Arrange
        AuthenticationResult auth = loginAsSystemAdmin();

        // Act - Parse JWT token
        JWT jwt = JWTParser.parse(auth.getAccessToken());

        // Assert
        assertThat(jwt.getJWTClaimsSet().getSubject()).isNotBlank();
        assertThat(jwt.getJWTClaimsSet().getClaim("tenant_id")).isNotNull();
        assertThat(jwt.getJWTClaimsSet().getClaim("realm_access")).isNotNull();
        assertThat(jwt.getJWTClaimsSet().getIssueTime()).isNotNull();
        assertThat(jwt.getJWTClaimsSet().getExpirationTime()).isNotNull();
    }

    // ==================== LOGOUT TESTS ====================

    @Test
    @Order(40)
    public void testLogout_Success() {
        // Arrange
        AuthenticationResult auth = loginAsSystemAdmin();

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/bff/auth/logout")
                .cookie(auth.getRefreshTokenCookie().getName(), auth.getRefreshTokenCookie().getValue())
                .exchange();

        // Assert
        response.expectStatus().isOk();

        // Verify refresh token cookie cleared
        HttpHeaders headers = response.returnResult(Void.class).getResponseHeaders();
        ResponseCookie clearedCookie = extractRefreshTokenCookie(headers);
        if (clearedCookie != null) {
            assertThat(clearedCookie.getMaxAge().getSeconds()).isEqualTo(0);
        }
    }

    // ==================== HELPER METHODS ====================

    private WebTestClient.ResponseSpec refreshTokenRequest(ResponseCookie refreshTokenCookie) {
        return webTestClient.post()
                .uri("/api/v1/bff/auth/refresh")
                .cookie(refreshTokenCookie.getName(), refreshTokenCookie.getValue())
                .exchange();
    }
}
```

---

## Dependencies

Add JWT parsing library to `pom.xml`:

```xml
<dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
    <version>9.37.3</version>
    <scope>test</scope>
</dependency>
```

---

## Testing Checklist

- [ ] Login succeeds with valid credentials
- [ ] Login fails with invalid username
- [ ] Login fails with invalid password
- [ ] Login fails with missing fields
- [ ] Access token extracted from response body
- [ ] Refresh token extracted from httpOnly cookie
- [ ] Refresh token has correct security flags (httpOnly, SameSite, Secure)
- [ ] Token refresh succeeds with valid cookie
- [ ] Token refresh fails with missing cookie
- [ ] Token refresh fails with invalid cookie
- [ ] Token rotation: old refresh token invalidated after refresh
- [ ] Concurrent refresh requests prevent race condition
- [ ] Access token contains valid JWT claims
- [ ] JWT signature is valid
- [ ] JWT contains correct user roles
- [ ] JWT contains correct tenant context
- [ ] Logout clears refresh token cookie
- [ ] User context endpoint returns correct data
- [ ] User context fails without token
- [ ] Correlation ID generated for each request
- [ ] CORS headers present on auth endpoints
- [ ] Rate limiting enforced on login endpoint

---

## Next Steps

1. **Implement AuthenticationTest** with all test scenarios
2. **Add JWT parsing library** (Nimbus JOSE JWT)
3. **Test token expiration** with mocked time
4. **Test concurrent refresh** with thread pool
5. **Validate cookie security** flags in different environments
6. **Document token rotation behavior**
7. **Test Keycloak integration** for JWT validation

---

## Notes

- **Token Storage**: Access token in memory (lost on refresh), refresh token in httpOnly cookie (persists)
- **Token Rotation**: Refresh endpoint returns new refresh token, invalidates old one
- **Race Condition**: Frontend prevents multiple simultaneous refreshes with shared promise
- **Correlation ID**: Auto-generated UUID for request tracing
- **Rate Limiting**: BFF endpoints limited to 500 req/min
- **HTTPS**: Secure flag only set on HTTPS connections
- **Keycloak**: JWT validation via Keycloak JWK set URI
