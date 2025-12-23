package com.ccbsa.wms.notification.application.api.exception;

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
import com.ccbsa.wms.notification.application.service.exception.NotificationNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global Exception Handler: GlobalExceptionHandler
 * <p>
 * Handles exceptions across all controllers and provides consistent error responses using the standardized ApiResponse format.
 * <p>
 * This handler extends {@link BaseGlobalExceptionHandler} to inherit common exception handling and adds notification-service-specific exception handlers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles NotificationNotFoundException - indicates notification does not exist.
     *
     * @param ex      The exception
     * @param request The HTTP request
     * @return Error response with 404 Not Found
     */
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotificationNotFoundException(NotificationNotFoundException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Notification not found: {} - RequestId: {}, Path: {}", ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("NOTIFICATION_NOT_FOUND", ex.getMessage()).path(path).requestId(requestId).build();
        return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error);
    }
}

