package com.ccbsa.wms.product.application.exception;

import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * Handles DataIntegrityViolationException. Returns 409 Conflict.
     * <p>
     * This exception is thrown when a database constraint violation occurs (e.g., duplicate key, foreign key violation).
     * We extract meaningful error messages from the exception and return appropriate error codes.
     * <p>
     * Common scenarios:
     * <ul>
     *   <li>Duplicate barcode constraint violation - converted to BarcodeAlreadyExistsException</li>
     *   <li>Duplicate product code constraint violation - converted to ProductCodeAlreadyExistsException</li>
     *   <li>Other constraint violations - returned as generic data integrity error</li>
     * </ul>
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 409 Conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        String message = ex.getMessage();
        String errorCode = "DATA_INTEGRITY_VIOLATION";
        String errorMessage = "A data integrity constraint violation occurred";

        // Extract meaningful error information from the exception
        if (message != null) {
            // Check for duplicate barcode constraint
            if (message.contains("uk_product_barcodes_barcode") || message.contains("duplicate key value") && message.contains("barcode")) {
                errorCode = "BARCODE_ALREADY_EXISTS";
                // Try to extract barcode value from message
                if (message.contains("Key (barcode)=(")) {
                    int start = message.indexOf("Key (barcode)=(") + 15;
                    int end = message.indexOf(")", start);
                    if (end > start) {
                        String barcode = message.substring(start, end);
                        errorMessage = String.format("Product barcode already exists: %s", barcode);
                    } else {
                        errorMessage = "Product barcode already exists";
                    }
                } else {
                    errorMessage = "Product barcode already exists";
                }
            }
            // Check for duplicate product code constraint
            else if (message.contains("uk_products_tenant_product_code") || message.contains("duplicate key value") && message.contains("product_code")) {
                errorCode = "PRODUCT_CODE_ALREADY_EXISTS";
                errorMessage = "Product code already exists for this tenant";
            }
            // Check for duplicate primary barcode constraint
            else if (message.contains("uk_products_tenant_primary_barcode") || message.contains("duplicate key value") && message.contains("primary_barcode")) {
                errorCode = "BARCODE_ALREADY_EXISTS";
                errorMessage = "Primary barcode already exists for this tenant";
            }
        }

        logger.warn("Data integrity violation: {} - RequestId: {}, Path: {}", errorMessage, requestId, path);

        ApiError error = ApiError.builder(errorCode, errorMessage).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.CONFLICT, error);
    }
}

