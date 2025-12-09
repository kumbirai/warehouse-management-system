package com.ccbsa.common.application.api;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for extracting request context information from HTTP requests.
 *
 * <p>Provides methods to extract request metadata such as request ID, path, and other
 * contextual information needed for error responses and logging.</p>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * String requestId = RequestContext.getRequestId(request);
 * String path = RequestContext.getRequestPath(request);
 * }</pre>
 */
public final class RequestContext {
    /**
     * Standard header name for request ID.
     */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    /**
     * Standard header name for correlation ID.
     */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private RequestContext() {
        // Utility class - prevent instantiation
    }

    /**
     * Extracts request ID from request header or generates a new one.
     *
     * <p>Checks for request ID in the following order:</p>
     * <ol>
     *   <li>X-Request-Id header</li>
     *   <li>X-Correlation-Id header</li>
     *   <li>Generated UUID if neither header is present</li>
     * </ol>
     *
     * @param request The HTTP request
     * @return Request ID string, never null
     */
    public static String getRequestId(HttpServletRequest request) {
        if (request == null) {
            return UUID.randomUUID()
                    .toString();
        }

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId != null && !requestId.trim()
                .isEmpty()) {
            return requestId.trim();
        }

        // Fallback to correlation ID
        requestId = request.getHeader(CORRELATION_ID_HEADER);
        if (requestId != null && !requestId.trim()
                .isEmpty()) {
            return requestId.trim();
        }

        // Generate new UUID if no header present
        return UUID.randomUUID()
                .toString();
    }

    /**
     * Extracts the request path from the HTTP request.
     *
     * @param request The HTTP request
     * @return Request path, or null if request is null
     */
    public static String getRequestPath(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getRequestURI();
    }

    /**
     * Extracts the request method from the HTTP request.
     *
     * @param request The HTTP request
     * @return Request method (GET, POST, etc.), or null if request is null
     */
    public static String getRequestMethod(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getMethod();
    }

    /**
     * Extracts the query string from the HTTP request.
     *
     * @param request The HTTP request
     * @return Query string, or null if request is null or no query string
     */
    public static String getQueryString(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getQueryString();
    }

    /**
     * Gets the full request URL including query string.
     *
     * @param request The HTTP request
     * @return Full request URL, or null if request is null
     */
    public static String getFullRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String requestUrl = request.getRequestURL()
                .toString();
        String queryString = request.getQueryString();

        if (queryString != null && !queryString.isEmpty()) {
            return String.format("%s?%s",
                    requestUrl,
                    queryString);
        }

        return requestUrl;
    }
}

