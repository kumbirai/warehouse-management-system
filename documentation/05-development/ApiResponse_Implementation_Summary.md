# ApiResponse Standardization Implementation Summary

**Date:** 2025-01  
**Status:** Completed  
**Version:** 1.0

---

## Overview

This document summarizes the implementation of the standardized `ApiResponse` structure across the Warehouse Management System to ensure consistent frontend consumption of backend
services.

## Implementation Details

### 1. Core Classes Created

#### `BaseGlobalExceptionHandler`

- Base exception handler for all services
- Handles common exceptions: `EntityNotFoundException`, `InvalidOperationException`, `DomainException`, validation exceptions, etc.
- Services extend this class to avoid code duplication (DRY principle)
- Located in: `common/common-application/src/main/java/com/ccbsa/common/application/api/exception/BaseGlobalExceptionHandler.java`

#### `ApiResponse<T>`

- Standardized response wrapper for all REST API responses
- Supports success responses with `data`, `links`, and `meta` fields
- Supports error responses with `error` field
- Provides `isSuccess()` and `isError()` helper methods
- Located in: `common/common-application/src/main/java/com/ccbsa/common/application/api/ApiResponse.java`

#### `ApiError`

- Standardized error response structure
- Builder pattern for flexible error construction
- Includes: `code`, `message`, `details`, `timestamp`, `path`, `requestId`
- Located in: `common/common-application/src/main/java/com/ccbsa/common/application/api/ApiError.java`

#### `ApiMeta`

- Metadata container for responses
- Supports pagination information
- Extensible for future metadata needs
- Located in: `common/common-application/src/main/java/com/ccbsa/common/application/api/ApiMeta.java`

#### `ApiResponseBuilder`

- Utility class for building consistent responses
- Provides methods for common HTTP status codes (200, 201, 202, 204)
- Error response builders with validation
- Located in: `common/common-application/src/main/java/com/ccbsa/common/application/api/ApiResponseBuilder.java`

#### `RequestContext`

- Utility class for extracting request metadata
- Extracts request ID from headers (X-Request-Id, X-Correlation-Id)
- Provides request path, method, query string, and full URL
- Handles null requests gracefully
- Located in: `common/common-application/src/main/java/com/ccbsa/common/application/api/RequestContext.java`

### 2. Code Quality Improvements

#### Null Safety

- Added null checks in `ApiResponse.error()` method
- Added null checks in `ApiResponseBuilder.error()` methods
- `RequestContext` methods handle null requests gracefully
- `ApiError.Builder` validates required fields (code, message)

#### Validation

- `ApiError.Builder` throws `IllegalStateException` if code or message is null/empty
- Input validation in builder methods
- Clear error messages for validation failures

#### Error Handling

- Enhanced `GlobalExceptionHandler` with:
    - `MethodArgumentNotValidException` handler for Spring validation errors
    - `ConstraintViolationException` handler for Bean Validation errors
    - Proper error details extraction
    - Request context inclusion in all errors

### 3. Documentation Updates

#### API Specifications (`documentation/02-api/API_Specifications.md`)

- Added comprehensive "Standardized Response Format" section
- Documented success and error response formats
- Provided implementation requirements for backend and frontend
- Added code examples for both Java and TypeScript

#### Service Architecture Document (`documentation/01-architecture/Service_Architecture_Document.md`)

- Updated Common Application Module section
- Added ApiResponse classes to module contents
- Added usage examples and requirements

#### Application Layer Templates (`documentation/guide/@03-mandated-application-layer-templates.md`)

- Updated all controller templates to use `ApiResponse<T>`
- Updated exception handler template to use `ApiError` and `RequestContext`
- Removed manual request ID extraction in favor of `RequestContext` utility

#### README (`common/common-application/README.md`)

- Comprehensive usage guide
- Code examples for all response types
- Best practices section
- Error code conventions

### 4. Code Updates

#### Tenant Service

- Updated `TenantCommandController` to use `ApiResponse<T>`
- Updated `TenantQueryController` to use `ApiResponse<T>`
- Refactored `GlobalExceptionHandler` to extend `BaseGlobalExceptionHandler`:
    - Removed duplicate exception handling code
    - Inherits common exception handlers from base class
    - Can add service-specific handlers if needed
