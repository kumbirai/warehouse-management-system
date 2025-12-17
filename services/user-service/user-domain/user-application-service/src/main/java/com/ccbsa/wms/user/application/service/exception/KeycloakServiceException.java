package com.ccbsa.wms.user.application.service.exception;

/**
 * Exception thrown when Keycloak service is unavailable or returns an error.
 * <p>
 * This exception indicates a problem with the Keycloak service itself, not with the authentication credentials.
 */
public class KeycloakServiceException
        extends RuntimeException {
    public KeycloakServiceException(String message) {
        super(message);
    }

    public KeycloakServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

