# Mandated Application Layer Templates

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.1
**Date:** 2025-01
**Status:** Approved

---

## Overview

Templates for the **Application Layer** module (`{service}-application`). Provides REST API endpoints with CQRS compliance.

## Lombok Usage in Application Layer

**MANDATORY**: Use Lombok for all DTOs and recommended for controllers in this layer.

**Controllers (Command/Query)**:

- Use `@Slf4j` for logging instead of manual logger declaration
- Use `@RequiredArgsConstructor` for dependency injection (fields marked `final`)
- Use `@RestController`, `@RequestMapping`, `@Tag` for REST API configuration

**DTOs (Command/Query/Result)**:

- Use `@Getter` / `@Setter` for field accessors
- Use `@Builder` for complex object construction (optional, simpler than application-service DTOs)
- Use `@NoArgsConstructor` for Jackson deserialization (required for request DTOs)
- Use `@AllArgsConstructor` for builder pattern support
- Use Jakarta Validation annotations (`@NotNull`, `@NotBlank`, etc.)

**Mappers**:

- Use `@Component` for Spring bean registration
- Use `@RequiredArgsConstructor` if mapper has dependencies

**Example:**

```java
// DTO with Lombok
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateConsignmentCommandDTO {
    @NotNull(message = "Batch number is required")
    private String batchNumber;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
}

// Controller with Lombok
@Slf4j
@RestController
@RequestMapping("/api/v1/stock/consignments")
@Tag(name = "Stock Consignment Commands")
@RequiredArgsConstructor
public class StockConsignmentCommandController {
    private final CreateConsignmentCommandHandler commandHandler;
    private final StockConsignmentDTOMapper mapper;

    // endpoint methods...
}
```

---

## Package Structure

The Application Layer module (`{service}-application`) follows a strict package structure to enforce CQRS separation and anti-corruption layer:

```
com.ccbsa.wms.{service}.application/
├── command/                           # Command controllers (write endpoints)
│   └── {DomainObject}CommandController.java
├── query/                             # Query controllers (read endpoints)
│   └── {DomainObject}QueryController.java
├── dto/                               # DTOs and mappers
│   ├── command/                       # Command DTOs
│   │   ├── {Action}{DomainObject}CommandDTO.java
│   │   └── {Action}{DomainObject}ResultDTO.java
│   ├── query/                         # Query DTOs
│   │   └── {DomainObject}QueryResultDTO.java
│   └── mapper/                        # DTO mappers
│       └── {DomainObject}DTOMapper.java
└── exception/                         # Exception handlers
    └── GlobalExceptionHandler.java
```

**Package Naming Convention:**

- Base package: `com.ccbsa.wms.{service}.application`
- Replace `{service}` with actual service name (e.g., `stock`, `location`, `product`)
- Replace `{DomainObject}` with actual domain object name (e.g., `StockConsignment`, `Location`, `Product`)

**Package Responsibilities:**

| Package       | Responsibility      | Contains                                                                                             |
|---------------|---------------------|------------------------------------------------------------------------------------------------------|
| `command`     | Command controllers | REST endpoints for write operations (POST, PUT, DELETE), annotated with `@RestController` and `@Tag` |
| `query`       | Query controllers   | REST endpoints for read operations (GET), annotated with `@RestController` and `@Tag`                |
| `dto.command` | Command DTOs        | Request and response DTOs for command endpoints                                                      |
| `dto.query`   | Query DTOs          | Response DTOs for query endpoints                                                                    |
| `dto.mapper`  | DTO mappers         | Mappers converting between DTOs and domain objects, annotated with `@Component`                      |
| `exception`   | Exception handlers  | Global exception handler for consistent error responses, annotated with `@RestControllerAdvice`      |

**Important Package Rules:**

- **CQRS separation**: Command and query controllers in separate packages
- **Anti-corruption layer**: DTOs protect domain from external API changes
- **No domain exposure**: Controllers never expose domain entities directly
- **Security**: All endpoints annotated with `@PreAuthorize` for authorization
- **Documentation**: All endpoints annotated with `@Operation` for OpenAPI

---

## Important Domain Driven Design, Clean Hexagonal Architecture principles, CQRS and Event Driven Design Notes

**Domain-Driven Design (DDD) Principles:**

- Application layer is the entry point for external clients
- DTOs form the anti-corruption layer protecting domain
- Controllers delegate to application service handlers
- No business logic in controllers

**Clean Hexagonal Architecture Principles:**

- Application layer is the outermost layer (adapter)
- Controllers adapt HTTP requests to application service calls
- DTOs isolate external API contracts from domain
- Mappers convert between external and internal representations