- Added `common-application` dependency to `tenant-application/pom.xml`

#### Common Application Module

- Added `BaseGlobalExceptionHandler` to provide common exception handling
- Added Jakarta Validation API dependency for `ConstraintViolationException` support

### 5. Dependencies

#### Added to `common-application/pom.xml`

- `spring-boot-starter-web` - For `ResponseEntity` and HTTP support
- `jackson-annotations` - For JSON serialization annotations

## Response Format Standards

### Success Response

```json
{
  "data": { ... },
  "links": { ... },
  "meta": { ... }
}
```

### Error Response

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

## Standard Error Codes

- `RESOURCE_NOT_FOUND` - Resource not found
- `VALIDATION_ERROR` - Request validation failed
- `INVALID_OPERATION` - Invalid operation attempted
- `INTERNAL_SERVER_ERROR` - Unexpected server error
- `UNAUTHORIZED` - Authentication required
- `FORBIDDEN` - Insufficient permissions
- `CONFLICT` - Resource conflict (e.g., duplicate)
- `RATE_LIMIT_EXCEEDED` - Rate limit exceeded

## Usage Requirements

### Backend Services MUST:

1. Use `ApiResponse<T>` wrapper for all REST API responses
2. Use `ApiResponseBuilder` utility class for consistent response creation
3. Use `ApiError` class for all error responses
4. Include error details in exception handlers using `ApiError.builder()`
5. Return `ResponseEntity<ApiResponse<T>>` from all controller methods
6. Use `RequestContext` utility for extracting request metadata

### Frontend Applications MUST:

1. Expect `ApiResponse<T>` format for all API responses
2. Check `isError()` or `isSuccess()` methods to determine response type
3. Access data via `response.data` for success responses
4. Access error details via `response.error` for error responses
5. Handle pagination via `response.meta.pagination` when present

## Migration Guide

### For Existing Services

1. **Add Dependency**
   ```xml
   <dependency>
       <groupId>com.ccbsa.wms</groupId>
       <artifactId>common-application</artifactId>
       <version>${project.version}</version>
   </dependency>
   ```

2. **Update Controllers**
    - Change return type from `ResponseEntity<DTO>` to `ResponseEntity<ApiResponse<DTO>>`
    - Use `ApiResponseBuilder.ok(data)` instead of `ResponseEntity.ok(data)`
    - Use `ApiResponseBuilder.created(data)` for 201 responses
    - Use `ApiResponseBuilder.noContent()` for 204 responses

3. **Update Exception Handlers**
    - Use `ApiError.builder()` to create errors
    - Use `RequestContext.getRequestId(request)` and `RequestContext.getRequestPath(request)`
    - Use `ApiResponseBuilder.error()` to return error responses
    - Add handlers for validation exceptions

## Testing

### Unit Testing Examples

```java
@Test
void testSuccessResponse() {
    TenantResponse data = new TenantResponse(...);
    ResponseEntity<ApiResponse<TenantResponse>> response = ApiResponseBuilder.ok(data);
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isSuccess()).isTrue();
    assertThat(response.getBody().getData()).isEqualTo(data);
}

@Test
void testErrorResponse() {
    ApiError error = ApiError.builder("ERROR_CODE", "Error message").build();
    ResponseEntity<ApiResponse<Void>> response = ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isError()).isTrue();
    assertThat(response.getBody().getError()).isEqualTo(error);
}
```

## Next Steps

1. **Update All Services** - Migrate all existing services to use `ApiResponse<T>`
2. **Frontend Integration** - Update frontend to expect standardized format
3. **API Gateway** - Ensure API Gateway preserves response format
4. **Monitoring** - Add metrics for error response types
5. **Documentation** - Update OpenAPI/Swagger documentation with response schemas

## References

- [API Specifications](../02-api/API_Specifications.md#standardized-response-format)
- [Service Architecture Document](../01-architecture/Service_Architecture_Document.md#common-application-module-common-application)
- [Application Layer Templates](../guide/@03-mandated-application-layer-templates.md)
- [Common Application README](../../common/common-application/README.md)

---

**Document Control**

- **Version:** 1.0
- **Last Updated:** 2025-01
- **Status:** Completed
- **Next Review:** When new services are added or response format changes

