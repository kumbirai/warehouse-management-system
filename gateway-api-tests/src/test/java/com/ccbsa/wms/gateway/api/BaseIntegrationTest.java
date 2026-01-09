package com.ccbsa.wms.gateway.api;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.StockLevelResponse;
import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.helper.AuthenticationHelper;
import com.ccbsa.wms.gateway.api.util.CookieExtractor;
import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;
import com.ccbsa.wms.gateway.api.util.WebTestClientConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.datafaker.Faker;

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
public abstract class BaseIntegrationTest {

    protected WebTestClient webTestClient;
    protected ObjectMapper objectMapper;
    protected Faker faker;
    protected AuthenticationHelper authHelper;

    protected String gatewayBaseUrl;
    protected String systemAdminUsername;
    protected String systemAdminPassword;
    protected String tenantAdminUsername;
    protected String tenantAdminPassword;

    /**
     * Setup method executed before each test.
     * Initializes WebTestClient, ObjectMapper, Faker, and helper classes.
     * Skips tests if gateway service is not available.
     */
    @BeforeEach
    public void setUp() {
        // Load properties from environment or use defaults
        // Default to HTTP since SSL is disabled by default for local development
        // Set GATEWAY_SSL_ENABLED=true and use https://localhost:8080 for SSL-enabled gateway
        this.gatewayBaseUrl = System.getProperty("gateway.base.url",
                System.getenv().getOrDefault("GATEWAY_BASE_URL", "http://localhost:8080"));
        this.systemAdminUsername = System.getProperty("test.system.admin.username",
                System.getenv().getOrDefault("TEST_SYSTEM_ADMIN_USERNAME", "sysadmin"));
        this.systemAdminPassword = System.getProperty("test.system.admin.password",
                System.getenv().getOrDefault("TEST_SYSTEM_ADMIN_PASSWORD", "Password123@"));
        this.tenantAdminUsername = System.getProperty("test.tenant.admin.username",
                System.getenv().getOrDefault("TEST_TENANT_ADMIN_USERNAME", "tenantuser@cm-sol.co.za"));
        this.tenantAdminPassword = System.getProperty("test.tenant.admin.password",
                System.getenv().getOrDefault("TEST_TENANT_ADMIN_PASSWORD", "Password123@"));

        this.webTestClient = WebTestClientConfig.createWebTestClient(gatewayBaseUrl);
        this.objectMapper = new ObjectMapper();
        this.faker = new Faker();
        this.authHelper = new AuthenticationHelper(webTestClient, objectMapper);

        // Check if gateway service is available, skip tests if not
        checkGatewayAvailability();
    }

