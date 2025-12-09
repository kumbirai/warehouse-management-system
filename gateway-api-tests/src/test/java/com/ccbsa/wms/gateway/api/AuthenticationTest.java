package com.ccbsa.wms.gateway.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;

import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for authentication endpoints.
 * Tests login, token refresh, get current user, and logout operations.
 */
@DisplayName("Authentication API Tests")
class AuthenticationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should login with valid credentials and get access token")
    void shouldLoginWithValidCredentials() {
        // When
        AuthenticationResult result = authHelper.login(TEST_USERNAME, TEST_PASSWORD);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isNotBlank();
        assertThat(result.getRefreshTokenCookie()).isNotBlank();
        assertThat(result.getUserContext()).isNotNull();
        assertThat(result.getUserContext().getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(result.getUserContext().getUserId()).isNotBlank();
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void shouldRejectLoginWithInvalidCredentials() {
        // When/Then
        webTestClient
                .post()
                .uri("/bff/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("""
                        {
                            "username": "invaliduser",
                            "password": "invalidpassword"
                        }
                        """))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should refresh access token using refresh token cookie")
    void shouldRefreshAccessToken() {
        // Given - Login first
        AuthenticationResult loginResult = authHelper.login(TEST_USERNAME, TEST_PASSWORD);
        String refreshTokenCookie = loginResult.getRefreshTokenCookie();

        // When - Refresh token
        AuthenticationResult refreshResult = authHelper.refreshToken(refreshTokenCookie);

        // Then
        assertThat(refreshResult).isNotNull();
        assertThat(refreshResult.getAccessToken()).isNotBlank();
        assertThat(refreshResult.getAccessToken()).isNotEqualTo(loginResult.getAccessToken()); // New token
        assertThat(refreshResult.getUserContext()).isNotNull();
        assertThat(refreshResult.getUserContext().getUsername()).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should get current user context with valid access token")
    void shouldGetCurrentUserContext() {
        // Given - Login first
        AuthenticationResult loginResult = authHelper.login(TEST_USERNAME, TEST_PASSWORD);
        String accessToken = loginResult.getAccessToken();

        // When - Get current user
        var userContext = authHelper.getCurrentUser(accessToken);

        // Then
        assertThat(userContext).isNotNull();
        assertThat(userContext.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(userContext.getUserId()).isNotBlank();
        assertThat(userContext.getRoles()).isNotNull();
    }

    @Test
    @DisplayName("Should reject get current user without access token")
    void shouldRejectGetCurrentUserWithoutToken() {
        // When/Then
        webTestClient
                .get()
                .uri("/bff/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() {
        // Given - Login first
        AuthenticationResult loginResult = authHelper.login(TEST_USERNAME, TEST_PASSWORD);

        // When - Logout
        authHelper.logout(loginResult.getAccessToken(), loginResult.getRefreshTokenCookie());

        // Then - Verify token is invalidated by trying to use it
        webTestClient
                .get()
                .uri("/bff/auth/me")
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", loginResult.getAccessToken()))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}

