package com.ccbsa.wms.gateway.api;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.fixture.TestData;
import com.ccbsa.wms.gateway.api.fixture.UserTestDataBuilder;
import com.ccbsa.wms.gateway.api.helper.AuthenticationHelper;
import com.ccbsa.wms.gateway.api.util.WebTestClientConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Base class for integration tests.
 *
 * <p>Provides:
 * <ul>
 *   <li>Common test setup (WebTestClient, ObjectMapper, AuthHelper)</li>
 *   <li>Authentication and access token management</li>
 *   <li>TestData singleton access</li>
 *   <li>UserTestDataBuilder factory method</li>
 * </ul>
 *
 * <p>Subclasses can override {@link #setUp()} to add custom setup logic,
 * but must call {@code super.setUp()} first.
 */
@Slf4j
public abstract class BaseIntegrationTest {

    protected static final String BASE_URL =
            System.getenv().getOrDefault("GATEWAY_BASE_URL", "https://localhost:8080/api/v1");
    protected static final String TEST_USERNAME =
            System.getenv().getOrDefault("TEST_USERNAME", "admin");
    protected static final String TEST_PASSWORD =
            System.getenv().getOrDefault("TEST_PASSWORD", "admin");

    protected WebTestClient webTestClient;
    protected AuthenticationHelper authHelper;
    protected ObjectMapper objectMapper;
    protected String accessToken;
    protected TestData testData;

    @BeforeEach
    void setUp() {
        // Initialize WebTestClient
        webTestClient = WebTestClientConfig.createWebTestClient(BASE_URL);

        // Initialize ObjectMapper
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Initialize AuthenticationHelper
        authHelper = new AuthenticationHelper(webTestClient, objectMapper);

        // Login to get access token
        AuthenticationResult authResult = authHelper.login(TEST_USERNAME, TEST_PASSWORD);
        accessToken = authResult.getAccessToken();

        // Get TestData instance
        testData = TestData.getInstance();

        log.debug("Test setup complete. Using tenant ID: {}", testData.getTestTenantId());
    }

    /**
     * Creates a user in ACTIVE status.
     * Convenience method for common use case.
     *
     * @return User ID
     */
    protected String createActiveUser() {
        return userBuilder()
                .withStatus(UserTestDataBuilder.UserStatus.ACTIVE)
                .build();
    }

    /**
     * Creates a UserTestDataBuilder instance with common dependencies pre-configured.
     *
     * @return UserTestDataBuilder ready to use
     */
    protected UserTestDataBuilder userBuilder() {
        return UserTestDataBuilder.builder()
                .withWebTestClient(webTestClient)
                .withAuthHelper(authHelper)
                .withAccessToken(accessToken)
                .withObjectMapper(objectMapper)
                .create();
    }

    /**
     * Creates a user in INACTIVE status.
     * Convenience method for common use case.
     *
     * @return User ID
     */
    protected String createInactiveUser() {
        return userBuilder()
                .withStatus(UserTestDataBuilder.UserStatus.INACTIVE)
                .build();
    }

    /**
     * Creates a user in SUSPENDED status.
     * Convenience method for common use case.
     *
     * @return User ID
     */
    protected String createSuspendedUser() {
        return userBuilder()
                .withStatus(UserTestDataBuilder.UserStatus.SUSPENDED)
                .build();
    }
}
