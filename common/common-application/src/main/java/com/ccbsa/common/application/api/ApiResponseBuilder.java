package com.ccbsa.common.application.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for building standardized API responses.
 *
 * <p>Provides convenient methods for creating consistent ResponseEntity instances
 * with ApiResponse wrappers.</p>
 *
 * <p>Usage Examples:</p>
 * <pre>{@code
 * // Success response with data
 * return ApiResponseBuilder.ok(data);
 *
 * // Success response with data and links
 * return ApiResponseBuilder.ok(data, links);
 *
 * // Created response
 * return ApiResponseBuilder.created(data);
 *
 * // Error response
 * return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, "ERROR_CODE", "Error message");
 *
 * // No content response
 * return ApiResponseBuilder.noContent();
 * }</pre>
 */
public final class ApiResponseBuilder {
    private ApiResponseBuilder() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a 200 OK response with data.
     *
     * @param <T>  The type of data
     * @param data The response data
     * @return ResponseEntity with ApiResponse
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Creates a 200 OK response with data and links.
     *
     * @param <T>   The type of data
     * @param data  The response data
     * @param links HATEOAS links
     * @return ResponseEntity with ApiResponse
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, Map<String, String> links) {
        return ResponseEntity.ok(ApiResponse.success(data, links));
    }

    /**
     * Creates a 200 OK response with data, links, and meta.
     *
     * @param <T>   The type of data
     * @param data  The response data
     * @param links HATEOAS links
     * @param meta  Metadata (pagination, etc.)
     * @return ResponseEntity with ApiResponse
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, Map<String, String> links, ApiMeta meta) {
        return ResponseEntity.ok(ApiResponse.success(data, links, meta));
    }

    /**
     * Creates a 201 Created response with data.
     *
     * @param <T>  The type of data
     * @param data The response data
     * @return ResponseEntity with ApiResponse
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data));
    }

    /**
     * Creates a 201 Created response with data and links.
     *
     * @param <T>   The type of data
     * @param data  The response data
     * @param links HATEOAS links
     * @return ResponseEntity with ApiResponse
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data, Map<String, String> links) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data, links));
    }

    /**
     * Creates a 202 Accepted response with data.
     *
     * @param <T>  The type of data
     * @param data The response data
     * @return ResponseEntity with ApiResponse
     */
    public static <T> ResponseEntity<ApiResponse<T>> accepted(T data) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(data));
    }

    /**
     * Creates a 204 No Content response.
     *
     * @param <T> The type parameter
     * @return ResponseEntity with no content
     */
    public static <T> ResponseEntity<ApiResponse<T>> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates an error response with the specified status code, error code, and message.
     *
     * @param <T>       The type parameter (unused for errors)
     * @param status    The HTTP status code (must not be null)
     * @param errorCode The error code (must not be null or empty)
     * @param message   The error message (must not be null or empty)
     * @return ResponseEntity with ApiResponse containing error
     * @throws NullPointerException     if status, errorCode, or message is null
     * @throws IllegalArgumentException if errorCode or message is empty
     */
    public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String errorCode, String message) {
        if (status == null) {
            throw new NullPointerException("HTTP status cannot be null");
        }
        ApiError error = ApiError.builder(errorCode, message).build();
        return ResponseEntity.status(status.value()).body(ApiResponse.error(error));
    }

    /**
     * Creates an error response with the specified status code, error code, message, and details.
     *
     * @param <T>       The type parameter (unused for errors)
     * @param status    The HTTP status code
     * @param errorCode The error code
     * @param message   The error message
     * @param details   Additional error details
     * @return ResponseEntity with ApiResponse containing error
     */
    public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String errorCode, String message, Map<String, Object> details) {
        ApiError error = ApiError.builder(errorCode, message).details(details).build();
        return ResponseEntity.status(status.value()).body(ApiResponse.error(error));
    }

    /**
     * Creates an error response with full error details.
     *
     * @param <T>    The type parameter (unused for errors)
     * @param status The HTTP status code (must not be null)
     * @param error  The ApiError object (must not be null)
     * @return ResponseEntity with ApiResponse containing error
     * @throws NullPointerException if status or error is null
     */
    public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, ApiError error) {
        if (status == null) {
            throw new NullPointerException("HTTP status cannot be null");
        }
        if (error == null) {
            throw new NullPointerException("ApiError cannot be null");
        }
        return ResponseEntity.status(status.value()).body(ApiResponse.error(error));
    }
}