**CQRS Principles:**

- **Command controllers**: Handle write operations, return command results
- **Query controllers**: Handle read operations, return query results
- **Separation**: Commands and queries use different controllers and DTOs
- **Optimization**: Query DTOs optimized for read operations

**Event-Driven Design Principles:**

- Controllers trigger commands that publish events
- Events processed asynchronously by event handlers
- Event correlation tracked through request headers
- Correlation ID extracted from X-Correlation-Id header and set in CorrelationContext via interceptor
- Idempotency handled at application service layer

---

## Command Controller Template

```java
package com.ccbsa.wms.{service}.application.command;

import com.ccbsa.wms.{service}.application.service.command.{Action}{DomainObject}CommandHandler;
import com.ccbsa.wms.{service}.application.service.command.dto.{Action}{DomainObject}Command;
import com.ccbsa.wms.{service}.application.service.command.dto.{Action}{DomainObject}Result;
import com.ccbsa.wms.{service}.application.dto.command.{Action}{DomainObject}CommandDTO;
import com.ccbsa.wms.{service}.application.dto.command.{Action}{DomainObject}ResultDTO;
import com.ccbsa.wms.{service}.application.dto.mapper.{DomainObject}DTOMapper;
import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/{service}/{domain-objects}")
@Tag(name = "{DomainObject} Commands", description = "{DomainObject} command operations")
public class {DomainObject}CommandController {
    
    private final {Action}{DomainObject}CommandHandler commandHandler;
    private final {DomainObject}DTOMapper mapper;
    
    @PostMapping("/{id}/{action}")
    @Operation(summary = "{Action} {DomainObject}", description = "{Action description}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<{Action}{DomainObject}ResultDTO>> {action}{DomainObject}(
            @PathVariable String id,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody(required = false) {Action}{DomainObject}CommandDTO commandDTO
    ) {
        // Map DTO to command
        {Action}{DomainObject}Command command = mapper.toCommand(
            commandDTO != null ? commandDTO : {Action}{DomainObject}CommandDTO.builder().build(),
            id,
            tenantId
        );
        
        // Execute command
        {Action}{DomainObject}Result result = commandHandler.handle(command);
        
        // Map result to DTO and wrap in ApiResponse
        {Action}{DomainObject}ResultDTO dto = mapper.toDTO(result);
        return ApiResponseBuilder.ok(dto);
    }
    
    @PostMapping
    @Operation(summary = "Create {DomainObject}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<Create{DomainObject}ResultDTO>> create{DomainObject}(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody Create{DomainObject}CommandDTO commandDTO
    ) {
        Create{DomainObject}Command command = mapper.toCreateCommand(commandDTO, tenantId);
        Create{DomainObject}Result result = commandHandler.handle(command);
        Create{DomainObject}ResultDTO dto = mapper.toDTO(result);
        return ApiResponseBuilder.created(dto);
    }
}
```

## Query Controller Template

```java
package com.ccbsa.wms.{service}.application.query;

import com.ccbsa.wms.{service}.application.service.query.Get{DomainObject}QueryHandler;
import com.ccbsa.wms.{service}.application.service.query.dto.{DomainObject}QueryResult;
import com.ccbsa.wms.{service}.application.dto.query.{DomainObject}QueryResultDTO;
import com.ccbsa.wms.{service}.application.dto.mapper.{DomainObject}DTOMapper;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/{service}/{domain-objects}")
@Tag(name = "{DomainObject} Queries", description = "{DomainObject} query operations")
public class {DomainObject}QueryController {
    
    private final Get{DomainObject}QueryHandler queryHandler;
    private final {DomainObject}DTOMapper mapper;
    
    @GetMapping("/{id}")
    @Operation(summary = "Get {DomainObject} by ID")
    @PreAuthorize("hasRole('VIEWER')")
    public ResponseEntity<ApiResponse<{DomainObject}QueryResultDTO>> get{DomainObject}(
            @PathVariable String id,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        Get{DomainObject}Query query = mapper.toQuery(id, tenantId);
        {DomainObject}QueryResult result = queryHandler.handle(query);
        {DomainObject}QueryResultDTO dto = mapper.toDTO(result);
        return ApiResponseBuilder.ok(dto);
    }
    
    @GetMapping
    @Operation(summary = "List {DomainObject}s")
    @PreAuthorize("hasRole('VIEWER')")
    public ResponseEntity<ApiResponse<List{DomainObject}sQueryResultDTO>> list{DomainObject}s(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String status
    ) {
        List{DomainObject}sQuery query = mapper.toListQuery(tenantId, status);
        List{DomainObject}sQueryResult result = queryHandler.handle(query);
        List{DomainObject}sQueryResultDTO dto = mapper.toDTO(result);
        return ApiResponseBuilder.ok(dto);
    }
}
```

