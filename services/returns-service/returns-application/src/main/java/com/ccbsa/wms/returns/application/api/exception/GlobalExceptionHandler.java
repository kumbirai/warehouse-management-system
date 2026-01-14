package com.ccbsa.wms.returns.application.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccbsa.common.application.api.ApiError;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.application.api.RequestContext;
import com.ccbsa.common.application.api.exception.BaseGlobalExceptionHandler;
import com.ccbsa.wms.returns.application.service.exception.PickingServiceException;
import com.ccbsa.wms.returns.domain.core.exception.ReturnNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global Exception Handler: GlobalExceptionHandler
 * <p>
 * Handles exceptions across all controllers and provides consistent error responses using the standardized ApiResponse format.
 *
 * <p>This handler extends {@link BaseGlobalExceptionHandler} to inherit common exception
 * handling and can add returns-service-specific exception handlers if needed.</p>
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

    /**
     * Handles ReturnNotFoundException. Returns 404 Not Found.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 404 Not Found
     */
    @ExceptionHandler(ReturnNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleReturnNotFound(ReturnNotFoundException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Return not found: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("RETURN_NOT_FOUND", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error);
    }

    /**
     * Handles PickingServiceException - indicates Picking Service is unavailable.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 503 Service Unavailable
     */
    @ExceptionHandler(PickingServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handlePickingServiceException(PickingServiceException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.error("Picking service error: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path, ex);

        ApiError error = ApiError.builder("PICKING_SERVICE_UNAVAILABLE", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.SERVICE_UNAVAILABLE, error);
    }
}

