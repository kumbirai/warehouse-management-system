package com.ccbsa.wms.gateway.api.util;

import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.wms.gateway.api.helper.AuthenticationHelper;

/**
 * Helper utility for adding required headers to authenticated requests.
 */
public final class RequestHeaderHelper {

    private static final String X_TENANT_ID_HEADER = "X-Tenant-Id";

    private RequestHeaderHelper() {
        // Utility class
    }

    /**
     * Adds X-Tenant-Id header to a WebTestClient request spec if needed.
     *
     * <p>Adds the header if:
     * - Tenant ID is present in the JWT token
     * - User is not SYSTEM_ADMIN (SYSTEM_ADMIN users can bypass tenant validation)
     *
     * @param requestSpec The WebTestClient request spec builder
     * @param authHelper  Authentication helper to extract tenant ID and check roles
     * @param accessToken The JWT access token
     * @return Request spec with X-Tenant-Id header added if needed
     */
    @SuppressWarnings("unchecked")
    public static <T extends WebTestClient.RequestHeadersSpec<?>> T addTenantHeaderIfNeeded(
            T requestSpec,
            AuthenticationHelper authHelper,
            String accessToken) {

        String tenantId = authHelper.getTenantIdFromToken(accessToken);
        boolean isSystemAdmin = authHelper.isSystemAdminUser(accessToken);

        // Add X-Tenant-Id header if tenant ID is present and user is not SYSTEM_ADMIN
        if (tenantId != null && !tenantId.isEmpty() && !isSystemAdmin) {
            return (T) requestSpec.header(X_TENANT_ID_HEADER, tenantId);
        }

        return requestSpec;
    }
}

