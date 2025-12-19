# BaseIntegrationTest Implementation Plan

## Overview

`BaseIntegrationTest` serves as the foundational abstract base class for all gateway API integration tests. It handles common framework concerns, authentication patterns,
WebTestClient configuration, and reusable test utilities to avoid code duplication (DRY principle).

---

## Objectives

1. **Spring Boot Test Configuration**: Configure Spring Boot test context with proper profiles and properties
2. **WebTestClient Setup**: Configure reactive WebTestClient for API calls through the gateway
3. **Authentication Management**: Handle JWT token extraction, refresh token management, and authenticated requests
4. **Cookie Handling**: Extract and manage httpOnly cookies for refresh tokens
5. **Test Data Management**: Provide utilities for test data cleanup and isolation
6. **Common Assertions**: Reusable assertion methods for consistent test validation
7. **Environment Configuration**: Load test credentials and gateway URLs from configuration

---

## Technical Requirements

### Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- WebFlux WebTestClient for reactive testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ for fluent assertions -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Faker for test data generation -->
    <dependency>
        <groupId>net.datafaker</groupId>
        <artifactId>datafaker</artifactId>
        <version>2.1.0</version>
        <scope>test</scope>
    </dependency>

    <!-- Jackson for JSON processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

## Test Configuration

### Application Test Profile (application-test.yml)

Create `gateway-api-tests/src/test/resources/application-test.yml`:

```yaml
# Gateway Service Configuration
gateway:
  base-url: http://localhost:8080

# Test User Credentials
test:
  users:
    system-admin:
      username: sysadmin
      password: Password123@
    tenant-admin:
      username: ${TEST_TENANT_ADMIN_USERNAME:tenantadmin}
      password: ${TEST_TENANT_ADMIN_PASSWORD:Password123@}

# WebTestClient Configuration
webclient:
  timeout: 30000  # 30 seconds
  buffer-size: 16MB

# Test Data Configuration
test-data:
  cleanup-after-tests: true
  faker-locale: en-US

# Logging Configuration
logging:
  level:
    com.ccbsa.wms.gateway.api: DEBUG
    org.springframework.web.reactive.function.client: DEBUG
```

---

## Class Structure

### Package Structure

```
gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/
├── BaseIntegrationTest.java
├── config/
│   └── TestConfiguration.java
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── UserContext.java
│   └── AuthenticationResult.java
├── util/
│   ├── WebTestClientConfig.java
│   ├── RequestHeaderHelper.java
│   └── CookieExtractor.java
└── helper/
    └── AuthenticationHelper.java
```

---

## Implementation Details

