package com.ccbsa.wms.integration.application.api.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccbsa.common.application.api.exception.BaseGlobalExceptionHandler;

/**
 * Global Exception Handler: GlobalExceptionHandler
 * <p>
 * Handles exceptions across all controllers and provides consistent error responses using the standardized ApiResponse format.
 *
 * <p>This handler extends {@link BaseGlobalExceptionHandler} to inherit common exception
 * handling and can add integration-service-specific exception handlers if needed.</p>
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
    // Add service-specific exception handlers here as needed
}

