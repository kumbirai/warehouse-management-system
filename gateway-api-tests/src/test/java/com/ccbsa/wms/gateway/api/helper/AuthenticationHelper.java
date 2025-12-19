package com.ccbsa.wms.gateway.api.helper;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.LoginRequest;
import com.ccbsa.wms.gateway.api.dto.LoginResponse;
import com.ccbsa.wms.gateway.api.util.CookieExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper class for authentication operations in integration tests.
 */
public class AuthenticationHelper {

    private static final String LOGIN_ENDPOINT = "/api/v1/bff/auth/login";
    private static final String REFRESH_ENDPOINT = "/api/v1/bff/auth/refresh";
    private static final String LOGOUT_ENDPOINT = "/api/v1/bff/auth/logout";
    private final WebTestClient webTestClient;
    private final ObjectMapper objectMapper;

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

        // Extract ApiResponse using ParameterizedTypeReference for proper generic type handling
        // This ensures correct deserialization of ApiResponse<LoginResponse>
        EntityExchangeResult<ApiResponse<LoginResponse>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {
                })
                .returnResult();

        // Extract ApiResponse from the exchange result
        ApiResponse<LoginResponse> apiResponse = exchangeResult.getResponseBody();

        assertThat(apiResponse)
                .as("API response should not be null. Status: %d, Response body: %s",
                        exchangeResult.getStatus().value(),
                        exchangeResult.getResponseBodyContent() != null
                                ? new String(exchangeResult.getResponseBodyContent())
                                : "null")
                .isNotNull();
        assertThat(apiResponse.isSuccess())
                .as("API response should be successful. Error: %s",
                        apiResponse.getError() != null ? apiResponse.getError().getMessage() : "none")
                .isTrue();

        LoginResponse loginResponse = apiResponse.getData();
        assertThat(loginResponse)
                .as("Login response data should not be null")
                .isNotNull();
        assertThat(loginResponse.getAccessToken())
                .as("Access token should not be blank")
                .isNotBlank();
        assertThat(loginResponse.getUserContext())
                .as("User context should not be null")
                .isNotNull();

        // Extract refresh token from cookie
        HttpHeaders headers = exchangeResult.getResponseHeaders();
        ResponseCookie refreshTokenCookie = CookieExtractor.extractRefreshTokenCookie(headers);

        assertThat(refreshTokenCookie).isNotNull();
        assertThat(refreshTokenCookie.getValue()).isNotBlank();

        return AuthenticationResult.builder()
                .accessToken(loginResponse.getAccessToken())
                .refreshTokenCookie(refreshTokenCookie)
                .userContext(loginResponse.getUserContext())
                .expiresIn(loginResponse.getExpiresIn() != null ? loginResponse.getExpiresIn().longValue() : null)
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

        // Extract ApiResponse using ParameterizedTypeReference for proper generic type handling
        EntityExchangeResult<ApiResponse<LoginResponse>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {
                })
                .returnResult();

        // Extract ApiResponse from the exchange result
        ApiResponse<LoginResponse> apiResponse = exchangeResult.getResponseBody();

        assertThat(apiResponse)
                .as("API response should not be null. Status: %d, Response body: %s",
                        exchangeResult.getStatus().value(),
                        exchangeResult.getResponseBodyContent() != null
                                ? new String(exchangeResult.getResponseBodyContent())
                                : "null")
                .isNotNull();
        assertThat(apiResponse.isSuccess())
                .as("API response should be successful. Error: %s",
                        apiResponse.getError() != null ? apiResponse.getError().getMessage() : "none")
                .isTrue();

        LoginResponse loginResponse = apiResponse.getData();
        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getAccessToken()).isNotBlank();

        // Extract new refresh token (token rotation)
        HttpHeaders headers = exchangeResult.getResponseHeaders();
        ResponseCookie newRefreshTokenCookie = CookieExtractor.extractRefreshTokenCookie(headers);

        return AuthenticationResult.builder()
                .accessToken(loginResponse.getAccessToken())
                .refreshTokenCookie(newRefreshTokenCookie)
                .userContext(loginResponse.getUserContext())
                .expiresIn(loginResponse.getExpiresIn() != null ? loginResponse.getExpiresIn().longValue() : null)
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