### 1. BaseIntegrationTest Abstract Class

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.LoginRequest;
import com.ccbsa.wms.gateway.api.dto.LoginResponse;
import com.ccbsa.wms.gateway.api.helper.AuthenticationHelper;
import com.ccbsa.wms.gateway.api.util.CookieExtractor;
import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;
import com.ccbsa.wms.gateway.api.util.WebTestClientConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base integration test class providing common test infrastructure
 * for gateway API integration tests.
 *
 * Features:
 * - WebTestClient configuration for reactive API testing
 * - JWT token management (access + refresh tokens)
 * - Cookie handling for httpOnly refresh tokens
 * - Authentication helpers for login/logout flows
 * - Faker instance for test data generation
 * - Common assertion utilities
 * - Request header management (correlation ID, tenant context)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected WebTestClient webTestClient;
    protected ObjectMapper objectMapper;
    protected Faker faker;
    protected AuthenticationHelper authHelper;

    @Value("${gateway.base-url}")
    protected String gatewayBaseUrl;

    @Value("${test.users.system-admin.username}")
    protected String systemAdminUsername;

    @Value("${test.users.system-admin.password}")
    protected String systemAdminPassword;

    @Value("${test.users.tenant-admin.username}")
    protected String tenantAdminUsername;

    @Value("${test.users.tenant-admin.password}")
    protected String tenantAdminPassword;

    /**
     * Setup method executed before each test.
     * Initializes WebTestClient, ObjectMapper, Faker, and helper classes.
     */
    @BeforeEach
    public void setUp() {
        this.webTestClient = WebTestClientConfig.createWebTestClient(gatewayBaseUrl);
        this.objectMapper = new ObjectMapper();
        this.faker = new Faker();
        this.authHelper = new AuthenticationHelper(webTestClient, objectMapper);
    }

    // ==================== AUTHENTICATION HELPERS ====================

    /**
     * Login as SYSTEM_ADMIN and return authentication result with tokens.
     *
     * @return AuthenticationResult containing access token, refresh token cookie, and user context
     */
    protected AuthenticationResult loginAsSystemAdmin() {
        return authHelper.login(systemAdminUsername, systemAdminPassword);
    }

    /**
     * Login as TENANT_ADMIN and return authentication result with tokens.
     *
     * @return AuthenticationResult containing access token, refresh token cookie, and user context
     */
    protected AuthenticationResult loginAsTenantAdmin() {
        return authHelper.login(tenantAdminUsername, tenantAdminPassword);
    }

    /**
     * Login with custom credentials.
     *
     * @param username the username
     * @param password the password
     * @return AuthenticationResult containing access token, refresh token cookie, and user context
     */
    protected AuthenticationResult login(String username, String password) {
        return authHelper.login(username, password);
    }

    /**
     * Logout and clear authentication tokens.
     *
     * @param refreshTokenCookie the refresh token cookie to invalidate
     */
    protected void logout(ResponseCookie refreshTokenCookie) {
        authHelper.logout(refreshTokenCookie);
    }

    /**
     * Refresh access token using refresh token cookie.
     *
     * @param refreshTokenCookie the refresh token cookie
     * @return AuthenticationResult with new tokens
     */
    protected AuthenticationResult refreshToken(ResponseCookie refreshTokenCookie) {
        return authHelper.refreshToken(refreshTokenCookie);
    }

    // ==================== REQUEST BUILDERS ====================

    /**
     * Create authenticated GET request with Bearer token.
     *
     * @param uri the request URI
     * @param accessToken the JWT access token
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedGet(String uri, String accessToken) {
        return webTestClient.get()
                .uri(uri)
                .headers(headers -> RequestHeaderHelper.addAuthHeaders(headers, accessToken));
    }

    /**
     * Create authenticated GET request with Bearer token and tenant context.
     *
     * @param uri the request URI
     * @param accessToken the JWT access token
     * @param tenantId the tenant ID for X-Tenant-Id header
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedGet(String uri, String accessToken, String tenantId) {
        return webTestClient.get()
                .uri(uri)
                .headers(headers -> {
                    RequestHeaderHelper.addAuthHeaders(headers, accessToken);
                    RequestHeaderHelper.addTenantHeader(headers, tenantId);
                });
    }

    /**
     * Create authenticated POST request with Bearer token.
     *
     * @param uri the request URI
     * @param accessToken the JWT access token
     * @param requestBody the request body
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedPost(String uri, String accessToken, Object requestBody) {
        return webTestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> RequestHeaderHelper.addAuthHeaders(headers, accessToken))
                .bodyValue(requestBody);
    }

    /**
     * Create authenticated POST request with Bearer token and tenant context.
     *
     * @param uri the request URI
     * @param accessToken the JWT access token
     * @param tenantId the tenant ID for X-Tenant-Id header
     * @param requestBody the request body
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedPost(String uri, String accessToken, String tenantId, Object requestBody) {
        return webTestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    RequestHeaderHelper.addAuthHeaders(headers, accessToken);
                    RequestHeaderHelper.addTenantHeader(headers, tenantId);
                })
                .bodyValue(requestBody);
    }

    /**
     * Create authenticated PUT request with Bearer token.
     *
     * @param uri the request URI
     * @param accessToken the JWT access token
     * @param requestBody the request body (optional)
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedPut(String uri, String accessToken, Object requestBody) {
        WebTestClient.RequestBodySpec spec = webTestClient.put()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> RequestHeaderHelper.addAuthHeaders(headers, accessToken));

        if (requestBody != null) {
            return spec.bodyValue(requestBody);
        }
        return spec;
    }

    /**
     * Create authenticated DELETE request with Bearer token.
     *
     * @param uri the request URI
     * @param accessToken the JWT access token
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedDelete(String uri, String accessToken) {
        return webTestClient.delete()
                .uri(uri)
                .headers(headers -> RequestHeaderHelper.addAuthHeaders(headers, accessToken));
    }

    // ==================== COOKIE HELPERS ====================

    /**
     * Extract refresh token cookie from response headers.
     *
     * @param headers the response headers
     * @return ResponseCookie containing the refresh token
     */
    protected ResponseCookie extractRefreshTokenCookie(HttpHeaders headers) {
        return CookieExtractor.extractRefreshTokenCookie(headers);
    }

    /**
     * Validate refresh token cookie properties.
     *
     * @param cookie the cookie to validate
     */
    protected void assertRefreshTokenCookieValid(ResponseCookie cookie) {
        assertThat(cookie).isNotNull();
        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getMaxAge().getSeconds()).isGreaterThan(0);
    }

    // ==================== COMMON ASSERTIONS ====================

    /**
     * Assert successful response (2xx status code).
     *
     * @param spec the response spec
     */
    protected void assertSuccessResponse(WebTestClient.ResponseSpec spec) {
        spec.expectStatus().is2xxSuccessful();
    }

    /**
     * Assert unauthorized response (401 status code).
     *
     * @param spec the response spec
     */
    protected void assertUnauthorized(WebTestClient.ResponseSpec spec) {
        spec.expectStatus().isUnauthorized();
    }

    /**
     * Assert forbidden response (403 status code).
     *
     * @param spec the response spec
     */
    protected void assertForbidden(WebTestClient.ResponseSpec spec) {
        spec.expectStatus().isForbidden();
    }

    /**
     * Assert bad request response (400 status code).
     *
     * @param spec the response spec
     */
    protected void assertBadRequest(WebTestClient.ResponseSpec spec) {
        spec.expectStatus().isBadRequest();
    }

    /**
     * Assert not found response (404 status code).
     *
     * @param spec the response spec
     */
    protected void assertNotFound(WebTestClient.ResponseSpec spec) {
        spec.expectStatus().isNotFound();
    }

    // ==================== FAKER HELPERS ====================

    /**
     * Generate random email address.
     *
     * @return random email
     */
    protected String randomEmail() {
        return faker.internet().emailAddress();
    }

    /**
     * Generate random username.
     *
     * @return random username
     */
    protected String randomUsername() {
        return faker.name().username();
    }

    /**
     * Generate random first name.
     *
     * @return random first name
     */
    protected String randomFirstName() {
        return faker.name().firstName();
    }

    /**
     * Generate random last name.
     *
     * @return random last name
     */
    protected String randomLastName() {
        return faker.name().lastName();
    }

    /**
     * Generate random company name.
     *
     * @return random company name
     */
    protected String randomCompanyName() {
        return faker.company().name();
    }

    /**
     * Generate random UUID string.
     *
     * @return random UUID
     */
    protected String randomUUID() {
        return UUID.randomUUID().toString();
    }

    // ==================== CORRELATION ID HELPERS ====================

    /**
     * Generate correlation ID for request tracing.
     *
     * @return correlation ID
     */
    protected String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Validate correlation ID is present in response headers.
     *
     * @param headers response headers
     * @param expectedCorrelationId expected correlation ID
     */
    protected void assertCorrelationIdPresent(HttpHeaders headers, String expectedCorrelationId) {
        String correlationId = headers.getFirst("X-Correlation-Id");
        assertThat(correlationId).isEqualTo(expectedCorrelationId);
    }
}
```

---

### 2. AuthenticationHelper Class

Create `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/helper/AuthenticationHelper.java`:

```java
package com.ccbsa.wms.gateway.api.helper;