## DTO Mapper Template

```java
package com.ccbsa.wms.{service}.application.dto.mapper;

import com.ccbsa.wms.{service}.application.service.command.dto.{Action}{DomainObject}Command;
import com.ccbsa.wms.{service}.application.service.command.dto.{Action}{DomainObject}Result;
import com.ccbsa.wms.{service}.application.dto.command.{Action}{DomainObject}CommandDTO;
import com.ccbsa.wms.{service}.application.dto.command.{Action}{DomainObject}ResultDTO;
import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import com.ccbsa.common.domain.valueobject.TenantId;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class {DomainObject}DTOMapper {
    
    public {Action}{DomainObject}Command toCommand(
            {Action}{DomainObject}CommandDTO dto,
            String id,
            String tenantId
    ) {
        return {Action}{DomainObject}Command.builder()
            .{domainObject}Id({DomainObject}Id.of(UUID.fromString(id)))
            .tenantId(TenantId.of(tenantId))
            .{attribute}(dto != null ? dto.get{Attribute}() : null)
            .build();
    }
    
    public {Action}{DomainObject}ResultDTO toDTO({Action}{DomainObject}Result result) {
        return {Action}{DomainObject}ResultDTO.builder()
            .id(result.get{DomainObject}Id().getValueAsString())
            .status(result.getStatus().name())
            .build();
    }
}
```

## Global Exception Handler Template

**Important:** Services should extend `BaseGlobalExceptionHandler` from `common-application` to avoid code duplication. The base handler provides common exception handling for all
services.

```java
package com.ccbsa.wms.{service}.application.exception;

import com.ccbsa.wms.{service}.domain.core.exception.{DomainObject}NotFoundException;
import com.ccbsa.common.application.api.ApiError;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.application.api.RequestContext;
import com.ccbsa.common.application.api.exception.BaseGlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global Exception Handler: GlobalExceptionHandler
 * 
 * Handles exceptions across all controllers and provides consistent error responses
 * using the standardized ApiResponse format.
 * 
 * <p>This handler extends {@link BaseGlobalExceptionHandler} to inherit common exception
 * handling and adds service-specific exception handlers.</p>
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
     * Handles service-specific domain exceptions.
     * 
     * @param ex The domain-specific exception
     * @param request The HTTP request
     * @return Error response
     */
    @ExceptionHandler({DomainObject}NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle{DomainObject}NotFound(
            {DomainObject}NotFoundException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);
        
        logger.warn("{DomainObject} not found: {} - RequestId: {}, Path: {}", 
                ex.getMessage(), requestId, path);
        
        ApiError error = ApiError.builder("RESOURCE_NOT_FOUND", ex.getMessage())
                .path(path)
                .requestId(requestId)
                .build();
        return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error);
    }
    
    // Add additional service-specific exception handlers here as needed
}
```

**Benefits of Extending BaseGlobalExceptionHandler:**

- ✅ Avoids code duplication (DRY principle)
- ✅ Consistent error handling across all services
- ✅ Automatic handling of common exceptions
- ✅ Services can add service-specific handlers
- ✅ Centralized maintenance of common exception logic

---

## Traceability Requirements

**Correlation ID Handling:**

1. **Request Interceptor**: Extract X-Correlation-Id header from incoming requests
2. **Context Setting**: Set correlation ID in `CorrelationContext` (ThreadLocal) for request lifecycle
3. **Event Publishing**: Correlation ID automatically available via `CorrelationContext` for event publishers
4. **Context Cleanup**: Clear `CorrelationContext` after request completion to prevent memory leaks

**Implementation Checklist:**

- [ ] Request interceptor configured to extract X-Correlation-Id header
- [ ] Correlation ID set in `CorrelationContext` at request entry point
- [ ] Correlation context cleared after request completion
- [ ] Correlation ID available to event publishers via `CorrelationContext`
- [ ] **Controllers use Lombok** (`@Slf4j`, `@RequiredArgsConstructor`)
- [ ] **All DTOs use Lombok** (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`)
- [ ] **Mappers use `@Component` and optionally `@RequiredArgsConstructor`**
- [ ] Request DTOs have `@NoArgsConstructor` for Jackson deserialization

---

**Document Control**

- **Version History:**
    - v1.1 (2025-01) - Added comprehensive Lombok usage guidelines for DTOs, controllers, and mappers
    - v1.0 (2025-01) - Initial template creation
- **Review Cycle:** Review when application layer patterns change