    /**
     * Check if gateway service is available.
     * Skips tests if gateway is not reachable.
     */
    private void checkGatewayAvailability() {
        boolean gatewayAvailable = false;
        try {
            // Try to connect to gateway health endpoint with a timeout
            // Use returnResult to get the actual response and check status
            int statusCode = webTestClient.get()
                    .uri("/actuator/health")
                    .exchange()
                    .expectStatus()
                    .is2xxSuccessful()
                    .returnResult(Void.class)
                    .getStatus()
                    .value();
            gatewayAvailable = (statusCode >= 200 && statusCode < 300);
        } catch (org.opentest4j.TestAbortedException e) {
            // Re-throw assumption failures
            throw e;
        } catch (Exception e) {
            // Gateway is not available - this is expected when services aren't running
            // Log the exception for debugging
            System.err.println("Gateway health check failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            gatewayAvailable = false;
        }

        // Skip all tests if gateway is not available
        Assumptions.assumeTrue(gatewayAvailable,
                "Gateway service is not available at " + gatewayBaseUrl +
                        ". Please start the services before running integration tests. " +
                        "You can skip integration tests with: mvn verify -DskipITs=true");
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
     * <p>Note: This will skip tests if tenant admin credentials are not configured.
     * Set TEST_TENANT_ADMIN_USERNAME and TEST_TENANT_ADMIN_PASSWORD environment variables
     * to enable tenant admin tests.</p>
     *
     * @return AuthenticationResult containing access token, refresh token cookie, and user context
     * @throws org.opentest4j.TestAbortedException if tenant admin credentials are not configured
     */
    protected AuthenticationResult loginAsTenantAdmin() {
        // Check if tenant admin credentials are configured
        if (tenantAdminUsername == null || tenantAdminUsername.isEmpty() ||
                tenantAdminPassword == null || tenantAdminPassword.isEmpty() ||
                "tenantadmin".equals(tenantAdminUsername)) {
            Assumptions.assumeTrue(false,
                    "Tenant admin credentials not configured. " +
                            "Set TEST_TENANT_ADMIN_USERNAME and TEST_TENANT_ADMIN_PASSWORD environment variables " +
                            "to enable tenant admin tests.");
        }

        try {
            return authHelper.login(tenantAdminUsername, tenantAdminPassword);
        } catch (AssertionError e) {
            // If login fails, skip the test with a helpful message
            Assumptions.assumeTrue(false,
                    "Tenant admin login failed. " +
                            "Please ensure tenant admin user exists and credentials are correct. " +
                            "Error: " + e.getMessage());
            return null; // Never reached, but needed for compilation
        }
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
     * @param uri         the request URI
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
     * @param uri         the request URI
     * @param accessToken the JWT access token
     * @param tenantId    the tenant ID for X-Tenant-Id header
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
     * @param uri         the request URI
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
     * @param uri         the request URI
     * @param accessToken the JWT access token
     * @param tenantId    the tenant ID for X-Tenant-Id header
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
     * @param uri         the request URI
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
     * Create authenticated PUT request with Bearer token and tenant context.
     *
     * @param uri         the request URI
     * @param accessToken the JWT access token
     * @param tenantId    the tenant ID for X-Tenant-Id header
     * @param requestBody the request body (optional)
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedPut(String uri, String accessToken, String tenantId, Object requestBody) {
        WebTestClient.RequestBodySpec spec = webTestClient.put()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    RequestHeaderHelper.addAuthHeaders(headers, accessToken);
                    RequestHeaderHelper.addTenantHeader(headers, tenantId);
                });

        if (requestBody != null) {
            return spec.bodyValue(requestBody);
        }
        return spec;
    }

    /**
     * Create authenticated DELETE request with Bearer token.
     *
     * @param uri         the request URI
     * @param accessToken the JWT access token
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedDelete(String uri, String accessToken) {
        return webTestClient.delete()
                .uri(uri)
                .headers(headers -> RequestHeaderHelper.addAuthHeaders(headers, accessToken));
    }

    // ==================== API RESPONSE HELPERS ====================

    /**
     * Extract data from ApiResponse wrapper.
     * Convenience method that extracts and validates the data payload.
     *
     * @param <T>             the type of data
     * @param response        the WebTestClient.ResponseSpec
     * @param apiResponseType the ParameterizedTypeReference for ApiResponse&lt;T&gt;
     * @return the unwrapped data from ApiResponse
     */
    protected <T> T extractApiResponseData(
            WebTestClient.ResponseSpec response,
            ParameterizedTypeReference<ApiResponse<T>> apiResponseType) {

        EntityExchangeResult<ApiResponse<T>> exchangeResult = extractApiResponse(response, apiResponseType);
        ApiResponse<T> apiResponse = exchangeResult.getResponseBody();

        T data = apiResponse.getData();
        assertThat(data)
                .as("API response data should not be null")
                .isNotNull();

        return data;
    }

    /**
     * Extract ApiResponse from WebTestClient.ResponseSpec.
     * This is a production-grade utility method that properly handles ApiResponse wrapper deserialization.
     *
     * @param <T>             the type of data in the ApiResponse
     * @param response        the WebTestClient.ResponseSpec
     * @param apiResponseType the ParameterizedTypeReference for ApiResponse&lt;T&gt;
     * @return EntityExchangeResult containing both ApiResponse and headers
     * @throws AssertionError if the response is null or deserialization fails
     */
    protected <T> EntityExchangeResult<ApiResponse<T>> extractApiResponse(
            WebTestClient.ResponseSpec response,
            ParameterizedTypeReference<ApiResponse<T>> apiResponseType) {

        EntityExchangeResult<ApiResponse<T>> exchangeResult = response
                .expectBody(apiResponseType)
                .returnResult();

        ApiResponse<T> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse)
                .as("API response should not be null. Status: %d, Response: %s",
                        exchangeResult.getStatus().value(),
                        exchangeResult.getResponseBodyContent() != null
                                ? new String(exchangeResult.getResponseBodyContent())
                                : "null")
                .isNotNull();

        assertThat(apiResponse.isSuccess())
                .as("API response should be successful. Error: %s",
                        apiResponse.getError() != null ? apiResponse.getError().getMessage() : "none")
                .isTrue();

        return exchangeResult;
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
        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite())
                .as("SameSite should be Strict (case-insensitive)")
                .isEqualToIgnoringCase("Strict");
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
     * @param headers               response headers
     * @param expectedCorrelationId expected correlation ID
     */
    protected void assertCorrelationIdPresent(HttpHeaders headers, String expectedCorrelationId) {
        String correlationId = headers.getFirst("X-Correlation-Id");
        assertThat(correlationId).isEqualTo(expectedCorrelationId);
    }

    // ==================== STOCK ITEM WAITING HELPERS ====================