import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.LoginRequest;
import com.ccbsa.wms.gateway.api.dto.LoginResponse;
import com.ccbsa.wms.gateway.api.util.CookieExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper class for authentication operations in integration tests.
 */
public class AuthenticationHelper {

    private final WebTestClient webTestClient;
    private final ObjectMapper objectMapper;

    private static final String LOGIN_ENDPOINT = "/api/v1/bff/auth/login";
    private static final String REFRESH_ENDPOINT = "/api/v1/bff/auth/refresh";
    private static final String LOGOUT_ENDPOINT = "/api/v1/bff/auth/logout";

    public AuthenticationHelper(WebTestClient webTestClient, ObjectMapper objectMapper) {
        this.webTestClient = webTestClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Login with username and password.
     *
     * @param username the username
     * @param password the password
     * @return AuthenticationResult with tokens and user context
     */
    public AuthenticationResult login(String username, String password) {
        LoginRequest loginRequest = LoginRequest.builder()
                .username(username)
                .password(password)
                .build();

        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange();

        response.expectStatus().isOk();

        // Extract access token from response body
        LoginResponse loginResponse = response.expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getAccessToken()).isNotBlank();
        assertThat(loginResponse.getUserContext()).isNotNull();

        // Extract refresh token from cookie
        HttpHeaders headers = response.returnResult(LoginResponse.class).getResponseHeaders();
        ResponseCookie refreshTokenCookie = CookieExtractor.extractRefreshTokenCookie(headers);

        assertThat(refreshTokenCookie).isNotNull();
        assertThat(refreshTokenCookie.getValue()).isNotBlank();

        return AuthenticationResult.builder()
                .accessToken(loginResponse.getAccessToken())
                .refreshTokenCookie(refreshTokenCookie)
                .userContext(loginResponse.getUserContext())
                .expiresIn(loginResponse.getExpiresIn())
                .build();
    }

