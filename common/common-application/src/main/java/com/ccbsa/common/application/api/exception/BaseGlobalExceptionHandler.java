package com.ccbsa.common.application.api.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccbsa.common.application.api.ApiError;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.application.api.RequestContext;
import com.ccbsa.common.domain.exception.DomainException;
import com.ccbsa.common.domain.exception.EntityNotFoundException;
import com.ccbsa.common.domain.exception.InvalidOperationException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Base Global Exception Handler for all services.
 *
 * <p>This base handler provides common exception handling for exceptions that are
 * shared across all services. Services can extend this class to add service-specific exception handlers while inheriting the common handlers.</p>
 *
 * <p>Handles the following common exceptions:</p>
 * <ul>
 *   <li>{@link EntityNotFoundException} - Resource not found (404)</li>
 *   <li>{@link InvalidOperationException} - Invalid operation (400)</li>
 *   <li>{@link DomainException} - Generic domain exception (400)</li>
 *   <li>{@link IllegalArgumentException} - Validation error (400)</li>
 *   <li>{@link IllegalStateException} - Invalid state (400)</li>
 *   <li>{@link MethodArgumentNotValidException} - Spring validation (400)</li>
 *   <li>{@link ConstraintViolationException} - Bean validation (400)</li>
 *   <li>{@link Exception} - Generic catch-all (500)</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * @RestControllerAdvice
 * public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {
 *
 *     // Service-specific exception handlers can be added here
 *     @ExceptionHandler(ServiceSpecificException.class)
 *     public ResponseEntity<ApiResponse<Void>> handleServiceSpecific(
 *             ServiceSpecificException ex, HttpServletRequest request) {
 *         // Handle service-specific exception
 *     }
 * }
 * }</pre>
 */
@RestControllerAdvice
public class BaseGlobalExceptionHandler {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Resource not found: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("RESOURCE_NOT_FOUND", ex.getMessage())
                .path(path)
                .requestId(requestId)
                .build();
        return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error);
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOperation(InvalidOperationException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Invalid operation: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("INVALID_OPERATION", ex.getMessage())
                .path(path)
                .requestId(requestId)
                .build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Domain exception: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("DOMAIN_ERROR", ex.getMessage())
                .path(path)
                .requestId(requestId)
                .build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Validation error: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("VALIDATION_ERROR", ex.getMessage())
                .path(path)
                .requestId(requestId)
                .build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Invalid state: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("INVALID_OPERATION", ex.getMessage())
                .path(path)
                .requestId(requestId)
                .build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        Map<String, Object> details = new HashMap<>();
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Validation failed",
                        (existing, replacement) -> existing));
        details.put("fieldErrors", fieldErrors);

        logger.warn("Request validation failed - RequestId: {}, Path: {}, FieldErrors: {}", requestId, path, fieldErrors.keySet());

        ApiError error = ApiError.builder("VALIDATION_ERROR", "Request validation failed")
                .details(details)
                .path(path)
                .requestId(requestId)
                .build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        Map<String, Object> details = new HashMap<>();
        Map<String, String> violations = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(violation -> violation.getPropertyPath()
                        .toString(), ConstraintViolation::getMessage, (existing, replacement) -> existing));
        details.put("constraintViolations", violations);

        logger.warn("Constraint validation failed - RequestId: {}, Path: {}, Violations: {}", requestId, path, violations.keySet());

        ApiError error = ApiError.builder("VALIDATION_ERROR", "Constraint validation failed")
                .details(details)
                .path(path)
                .requestId(requestId)
                .build();
        return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.error("Unexpected error occurred - RequestId: {}, Path: {}", requestId, path, ex);

        ApiError error = ApiError.builder("INTERNAL_SERVER_ERROR", "An unexpected error occurred")
                .path(path)
                .requestId(requestId)
                .build();
        return ApiResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, error);
    }
}