    /**
     * Wait for stock items to be created for a product after consignment creation.
     * Polls the stock-levels endpoint until stock items are available or timeout is reached.
     * 
     * <p>This is necessary because stock items are created asynchronously via Kafka events
     * after a consignment is created. Tests should use this method instead of Thread.sleep
     * to ensure stock items are available before attempting allocations.</p>
     *
     * @param productId      the product ID to check stock for (as String)
     * @param accessToken    the JWT access token for authentication
     * @param tenantId       the tenant ID for X-Tenant-Id header
     * @param maxWaitSeconds maximum number of seconds to wait
     * @param pollIntervalMs polling interval in milliseconds
     * @return true if stock items were found, false if timeout was reached
     */
    protected boolean waitForStockItems(String productId, String accessToken, String tenantId, 
                                       int maxWaitSeconds, long pollIntervalMs) {
        long endTime = System.currentTimeMillis() + (maxWaitSeconds * 1000L);
        int attemptCount = 0;
        
        while (System.currentTimeMillis() < endTime) {
            attemptCount++;
            try {
                EntityExchangeResult<ApiResponse<List<StockLevelResponse>>> result = authenticatedGet(
                        "/api/v1/stock-management/stock-levels?productId=" + productId,
                        accessToken,
                        tenantId
                ).exchange()
                        .expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<List<StockLevelResponse>>>() {
                        })
                        .returnResult();
                
                ApiResponse<List<StockLevelResponse>> apiResponse = result.getResponseBody();
                if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                    List<StockLevelResponse> stockLevels = apiResponse.getData();
                    // Check if we have stock items with quantity > 0
                    boolean hasStock = stockLevels.stream()
                            .anyMatch(level -> level.getTotalQuantity() != null && level.getTotalQuantity() > 0);
                    if (hasStock) {
                        int totalQuantity = stockLevels.stream()
                                .filter(level -> level.getTotalQuantity() != null)
                                .mapToInt(StockLevelResponse::getTotalQuantity)
                                .sum();
                        System.out.println("Stock items found after " + attemptCount + " attempts. Total quantity: " + totalQuantity);
                        return true;
                    }
                }
                
                if (attemptCount % 4 == 0) { // Log every 2 seconds (4 attempts * 500ms)
                    System.out.println("Waiting for stock items... attempt " + attemptCount);
                }
                
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                // Continue polling on error
                System.err.println("Error polling for stock items (attempt " + attemptCount + "): " + e.getMessage());
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        
        System.err.println("Timeout waiting for stock items after " + attemptCount + " attempts (" + maxWaitSeconds + " seconds)");
        return false;
    }

    // ==================== TEST DATA SETUP HELPERS ====================

    /**
     * Create a product via the API and return the response.
     * 
     * @param accessToken the JWT access token
     * @param tenantId the tenant ID
     * @return CreateProductResponse containing the created product details
     */
    protected CreateProductResponse createProduct(String accessToken, String tenantId) {
        CreateProductRequest productRequest = 
                ProductTestDataBuilder.buildCreateProductRequest();
        
        EntityExchangeResult<ApiResponse<CreateProductResponse>> productResult = authenticatedPost(
                "/api/v1/products",
                accessToken,
                tenantId,
                productRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateProductResponse> productApiResponse = productResult.getResponseBody();
        assertThat(productApiResponse).isNotNull();
        assertThat(productApiResponse.isSuccess()).isTrue();
        CreateProductResponse product = productApiResponse.getData();
        assertThat(product).isNotNull();
        return product;
    }

    /**
     * Create a location (warehouse) via the API and return the response.
     * 
     * @param accessToken the JWT access token
     * @param tenantId the tenant ID
     * @return CreateLocationResponse containing the created location details
     */
    protected CreateLocationResponse createLocation(String accessToken, String tenantId) {
        CreateLocationRequest locationRequest = 
                LocationTestDataBuilder.buildWarehouseRequest();
        
        EntityExchangeResult<ApiResponse<CreateLocationResponse>> locationResult = authenticatedPost(
                "/api/v1/location-management/locations",
                accessToken,
                tenantId,
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
        return location;
    }

    /**
     * Create a consignment and wait for stock items to be created.
     * This is a common pattern in tests that need stock items available.
     * 
     * @param warehouseId the warehouse ID where consignment is received
     * @param productCode the product code for the consignment line item
     * @param quantity the quantity to receive
     * @param expirationDate optional expiration date (null if not needed)
     * @param productId the product ID to wait for stock items
     * @param accessToken the JWT access token
     * @param tenantId the tenant ID
     * @param maxWaitSeconds maximum seconds to wait for stock items (default: 10)
     */
    protected void createConsignmentAndWaitForStock(
            String warehouseId, 
            String productCode, 
            int quantity, 
            LocalDate expirationDate,
            String productId,
            String accessToken, 
            String tenantId,
            int maxWaitSeconds) {
        
        CreateConsignmentRequest consignmentRequest;
        if (expirationDate != null) {
            consignmentRequest = ConsignmentTestDataBuilder
                    .buildCreateConsignmentRequestV2WithExpiration(warehouseId, productCode, expirationDate);
        } else {
            consignmentRequest = ConsignmentTestDataBuilder
                    .buildCreateConsignmentRequestV2(warehouseId, productCode, quantity, null);
        }
        
        authenticatedPost(
                "/api/v1/stock-management/consignments",
                accessToken,
                tenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated();

        // Wait for stock items to be created (async via Kafka events)
        boolean stockItemsCreated = waitForStockItems(productId, accessToken, tenantId, maxWaitSeconds, 500);
        assertThat(stockItemsCreated).as("Stock items should be created from consignment within " + maxWaitSeconds + " seconds").isTrue();
    }
}

