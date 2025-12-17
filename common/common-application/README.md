# Common Application Module

This module provides shared application layer utilities and base classes for all services in the Warehouse Management
System.

## Standardized API Response

All backend services MUST use the standardized `ApiResponse<T>` wrapper to ensure consistent frontend consumption.

### Core Classes

- **`ApiResponse<T>`** - Standardized response wrapper for all REST API responses
- **`ApiError`** - Standardized error response structure
- **`ApiMeta`** - Metadata for responses (pagination, etc.)
- **`ApiResponseBuilder`** - Utility class for building consistent responses
- **`RequestContext`** - Utility for extracting request metadata (request ID, path, etc.)
- **`BaseGlobalExceptionHandler`** - Base exception handler for all services (extends to avoid DRY violations)

### Usage Examples

#### Success Responses

```java
// Simple success response
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@PathVariable String id) {
    TenantResponse data = // ... fetch tenant
    return ApiResponseBuilder.ok(data);
}

// Success response with links (HATEOAS)
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@PathVariable String id) {
    TenantResponse data = // ... fetch tenant
    Map<String, String> links = Map.of(
        "self", "/api/v1/tenants/" + id,
        "activate", "/api/v1/tenants/" + id + "/activate"
    );
    return ApiResponseBuilder.ok(data, links);
}

// Success response with pagination
@GetMapping
public ResponseEntity<ApiResponse<List<TenantResponse>>> listTenants(
        @RequestParam int page, @RequestParam int size) {
    List<TenantResponse> data = // ... fetch tenants
    long totalElements = // ... get total count
    ApiMeta.Pagination pagination = ApiMeta.Pagination.of(page, size, totalElements);
    ApiMeta meta = ApiMeta.builder().pagination(pagination).build();
    return ApiResponseBuilder.ok(data, null, meta);
}

// Created response
@PostMapping
public ResponseEntity<ApiResponse<CreateTenantResponse>> createTenant(
        @RequestBody CreateTenantRequest request) {
    CreateTenantResponse data = // ... create tenant
    return ApiResponseBuilder.created(data);
}

// No content response (204)
@PutMapping("/{id}/activate")
public ResponseEntity<ApiResponse<Void>> activateTenant(@PathVariable String id) {
    // ... activate tenant
    return ApiResponseBuilder.noContent();
}
```

#### Error Responses

```java
// In exception handlers
@ExceptionHandler(EntityNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleNotFound(
        EntityNotFoundException ex, HttpServletRequest request) {
    ApiError error = ApiError.builder("RESOURCE_NOT_FOUND", ex.getMessage())
            .path(RequestContext.getRequestPath(request))
            .requestId(RequestContext.getRequestId(request))
            .build();
    return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error);
}

// Error with details
@ExceptionHandler(ValidationException.class)
public ResponseEntity<ApiResponse<Void>> handleValidation(
        ValidationException ex, HttpServletRequest request) {
    Map<String, Object> details = Map.of(
        "field", ex.getField(),
        "rejectedValue", ex.getRejectedValue()
    );
    ApiError error = ApiError.builder("VALIDATION_ERROR", ex.getMessage())
            .details(details)
            .path(RequestContext.getRequestPath(request))
            .requestId(RequestContext.getRequestId(request))
            .build();
    return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
}
```

### Response Format

#### Success Response

```json
{
  "data": { ... },
  "links": { ... },
  "meta": { ... }
}
```

#### Error Response

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": { ... },
    "timestamp": "2025-11-15T10:30:00Z",
    "path": "/api/v1/resource",
    "requestId": "req-123"
  }
}
```

### Request Context

The `RequestContext` utility class provides methods to extract request metadata:

```java
// Get request ID (from header or generate new)
String requestId = RequestContext.getRequestId(request);

// Get request path
String path = RequestContext.getRequestPath(request);

// Get request method
String method = RequestContext.getRequestMethod(request);

// Get full request URL
String fullUrl = RequestContext.getFullRequestUrl(request);
```

### Error Codes

Standard error codes used across services:

- `RESOURCE_NOT_FOUND` - Resource not found
- `VALIDATION_ERROR` - Request validation failed
- `INVALID_OPERATION` - Invalid operation attempted
- `INTERNAL_SERVER_ERROR` - Unexpected server error
- `UNAUTHORIZED` - Authentication required
- `FORBIDDEN` - Insufficient permissions
- `CONFLICT` - Resource conflict (e.g., duplicate)
- `RATE_LIMIT_EXCEEDED` - Rate limit exceeded

### Dependencies

This module requires:

- Spring Boot Web (for `ResponseEntity`)
- Jackson (for JSON annotations)
- Jakarta Servlet API (for `RequestContext`)

### Integration

Add this dependency to your service's `{service}-application` module:

```xml
<dependency>
    <groupId>com.ccbsa.wms</groupId>
    <artifactId>common-application</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Exception Handling

**BaseGlobalExceptionHandler:**

All services should extend `BaseGlobalExceptionHandler` to avoid code duplication:

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {
    
    // Add service-specific exception handlers here
    @ExceptionHandler(ServiceSpecificException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceSpecific(
            ServiceSpecificException ex, HttpServletRequest request) {
        // Handle service-specific exception
    }
}
```

**Common Exceptions Handled by Base Handler:**

- `EntityNotFoundException` - Resource not found (404)
- `InvalidOperationException` - Invalid operation (400)
- `DomainException` - Generic domain exception (400)
- `IllegalArgumentException` - Validation error (400)
- `IllegalStateException` - Invalid state (400)
- `MethodArgumentNotValidException` - Spring validation (400)
- `ConstraintViolationException` - Bean validation (400)
- `Exception` - Generic catch-all (500)

### Best Practices

1. **Always use `ApiResponse<T>`** - Never return raw DTOs from controllers
2. **Use `ApiResponseBuilder`** - Use builder methods for consistency
3. **Extend `BaseGlobalExceptionHandler`** - Avoid duplicating exception handling code
4. **Include request context** - Always include path and request ID in error responses
5. **Use appropriate error codes** - Follow standard error code conventions
6. **Add error details** - Include validation errors and field-specific messages in error details
7. **Handle nulls properly** - Use `RequestContext` utility which handles null requests gracefully

### See Also

- [API Specifications](../../documentation/02-api/API_Specifications.md#standardized-response-format)
- [Service Architecture Document](../../documentation/01-architecture/Service_Architecture_Document.md#common-application-module-common-application)
- [Application Layer Templates](../../documentation/guide/@03-mandated-application-layer-templates.md)

