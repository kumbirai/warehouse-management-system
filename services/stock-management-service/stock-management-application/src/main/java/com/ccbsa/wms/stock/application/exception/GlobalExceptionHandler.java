package com.ccbsa.wms.stock.application.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccbsa.common.application.api.ApiError;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.application.api.RequestContext;
import com.ccbsa.common.application.api.exception.BaseGlobalExceptionHandler;
import com.ccbsa.wms.stock.application.service.exception.ProductServiceException;
import com.ccbsa.wms.stock.domain.core.exception.ConsignmentNotFoundException;
import com.ccbsa.wms.stock.domain.core.exception.InvalidConsignmentReferenceException;
import com.ccbsa.wms.stock.domain.core.exception.InvalidExpirationDateException;
import com.ccbsa.wms.stock.domain.core.exception.InvalidQuantityException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global Exception Handler: GlobalExceptionHandler
 * <p>
 * Handles exceptions across all controllers and provides consistent error responses using the standardized ApiResponse format.
 * <p>
 * This handler extends {@link BaseGlobalExceptionHandler} to inherit common exception handling and adds stock-management-service-specific exception handlers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    /**
     * Handles ConsignmentNotFoundException. Returns 404 Not Found.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 404 Not Found
     */
    @ExceptionHandler(ConsignmentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleConsignmentNotFound(ConsignmentNotFoundException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Consignment not found: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("CONSIGNMENT_NOT_FOUND", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error);
    }

    /**
     * Handles InvalidConsignmentReferenceException. Returns 400 Bad Request.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 400 Bad Request
     */
    @ExceptionHandler(InvalidConsignmentReferenceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidConsignmentReference(InvalidConsignmentReferenceException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Invalid consignment reference: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("INVALID_CONSIGNMENT_REFERENCE", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }

    /**
     * Handles InvalidQuantityException. Returns 400 Bad Request.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 400 Bad Request
     */
    @ExceptionHandler(InvalidQuantityException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidQuantity(InvalidQuantityException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Invalid quantity: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("INVALID_QUANTITY", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }

    /**
     * Handles InvalidExpirationDateException. Returns 400 Bad Request.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 400 Bad Request
     */
    @ExceptionHandler(InvalidExpirationDateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidExpirationDate(InvalidExpirationDateException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Invalid expiration date: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("INVALID_EXPIRATION_DATE", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }

    /**
     * Handles authorization denied exceptions from Spring Security method security. Returns 403 Forbidden instead of 500 Internal Server Error.
     *
     * @param ex      The authorization exception
     * @param request The HTTP request
     * @return Error response with 403 Forbidden
     */
    @ExceptionHandler( {AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Access denied: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("ACCESS_DENIED", "You do not have permission to perform this action").path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.FORBIDDEN, error);
    }

    /**
     * Handles ProductServiceException - indicates Product Service is unavailable.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 503 Service Unavailable
     */
    @ExceptionHandler(ProductServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductServiceException(ProductServiceException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.error("Product service error: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path, ex);

        ApiError error = ApiError.builder("PRODUCT_SERVICE_UNAVAILABLE", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.SERVICE_UNAVAILABLE, error);
    }

    /**
     * Handles HttpMessageNotReadableException - JSON deserialization errors (e.g., invalid enum values).
     * Returns 400 Bad Request instead of 500 Internal Server Error.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 400 Bad Request
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        // Extract the root cause message for better error reporting
        String message = ex.getMessage();
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            message = ex.getCause().getMessage();
        }

        logger.warn("JSON deserialization error: {} - RequestId: {}, Path: {}", message, requestId, path);

        ApiError error = ApiError.builder("INVALID_REQUEST", message).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }

    /**
     * Handles DataIntegrityViolationException - database constraint violations (e.g., unique constraints, foreign key violations).
     * Returns 409 Conflict instead of 500 Internal Server Error.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 409 Conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        // Extract a more user-friendly message from the exception
        String message = "Data integrity violation";
        if (ex.getMessage() != null) {
            // Try to extract a more meaningful message
            String errorMessage = ex.getMessage();
            if (errorMessage.contains("unique constraint") || errorMessage.contains("duplicate key")) {
                message = "A record with this information already exists";
            } else if (errorMessage.contains("foreign key constraint") || errorMessage.contains("referential integrity")) {
                message = "Referenced record does not exist";
            } else {
                message = "Data integrity constraint violation";
            }
        }

        logger.warn("Data integrity violation: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("DATA_INTEGRITY_VIOLATION", message).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.CONFLICT, error);
    }

    /**
     * Override IllegalStateException handler to provide more specific error codes for stock-related errors.
     * Returns 400 Bad Request with appropriate error code based on the error message.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 400 Bad Request
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);
        String message = ex.getMessage();

        // Check if this is an insufficient stock error
        String errorCode = "INVALID_OPERATION";
        if (message != null && (message.contains("Insufficient") && message.contains("stock"))) {
            errorCode = "INSUFFICIENT_STOCK";
        }

        logger.warn("Invalid state: {} - RequestId: {}, Path: {}", message, requestId, path);

        ApiError error = ApiError.builder(errorCode, message).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }
}

