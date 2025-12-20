package com.ccbsa.common.application.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Test utilities for ApiResponse testing.
 *
 * <p>Provides helper methods for creating test data and assertions
 * in unit and integration tests.</p>
 */
public final class ApiResponseTestUtils {
    private ApiResponseTestUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Asserts that a ResponseEntity contains a successful ApiResponse.
     *
     * @param <T>            The type of data
     * @param response       The ResponseEntity to assert
     * @param expectedStatus The expected HTTP status
     * @param expectedData   The expected data (can be null)
     */
    public static <T> void assertSuccessResponse(ResponseEntity<ApiResponse<T>> response, HttpStatus expectedStatus, T expectedData) {
        assert response != null : "Response cannot be null";
        assert response.getStatusCode() == expectedStatus : String.format("Expected status %s but got %s", expectedStatus, response.getStatusCode());
        ApiResponse<T> body = response.getBody();
        assert body != null : "Response body cannot be null";
        if (body != null) {
            assert body.isSuccess() : "Response should be successful";
            assert body.isError() == false : "Response should not be an error";

            if (expectedData != null) {
                assert body.getData() != null && body.getData().equals(expectedData) : String.format("Expected data %s but got %s", expectedData, body.getData());
            }
        }
    }

    /**
     * Asserts that a ResponseEntity contains an error ApiResponse.
     *
     * @param <T>               The type parameter
     * @param response          The ResponseEntity to assert
     * @param expectedStatus    The expected HTTP status
     * @param expectedErrorCode The expected error code
     */
    public static <T> void assertErrorResponse(ResponseEntity<ApiResponse<T>> response, HttpStatus expectedStatus, String expectedErrorCode) {
        assert response != null : "Response cannot be null";
        assert response.getStatusCode() == expectedStatus : String.format("Expected status %s but got %s", expectedStatus, response.getStatusCode());
        ApiResponse<T> body = response.getBody();
        assert body != null : "Response body cannot be null";
        if (body != null) {
            assert body.isError() : "Response should be an error";
            ApiError error = body.getError();
            assert error != null : "Error should not be null";
            if (error != null) {
                assert error.getCode() != null && error.getCode().equals(expectedErrorCode) :
                        String.format("Expected error code %s but got %s", expectedErrorCode, error.getCode());
            }
        }
    }

    /**
     * Creates a test ApiError with minimal required fields.
     *
     * @param code    The error code
     * @param message The error message
     * @return ApiError instance
     */
    public static ApiError createTestError(String code, String message) {
        return ApiError.builder(code, message).build();
    }

    /**
     * Creates a test ApiError with all fields.
     *
     * @param code      The error code
     * @param message   The error message
     * @param path      The request path
     * @param requestId The request ID
     * @param details   The error details
     * @return ApiError instance
     */
    public static ApiError createTestError(String code, String message, String path, String requestId, Map<String, Object> details) {
        return ApiError.builder(code, message).path(path).requestId(requestId).details(details).build();
    }

    /**
     * Creates test links for HATEOAS.
     *
     * @param self       The self link
     * @param additional Additional links (key-value pairs)
     * @return Map of links
     */
    @SafeVarargs
    public static Map<String, String> createTestLinks(String self, Map.Entry<String, String>... additional) {
        Map<String, String> links = new HashMap<>();
        links.put("self", self);
        if (additional != null) {
            for (Map.Entry<String, String> entry : additional) {
                links.put(entry.getKey(), entry.getValue());
            }
        }
        return links;
    }

    /**
     * Creates test pagination metadata.
     *
     * @param page          The page number
     * @param size          The page size
     * @param totalElements The total number of elements
     * @return ApiMeta with pagination
     */
    public static ApiMeta createTestPaginationMeta(int page, int size, long totalElements) {
        ApiMeta.Pagination pagination = ApiMeta.Pagination.of(page, size, totalElements);
        return ApiMeta.builder().pagination(pagination).build();
    }
}

