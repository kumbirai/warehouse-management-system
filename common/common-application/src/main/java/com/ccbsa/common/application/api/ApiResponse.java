package com.ccbsa.common.application.api;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standardized API Response wrapper for all REST API responses.
 * <p>
 * This class ensures consistent response format across all backend services, enabling the frontend to consume backend services uniformly.
 *
 * <p>Success Response Format:</p>
 * <pre>{@code
 * {
 *   "data": { ... },
 *   "links": { ... },
 *   "meta": { ... }
 * }
 * }</pre>
 *
 * <p>Error Response Format:</p>
 * <pre>{@code
 * {
 *   "error": {
 *     "code": "ERROR_CODE",
 *     "message": "Human-readable error message",
 *     "details": { ... },
 *     "timestamp": "2025-11-15T10:30:00Z",
 *     "path": "/api/v1/resource",
 *     "requestId": "req-123"
 *   }
 * }
 * }</pre>
 *
 * @param <T> The type of data payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiResponse<T> {
    private T data;
    private ApiError error;
    private Map<String, String> links;
    private ApiMeta meta;

    private ApiResponse() {
        // Private constructor for builder pattern
    }

    private ApiResponse(T data, ApiError error, Map<String, String> links, ApiMeta meta) {
        this.data = data;
        this.error = error;
        this.links = copyLinks(links);
        this.meta = meta;
    }

    private static Map<String, String> copyLinks(Map<String, String> links) {
        return links == null ? null : Map.copyOf(links);
    }

    /**
     * Creates a success response with data.
     *
     * <p>Note: For 204 No Content responses, use {@link #noContent()} instead.</p>
     *
     * @param <T>  The type of data
     * @param data The response data (can be null for empty responses)
     * @return ApiResponse with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null, null, null);
    }

    /**
     * Creates a success response with data and links.
     *
     * @param <T>   The type of data
     * @param data  The response data
     * @param links HATEOAS links
     * @return ApiResponse with data and links
     */
    public static <T> ApiResponse<T> success(T data, Map<String, String> links) {
        return new ApiResponse<>(data, null, links, null);
    }

    /**
     * Creates a success response with data, links, and meta.
     *
     * @param <T>   The type of data
     * @param data  The response data
     * @param links HATEOAS links
     * @param meta  Metadata (pagination, etc.)
     * @return ApiResponse with data, links, and meta
     */
    public static <T> ApiResponse<T> success(T data, Map<String, String> links, ApiMeta meta) {
        return new ApiResponse<>(data, null, links, meta);
    }

    /**
     * Creates an error response.
     *
     * @param <T>   The type parameter (unused for errors)
     * @param error The error details (must not be null)
     * @return ApiResponse with error
     * @throws NullPointerException if error is null
     */
    public static <T> ApiResponse<T> error(ApiError error) {
        if (error == null) {
            throw new NullPointerException("Error cannot be null");
        }
        return new ApiResponse<>(null, error, null, null);
    }

    /**
     * Creates a no-content success response (for 204 responses).
     *
     * @param <T> The type parameter
     * @return Empty ApiResponse
     */
    public static <T> ApiResponse<T> noContent() {
        return new ApiResponse<>(null, null, null, null);
    }

    public T getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }

    public Map<String, String> getLinks() {
        return links == null ? null : Map.copyOf(links);
    }

    public ApiMeta getMeta() {
        return meta;
    }

    /**
     * Checks if this response is an error response.
     *
     * @return true if error is present, false otherwise
     */
    public boolean isError() {
        return error != null;
    }

    /**
     * Checks if this response is a success response.
     *
     * @return true if no error is present, false otherwise
     */
    public boolean isSuccess() {
        return error == null;
    }
}

