package com.ccbsa.wms.location.application.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccbsa.common.application.api.ApiError;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.application.api.RequestContext;
import com.ccbsa.common.application.api.exception.BaseGlobalExceptionHandler;
import com.ccbsa.wms.location.domain.core.exception.BarcodeAlreadyExistsException;
import com.ccbsa.wms.location.domain.core.exception.CodeAlreadyExistsException;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global Exception Handler: GlobalExceptionHandler
 * <p>
 * Handles exceptions across all controllers and provides consistent error responses using the standardized ApiResponse format.
 * <p>
 * This handler extends {@link BaseGlobalExceptionHandler} to inherit common exception handling and adds location-management-service-specific exception handlers.
 * <p>
 * Common exceptions are handled by the base class:
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

    /**
     * Handles LocationNotFoundException.
     * <p>
     * Returns 404 Not Found when a location resource cannot be found, regardless of HTTP method.
     * This follows REST API best practices where "resource not found" is always a 404 error,
     * whether it's a GET (query) or PUT/DELETE/PATCH (command) operation.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 404 Not Found
     */
    @ExceptionHandler(LocationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocationNotFound(LocationNotFoundException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);
        String method = request.getMethod();

        logger.warn("Location not found: {} - RequestId: {}, Path: {}, Method: {}", ex.getMessage(), requestId, path, method);
        ApiError error = ApiError.builder("LOCATION_NOT_FOUND", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error);
    }

    /**
     * Handles BarcodeAlreadyExistsException. Returns 400 Bad Request.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 400 Bad Request
     */
    @ExceptionHandler(BarcodeAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBarcodeAlreadyExists(BarcodeAlreadyExistsException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Barcode already exists: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("BARCODE_ALREADY_EXISTS", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }

    /**
     * Handles CodeAlreadyExistsException. Returns 400 Bad Request.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 400 Bad Request
     */
    @ExceptionHandler(CodeAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleCodeAlreadyExists(CodeAlreadyExistsException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Code already exists: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("CODE_ALREADY_EXISTS", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
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
}