    /**
     * Refresh access token using refresh token cookie.
     *
     * @param refreshTokenCookie the refresh token cookie
     * @return AuthenticationResult with new tokens
     */
    public AuthenticationResult refreshToken(ResponseCookie refreshTokenCookie) {
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri(REFRESH_ENDPOINT)
                .cookie(refreshTokenCookie.getName(), refreshTokenCookie.getValue())
                .exchange();

        response.expectStatus().isOk();

        // Extract new access token
        LoginResponse loginResponse = response.expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getAccessToken()).isNotBlank();

        // Extract new refresh token (token rotation)
        HttpHeaders headers = response.returnResult(LoginResponse.class).getResponseHeaders();
        ResponseCookie newRefreshTokenCookie = CookieExtractor.extractRefreshTokenCookie(headers);

        return AuthenticationResult.builder()
                .accessToken(loginResponse.getAccessToken())
                .refreshTokenCookie(newRefreshTokenCookie)
                .userContext(loginResponse.getUserContext())
                .expiresIn(loginResponse.getExpiresIn())
                .build();
    }

    /**
     * Logout and clear tokens.
     *
     * @param refreshTokenCookie the refresh token cookie to invalidate
     */
    public void logout(ResponseCookie refreshTokenCookie) {
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri(LOGOUT_ENDPOINT)
                .cookie(refreshTokenCookie.getName(), refreshTokenCookie.getValue())
                .exchange();

        response.expectStatus().isOk();

        // Verify refresh token cookie is cleared
        HttpHeaders headers = response.returnResult(Void.class).getResponseHeaders();
        ResponseCookie clearedCookie = CookieExtractor.extractRefreshTokenCookie(headers);

        // Cookie should be present but with maxAge=0 or empty value
        if (clearedCookie != null) {
            assertThat(clearedCookie.getMaxAge().getSeconds()).isEqualTo(0);
        }
    }
}
```

---

### 3. Utility Classes

#### WebTestClientConfig

Create `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/util/WebTestClientConfig.java`:

```java
package com.ccbsa.wms.gateway.api.util;

