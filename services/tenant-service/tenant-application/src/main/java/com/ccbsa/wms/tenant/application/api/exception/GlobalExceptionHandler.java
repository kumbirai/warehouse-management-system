package com.ccbsa.wms.tenant.application.api.exception;

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

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global Exception Handler: GlobalExceptionHandler
 * <p>
 * Handles exceptions across all controllers and provides consistent error responses
 * using the standardized ApiResponse format.
 *
 * <p>This handler extends {@link BaseGlobalExceptionHandler} to inherit common exception
 * handling and can add tenant-service-specific exception handlers if needed.</p>
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
     * Handles authorization denied exceptions from Spring Security method security.
     * Returns 403 Forbidden instead of 500 Internal Server Error.
     *
     * @param ex      The authorization exception
     * @param request The HTTP request
     * @return Error response with 403 Forbidden
     */
    @ExceptionHandler( {AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDenied(Exception ex,
                                                                       HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Access denied - RequestId: {}, Path: {}, Message: {}",
                requestId,
                path,
                ex.getMessage());

        ApiError error = ApiError.builder("ACCESS_DENIED",
                        "Access denied. Insufficient permissions to perform this operation.")
                .path(path)
                .requestId(requestId)
                .build();
        return ApiResponseBuilder.error(HttpStatus.FORBIDDEN,
                error);
    }
}

