package com.ccbsa.wms.gateway.api.util;

import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.wms.gateway.api.helper.AuthenticationHelper;

/**
 * Helper utility for authenticated requests.
 *
 * <p>Note: This helper does NOT manually add X-Tenant-Id, X-User-Id, or X-Role headers.
 * The gateway (TenantValidationFilter + TenantContextFilter) automatically injects these
 * headers from the JWT token. This ensures tests accurately simulate production behavior
 * where the frontend does not send these headers.
 *
 * <p>Headers injected by gateway:
 * <ul>
 *   <li>X-Tenant-Id - From JWT tenant_id claim</li>
 *   <li>X-User-Id - From JWT sub (subject) claim</li>
 *   <li>X-Role - From JWT realm_access.roles claim (comma-separated)</li>
 * </ul>
 */
public final class RequestHeaderHelper {

    private RequestHeaderHelper() {
        // Utility class
    }

    /**
     * Returns the request spec as-is.
     *
     * <p>This method exists for backward compatibility with existing tests.
     * The gateway automatically injects required headers (X-Tenant-Id, X-User-Id, X-Role)
     * from the JWT token, so no manual header addition is needed.
     *
     * <p>Tests should rely on the gateway to inject headers, just like the frontend does.
     * This ensures tests accurately simulate production behavior.
     *
     * @param requestSpec The WebTestClient request spec builder
     * @param authHelper  Authentication helper (unused, kept for backward compatibility)
     * @param accessToken The JWT access token (unused, kept for backward compatibility)
     * @return Request spec unchanged (gateway will inject headers)
     */
    public static <T extends WebTestClient.RequestHeadersSpec<?>> T addTenantHeaderIfNeeded(
            T requestSpec,
            @SuppressWarnings("unused") AuthenticationHelper authHelper,
            @SuppressWarnings("unused") String accessToken) {
        // Gateway automatically injects X-Tenant-Id, X-User-Id, and X-Role headers
        // from the JWT token via TenantValidationFilter and TenantContextFilter.
        // No manual header addition needed - tests should simulate production behavior.
        return requestSpec;
    }
}

