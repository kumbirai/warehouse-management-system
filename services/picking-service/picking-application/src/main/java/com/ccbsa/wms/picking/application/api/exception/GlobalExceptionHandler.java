package com.ccbsa.wms.picking.application.api.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccbsa.common.application.api.ApiError;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.application.api.RequestContext;
import com.ccbsa.common.application.api.exception.BaseGlobalExceptionHandler;
import com.ccbsa.wms.picking.domain.core.exception.PickingListNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Global Exception Handler: GlobalExceptionHandler
 * <p>
 * Handles exceptions across all controllers and provides consistent error responses using the standardized ApiResponse format.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {
    private final Environment environment;

    @ExceptionHandler(PickingListNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePickingListNotFoundException(PickingListNotFoundException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Picking list not found: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("PICKING_LIST_NOT_FOUND", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error);
    }

    /**
     * Handles RuntimeException to extract root cause and provide detailed error information.
     * <p>
     * Extracts the root cause from the exception chain and includes it in the error response
     * for better debugging. In development, includes root cause message in details.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        // Extract root cause from exception chain
        Throwable rootCause = ex;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        // Build error message with root cause information
        String errorMessage = ex.getMessage();
        if (rootCause != ex && rootCause.getMessage() != null && !rootCause.getMessage().equals(ex.getMessage())) {
            errorMessage = String.format("%s. Root cause: %s", ex.getMessage(), rootCause.getMessage());
        }

        logger.error("Runtime error occurred - RequestId: {}, Path: {}, Root cause: {}", requestId, path, rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage(), ex);

        // Include root cause details in development
        Map<String, Object> details = null;
        if (isDevelopmentMode()) {
            details = new HashMap<>();
            details.put("rootCause", rootCause.getClass().getName());
            details.put("rootCauseMessage", rootCause.getMessage());
            if (rootCause != ex) {
                details.put("wrappedException", ex.getClass().getName());
            }
        }

        ApiError error = ApiError.builder("INTERNAL_SERVER_ERROR", errorMessage).details(details).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, error);
    }

    /**
     * Checks if the application is running in development mode.
     * <p>
     * This is determined by checking if the active profile includes "dev" or "development".
     */
    private boolean isDevelopmentMode() {
        if (environment == null) {
            return false;
        }
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (profile.contains("dev") || profile.contains("development")) {
                return true;
            }
        }
        // If no profiles are set, assume development for better error visibility
        return activeProfiles.length == 0;
    }
}