import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

/**
 * Configuration for WebTestClient instances.
 */
public class WebTestClientConfig {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024 * 1024; // 16MB

    /**
     * Create WebTestClient with default configuration.
     *
     * @param baseUrl the gateway base URL
     * @return configured WebTestClient
     */
    public static WebTestClient createWebTestClient(String baseUrl) {
        return WebTestClient.bindToServer()
                .baseUrl(baseUrl)
                .responseTimeout(DEFAULT_TIMEOUT)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_BUFFER_SIZE))
                .build();
    }

    /**
     * Create WebTestClient with custom timeout.
     *
     * @param baseUrl the gateway base URL
     * @param timeout the timeout duration
     * @return configured WebTestClient
     */
    public static WebTestClient createWebTestClient(String baseUrl, Duration timeout) {
        return WebTestClient.bindToServer()
                .baseUrl(baseUrl)
                .responseTimeout(timeout)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_BUFFER_SIZE))
                .build();
    }
}
```

#### RequestHeaderHelper

Create `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/util/RequestHeaderHelper.java`:

```java
package com.ccbsa.wms.gateway.api.util;

import org.springframework.http.HttpHeaders;

import java.util.UUID;

/**
 * Helper for adding common request headers.
 */
public class RequestHeaderHelper {

    /**
     * Add authorization header with Bearer token.
     *
     * @param headers the HttpHeaders
     * @param accessToken the JWT access token
     */
    public static void addAuthHeaders(HttpHeaders headers, String accessToken) {
        headers.setBearerAuth(accessToken);
        headers.set("X-Correlation-Id", UUID.randomUUID().toString());
    }

    /**
     * Add tenant context header.
     *
     * @param headers the HttpHeaders
     * @param tenantId the tenant ID
     */
    public static void addTenantHeader(HttpHeaders headers, String tenantId) {
        headers.set("X-Tenant-Id", tenantId);
    }

    /**
     * Add correlation ID header.
     *
     * @param headers the HttpHeaders
     * @param correlationId the correlation ID
     */
    public static void addCorrelationIdHeader(HttpHeaders headers, String correlationId) {
        headers.set("X-Correlation-Id", correlationId);
    }
}
```

#### CookieExtractor

Create `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/util/CookieExtractor.java`:

```java
package com.ccbsa.wms.gateway.api.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * Utility for extracting cookies from HTTP response headers.
 */
public class CookieExtractor {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    /**
     * Extract refresh token cookie from response headers.
     *
     * @param headers the response headers
     * @return ResponseCookie or null if not found
     */
    public static ResponseCookie extractRefreshTokenCookie(HttpHeaders headers) {
        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }

        for (String cookieHeader : cookies) {
            ResponseCookie cookie = ResponseCookie.parse(cookieHeader);
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie;
            }
        }

        return null;
    }

    /**
     * Extract cookie by name from response headers.
     *
     * @param headers the response headers
     * @param cookieName the cookie name
     * @return ResponseCookie or null if not found
     */
    public static ResponseCookie extractCookie(HttpHeaders headers, String cookieName) {
        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }

        for (String cookieHeader : cookies) {
            ResponseCookie cookie = ResponseCookie.parse(cookieHeader);
            if (cookieName.equals(cookie.getName())) {
                return cookie;
            }
        }

        return null;
    }
}
```

---

### 4. DTO Classes

#### LoginRequest

Create `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/dto/LoginRequest.java`:

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
public class LoginRequest {
    private String username;
    private String password;
}
```

