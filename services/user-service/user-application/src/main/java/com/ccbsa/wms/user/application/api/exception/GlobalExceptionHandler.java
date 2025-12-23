package com.ccbsa.wms.user.application.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccbsa.common.application.api.ApiError;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.application.api.RequestContext;
import com.ccbsa.common.application.api.exception.BaseGlobalExceptionHandler;
import com.ccbsa.wms.user.application.service.exception.AuthenticationException;
import com.ccbsa.wms.user.application.service.exception.DuplicateUserException;
import com.ccbsa.wms.user.application.service.exception.InsufficientPrivilegesException;
import com.ccbsa.wms.user.application.service.exception.KeycloakServiceException;
import com.ccbsa.wms.user.application.service.exception.RoleAssignmentException;
import com.ccbsa.wms.user.application.service.exception.TenantMismatchException;
import com.ccbsa.wms.user.application.service.exception.TenantNotActiveException;
import com.ccbsa.wms.user.application.service.exception.TenantNotFoundException;
import com.ccbsa.wms.user.application.service.exception.TenantServiceException;
import com.ccbsa.wms.user.application.service.exception.UserCreationException;
import com.ccbsa.wms.user.application.service.exception.UserNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global Exception Handler: GlobalExceptionHandler
 * <p>
 * Handles exceptions across all controllers and provides consistent error responses using the standardized ApiResponse format.
 *
 * <p>This handler extends {@link BaseGlobalExceptionHandler} to inherit common exception
 * handling and adds user-service-specific exception handlers.</p>
 *
 * <p>Common exceptions are handled by the base class:</p>
 * <ul>
 *   <li>{@link com.ccbsa.common.domain.exception.EntityNotFoundException}</li>
 *   <li>{@link com.ccbsa.common.domain.exception.InvalidOperationException}</li>
 *   <li>{@link com.ccbsa.common.domain.exception.DomainException}</li>
 *   <li>{@link IllegalArgumentException}</li>
 *   <li>{@link IllegalStateException}</li>
 *   <li>{@link org.springframework.web.bind.MethodArgumentNotValidException}</li>
 *   <li>{@link jakarta.validation.ConstraintViolationException}</li>
 *   <li>{@link Exception}</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles AuthenticationException - indicates authentication failed (invalid credentials).
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Authentication failed: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("AUTHENTICATION_FAILED", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.UNAUTHORIZED, error);
    }

    /**
     * Handles KeycloakServiceException - indicates Keycloak service is unavailable.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 503 Service Unavailable
     */
    @ExceptionHandler(KeycloakServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleKeycloakServiceException(KeycloakServiceException ex, HttpServletRequest request) {

        logger.error("Keycloak service error: {}", ex.getMessage(), ex);

        return ApiResponseBuilder.error(HttpStatus.SERVICE_UNAVAILABLE, "Authentication service is temporarily unavailable. Please try again later.", "KEYCLOAK_SERVICE_ERROR");
    }

    /**
     * Handles TenantNotFoundException - indicates tenant does not exist.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 404 Not Found
     */
    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenantNotFoundException(TenantNotFoundException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Tenant not found: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("TENANT_NOT_FOUND", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error);
    }

    /**
     * Handles TenantNotActiveException - indicates tenant is not active.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 400 Bad Request
     */
    @ExceptionHandler(TenantNotActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenantNotActiveException(TenantNotActiveException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Tenant not active: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("TENANT_NOT_ACTIVE", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }

    /**
     * Handles TenantServiceException - indicates tenant service is unavailable.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 503 Service Unavailable
     */
    @ExceptionHandler(TenantServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenantServiceException(TenantServiceException ex, HttpServletRequest request) {
        logger.error("Tenant service error: {}", ex.getMessage(), ex);

        return ApiResponseBuilder.error(HttpStatus.SERVICE_UNAVAILABLE, "Tenant service is temporarily unavailable. Please try again later.", "TENANT_SERVICE_ERROR");
    }

    /**
     * Handles UserNotFoundException - indicates user does not exist.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 404 Not Found
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(UserNotFoundException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("User not found: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("USER_NOT_FOUND", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error);
    }

    /**
     * Handles DuplicateUserException - indicates user with same username/email already exists.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 400 Bad Request
     */
    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateUserException(DuplicateUserException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Duplicate user: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("DUPLICATE_USER", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }

    /**
     * Handles UserCreationException - indicates user creation failed.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 500 Internal Server Error
     */
    @ExceptionHandler(UserCreationException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserCreationException(UserCreationException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.error("User creation failed: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path, ex);

        ApiError error = ApiError.builder("USER_CREATION_FAILED", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, error);
    }

    /**
     * Handles InsufficientPrivilegesException - indicates user does not have sufficient privileges.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 403 Forbidden
     */
    @ExceptionHandler(InsufficientPrivilegesException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientPrivilegesException(InsufficientPrivilegesException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Insufficient privileges: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("INSUFFICIENT_PRIVILEGES", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.FORBIDDEN, error);
    }

    /**
     * Handles TenantMismatchException - indicates tenant mismatch in operation.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 403 Forbidden
     */
    @ExceptionHandler(TenantMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenantMismatchException(TenantMismatchException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Tenant mismatch: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("TENANT_MISMATCH", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.FORBIDDEN, error);
    }

    /**
     * Handles RoleAssignmentException - indicates role assignment/removal failed.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 500 Internal Server Error
     */
    @ExceptionHandler(RoleAssignmentException.class)
    public ResponseEntity<ApiResponse<Void>> handleRoleAssignmentException(RoleAssignmentException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.error("Role assignment failed: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path, ex);

        ApiError error = ApiError.builder("ROLE_ASSIGNMENT_FAILED", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, error);
    }
}

