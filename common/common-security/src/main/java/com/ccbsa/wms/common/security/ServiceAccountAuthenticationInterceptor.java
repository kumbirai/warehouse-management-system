package com.ccbsa.wms.common.security;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RestTemplate Interceptor for Service-to-Service Authentication
 * <p>
 * Production-grade interceptor that automatically injects authentication headers
 * for inter-service REST calls. This interceptor supports two authentication modes:
 * <p>
 * 1. **HTTP Request Context Available** (e.g., REST endpoint handling):
 * - Forwards the Authorization header from the incoming HTTP request
 * - Forwards the X-Tenant-Id header for tenant context
 * - Maintains request tracing with original user credentials
 * <p>
 * 2. **No HTTP Request Context** (e.g., event listeners, scheduled jobs):
 * - Automatically obtains service account token from {@link ServiceAccountTokenProvider}
 * - Injects service account JWT token in Authorization header
 * - Allows event-driven calls to authenticate without user context
 * <p>
 * This interceptor solves the critical issue where event listeners don't have
 * HTTP request context and therefore cannot forward Authorization headers.
 * <p>
 * Usage:
 * <pre>
 * &#64;Bean
 * public RestTemplate serviceToServiceRestTemplate(ServiceAccountAuthenticationInterceptor interceptor) {
 *     RestTemplate restTemplate = new RestTemplate();
 *     restTemplate.setInterceptors(Collections.singletonList(interceptor));
 *     return restTemplate;
 * }
 * </pre>
 *
 * @see ServiceAccountTokenProvider
 */
@Slf4j
@RequiredArgsConstructor
@SuppressFBWarnings(value = {"CT_CONSTRUCTOR_THROW", "UPM_UNCALLED_PRIVATE_METHOD"}, justification =
        "CT_CONSTRUCTOR_THROW: Lombok-generated constructor is safe for Spring-managed components. "
                + "UPM_UNCALLED_PRIVATE_METHOD: Private methods are called from intercept() method, SpotBugs cannot detect this.")
public class ServiceAccountAuthenticationInterceptor implements ClientHttpRequestInterceptor {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "ServiceAccountTokenProvider is a Spring-managed component that should not be mutated externally. "
            + "Constructor injection ensures proper lifecycle management by Spring container.")
    private final ServiceAccountTokenProvider serviceAccountTokenProvider;

    @Override
    @NonNull
    public ClientHttpResponse intercept(@NonNull HttpRequest request, @NonNull byte[] body, @NonNull ClientHttpRequestExecution execution) throws IOException {

        // Try to forward Authorization header from current HTTP request context
        String authorizationHeader = getAuthorizationHeaderFromContext();

        if (authorizationHeader != null) {
            // HTTP request context available - forward existing Authorization header
            log.debug("Forwarding Authorization header from HTTP request context to: {}", request.getURI());
            request.getHeaders().set("Authorization", authorizationHeader);

            // Also forward X-Tenant-Id header if available
            String tenantIdHeader = getTenantIdHeaderFromContext();
            if (tenantIdHeader != null) {
                request.getHeaders().set("X-Tenant-Id", tenantIdHeader);
                log.debug("Forwarding X-Tenant-Id header: {}", tenantIdHeader);
            }
        } else {
            // No HTTP request context (event listener, scheduled job, etc.)
            // Use service account token for authentication
            log.debug("No HTTP request context available. Using service account token for: {}", request.getURI());

            try {
                String serviceAccountToken = serviceAccountTokenProvider.getAccessToken();
                request.getHeaders().set("Authorization", "Bearer " + serviceAccountToken);
                log.info("Injected service account token for service-to-service call to: {}", request.getURI());

                // For service account calls, still forward X-Tenant-Id if available from TenantContext
                String tenantId = getTenantIdFromTenantContext();
                if (tenantId != null) {
                    request.getHeaders().set("X-Tenant-Id", tenantId);
                    log.debug("Forwarding X-Tenant-Id from TenantContext: {}", tenantId);
                } else {
                    // Downgrade to DEBUG for Keycloak endpoints (tenant context not required)
                    String uri = request.getURI().toString();
                    if (uri.contains("/realms/") || uri.contains("/protocol/openid-connect")) {
                        log.debug("No X-Tenant-Id available from TenantContext for Keycloak call to: {} (tenant context not required for Keycloak)", uri);
                    } else {
                        log.warn("No X-Tenant-Id available from TenantContext for service-to-service call to: {}", uri);
                    }
                }
            } catch (ServiceAccountAuthenticationException e) {
                log.error("Failed to obtain service account token for inter-service call to: {}. Error: {}", request.getURI(), e.getMessage(), e);
                // Allow the request to proceed without token - let the target service return 401
                // This allows for better error messages and doesn't fail fast
            }
        }

        // Continue with the request
        return execution.execute(request, body);
    }

    /**
     * Extracts the Authorization header from the current HTTP request context.
     * <p>
     * This method works when the call is made within an HTTP request handling thread
     * (e.g., REST controller handling a request).
     *
     * @return Authorization header value or null if not available
     */
    private String getAuthorizationHeaderFromContext() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("Authorization");
            }
        } catch (Exception e) {
            log.debug("Could not extract Authorization header from request context: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extracts the X-Tenant-Id header from the current HTTP request context.
     *
     * @return X-Tenant-Id header value or null if not available
     */
    private String getTenantIdHeaderFromContext() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("X-Tenant-Id");
            }
        } catch (Exception e) {
            log.debug("Could not extract X-Tenant-Id header from request context: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extracts tenant ID from TenantContext (ThreadLocal).
     * <p>
     * This is used when HTTP request context is not available but TenantContext
     * has been set (e.g., by event listeners).
     *
     * @return Tenant ID or null if not available
     */
    private String getTenantIdFromTenantContext() {
        try {
            if (TenantContext.getTenantId() != null) {
                return TenantContext.getTenantId().getValue();
            }
        } catch (Exception e) {
            log.debug("Could not extract tenant ID from TenantContext: {}", e.getMessage());
        }
        return null;
    }
}
