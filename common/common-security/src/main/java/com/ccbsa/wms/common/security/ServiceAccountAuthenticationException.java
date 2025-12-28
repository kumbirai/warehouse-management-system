package com.ccbsa.wms.common.security;

/**
 * Exception thrown when service account authentication fails.
 * <p>
 * This exception is thrown when:
 * - Service account token cannot be obtained from Keycloak
 * - Token refresh fails
 * - Service account credentials are invalid
 * - Keycloak token endpoint is unreachable
 */
public class ServiceAccountAuthenticationException extends RuntimeException {

    public ServiceAccountAuthenticationException(String message) {
        super(message);
    }

    public ServiceAccountAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
