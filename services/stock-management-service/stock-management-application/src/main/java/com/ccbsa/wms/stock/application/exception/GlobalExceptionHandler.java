package com.ccbsa.wms.stock.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccbsa.common.application.api.ApiError;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.application.api.RequestContext;
import com.ccbsa.common.application.api.exception.BaseGlobalExceptionHandler;
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
public class GlobalExceptionHandler
        extends BaseGlobalExceptionHandler {

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

        ApiError error = ApiError.builder("CONSIGNMENT_NOT_FOUND", ex.getMessage())
                .path(path)
                .requestId(requestId)
                .build();
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

        ApiError error = ApiError.builder("INVALID_CONSIGNMENT_REFERENCE", ex.getMessage())
                .path(path)
                .requestId(requestId)
                .build();
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

        ApiError error = ApiError.builder("INVALID_QUANTITY", ex.getMessage())
                .path(path)
                .requestId(requestId)
                .build();
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

        ApiError error = ApiError.builder("INVALID_EXPIRATION_DATE", ex.getMessage())
                .path(path)
                .requestId(requestId)
                .build();
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

        ApiError error = ApiError.builder("ACCESS_DENIED", "You do not have permission to perform this action")
                .path(path)
                .requestId(requestId)
                .build();
        return ApiResponseBuilder.error(HttpStatus.FORBIDDEN, error);
    }
}

