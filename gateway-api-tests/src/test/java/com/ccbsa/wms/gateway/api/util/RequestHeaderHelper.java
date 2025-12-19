package com.ccbsa.wms.gateway.api.util;

import java.util.UUID;

import org.springframework.http.HttpHeaders;

/**
 * Helper for adding common request headers.
 */
public class RequestHeaderHelper {

    /**
     * Add authorization header with Bearer token.
     *
     * @param headers     the HttpHeaders
     * @param accessToken the JWT access token
     */
    public static void addAuthHeaders(HttpHeaders headers, String accessToken) {
        headers.setBearerAuth(accessToken);
        headers.set("X-Correlation-Id", UUID.randomUUID().toString());
    }

    /**
     * Add tenant context header.
     *
     * @param headers  the HttpHeaders
     * @param tenantId the tenant ID
     */
    public static void addTenantHeader(HttpHeaders headers, String tenantId) {
        headers.set("X-Tenant-Id", tenantId);
    }

    /**
     * Add correlation ID header.
     *
     * @param headers       the HttpHeaders
     * @param correlationId the correlation ID
     */
    public static void addCorrelationIdHeader(HttpHeaders headers, String correlationId) {
        headers.set("X-Correlation-Id", correlationId);
    }
}

