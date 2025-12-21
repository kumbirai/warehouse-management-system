package com.ccbsa.wms.product.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccbsa.common.application.api.ApiError;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.application.api.RequestContext;
import com.ccbsa.common.application.api.exception.BaseGlobalExceptionHandler;
import com.ccbsa.wms.product.domain.core.exception.BarcodeAlreadyExistsException;
import com.ccbsa.wms.product.domain.core.exception.ProductCodeAlreadyExistsException;
import com.ccbsa.wms.product.domain.core.exception.ProductNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global Exception Handler: GlobalExceptionHandler
 * <p>
 * Handles exceptions across all controllers and provides consistent error responses using the standardized ApiResponse format.
 * <p>
 * This handler extends {@link BaseGlobalExceptionHandler} to inherit common exception handling and adds product-service-specific exception handlers.
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
     * Handles ProductNotFoundException. Returns 404 Not Found.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 404 Not Found
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Product not found: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("PRODUCT_NOT_FOUND", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error);
    }

    /**
     * Handles BarcodeAlreadyExistsException. Returns 409 Conflict.
     * <p>
     * This exception indicates a resource conflict where the barcode already exists for the tenant.
     * HTTP 409 Conflict is the appropriate status code for resource conflicts.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 409 Conflict
     */
    @ExceptionHandler(BarcodeAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBarcodeAlreadyExists(BarcodeAlreadyExistsException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Barcode already exists: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("BARCODE_ALREADY_EXISTS", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.CONFLICT, error);
    }

    /**
     * Handles ProductCodeAlreadyExistsException. Returns 409 Conflict.
     * <p>
     * This exception indicates a resource conflict where the product code already exists for the tenant.
     * HTTP 409 Conflict is the appropriate status code for resource conflicts.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 409 Conflict
     */
    @ExceptionHandler(ProductCodeAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductCodeAlreadyExists(ProductCodeAlreadyExistsException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Product code already exists: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("PRODUCT_CODE_ALREADY_EXISTS", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.CONFLICT, error);
    }
}