#### LoginResponse

Create `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/dto/LoginResponse.java`:

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
public class LoginResponse {
    private String accessToken;
    private UserContext userContext;
    private Long expiresIn;
}
```

#### UserContext

Create `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/dto/UserContext.java`:

```java
package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String tenantId;
    private List<String> roles;
}
```

#### AuthenticationResult

Create `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/dto/AuthenticationResult.java`:

```java
package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseCookie;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResult {
    private String accessToken;
    private ResponseCookie refreshTokenCookie;
    private UserContext userContext;
    private Long expiresIn;

    /**
     * Get tenant ID from user context.
     *
     * @return tenant ID
     */
    public String getTenantId() {
        return userContext != null ? userContext.getTenantId() : null;
    }

    /**
     * Check if user has specific role.
     *
     * @param role the role to check
     * @return true if user has role
     */
    public boolean hasRole(String role) {
        return userContext != null &&
               userContext.getRoles() != null &&
               userContext.getRoles().contains(role);
    }
}
```

---

## Usage Example

### Example Test Class Using BaseIntegrationTest

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import org.junit.jupiter.api.Test;

public class ExampleIntegrationTest extends BaseIntegrationTest {

    @Test
    public void testLoginAsSystemAdmin() {
        // Login as system admin
        AuthenticationResult auth = loginAsSystemAdmin();

        // Verify tokens received
        assertThat(auth.getAccessToken()).isNotBlank();
        assertThat(auth.getRefreshTokenCookie()).isNotNull();
        assertThat(auth.getUserContext().getUsername()).isEqualTo(systemAdminUsername);

        // Make authenticated request
        authenticatedGet("/api/v1/tenants", auth.getAccessToken())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testTokenRefresh() {
        // Login
        AuthenticationResult auth = loginAsSystemAdmin();

        // Refresh token
        AuthenticationResult refreshedAuth = refreshToken(auth.getRefreshTokenCookie());

        // Verify new token received
        assertThat(refreshedAuth.getAccessToken()).isNotBlank();
        assertThat(refreshedAuth.getAccessToken()).isNotEqualTo(auth.getAccessToken());
    }
}
```

---

## Testing Checklist

- [ ] WebTestClient configured with proper timeout and buffer size
- [ ] Authentication helper methods work for login, logout, refresh
- [ ] Refresh token extracted from httpOnly cookies correctly
- [ ] Access token extracted from response body
- [ ] Authenticated request builders add proper headers (Authorization, X-Correlation-Id)
- [ ] Tenant context header added when required
- [ ] Faker instance generates realistic test data
- [ ] Common assertion methods work correctly
- [ ] Cookie validation methods verify httpOnly, SameSite, Secure flags
- [ ] Test configuration loads credentials from application-test.yml
- [ ] Environment variables override for TEST_TENANT_ADMIN_USERNAME

---

## Next Steps

1. **Implement BaseIntegrationTest** with all helper methods
2. **Create utility classes** (WebTestClientConfig, RequestHeaderHelper, CookieExtractor)
3. **Create DTO classes** (LoginRequest, LoginResponse, UserContext, AuthenticationResult)
4. **Write unit tests** for utility classes
5. **Validate authentication flow** with simple integration test
6. **Document usage patterns** for other test classes

---

## Notes

- **Thread Safety**: BaseIntegrationTest creates new instances for each test method (@BeforeEach)
- **Cookie Handling**: ResponseCookie from Spring Framework used for cookie parsing
- **Token Rotation**: Refresh token endpoint returns new refresh token (security best practice)
- **Error Handling**: WebTestClient throws exceptions on unexpected status codes (use expectStatus() to validate)
- **Faker Locale**: Configured to en-US for consistent test data generation
- **Correlation ID**: Auto-generated UUID for request tracing in all authenticated requests
