# Mandated Application Service Templates

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.1
**Date:** 2025-01
**Status:** Approved
**Related Documents:**

- [Mandated Implementation Template Guide](mandated-Implementation-template-guide.md)
- [Service Architecture Document](../01-project-planning/architecture/Service_Architecture_Document.md)

---

## Overview

This document provides comprehensive code templates for the **Application Service** module (`{service}-domain/{service}-application-service`). This layer orchestrates use cases and
defines port interfaces.

**Key Principles:**

- Use case handlers (one handler per use case)
- Port interfaces (repository and service ports)
- CQRS separation (command vs query handlers)
- Event publishing after successful commits
- Transaction management (one transaction per use case)
- **Lombok recommended for Command/Query/Result DTOs to reduce boilerplate**

## Lombok Usage in Application Service Layer

**RECOMMENDED**: Use Lombok for all DTOs (Commands, Queries, Results) in this layer.

**Handlers (Command/Query)**:

- Use `@Slf4j` for logging instead of manual logger declaration
- Use `@RequiredArgsConstructor` for dependency injection (fields marked `final`)

**DTOs (Commands, Queries, Results)**:

- Use `@Getter` for field accessors
- Use `@Builder` for fluent object construction
- Use `@ToString` for debugging (avoid for sensitive data)
- Use `@EqualsAndHashCode` for value comparison (if needed)

**Anti-Patterns to Avoid:**

- `@Data` - Too broad, prefer specific annotations
- `@Value` - Only for truly immutable value objects
- Avoid Lombok if DTOs contain complex validation logic

---

## Package Structure

The Application Service module (`{service}-domain/{service}-application-service`) follows a strict package structure to enforce CQRS and port/adapter patterns:

```
com.ccbsa.wms.{service}.application.service/
├── command/                           # Command handlers (write operations)
│   ├── {Action}{DomainObject}CommandHandler.java
│   └── dto/                           # Command DTOs
│       ├── {Action}{DomainObject}Command.java
│       └── {Action}{DomainObject}Result.java
├── query/                             # Query handlers (read operations)
│   ├── Get{DomainObject}QueryHandler.java
│   ├── List{DomainObject}sQueryHandler.java
│   └── dto/                           # Query DTOs
│       ├── Get{DomainObject}Query.java
│       ├── {DomainObject}QueryResult.java
│       └── {DomainObject}View.java
└── port/                              # Port interfaces (contracts)
    ├── repository/                    # Repository ports (aggregate persistence)
    │   └── {DomainObject}Repository.java
    ├── data/                          # Data ports (read model access)
    │   └── {DomainObject}ViewRepository.java
    ├── service/                      # Service ports (external integrations)
    │   └── {ExternalSystem}Service.java
    └── messaging/                     # Event publisher ports
        └── {Service}EventPublisher.java
```

**Package Naming Convention:**

- Base package: `com.ccbsa.wms.{service}.application.service`
- Replace `{service}` with actual service name (e.g., `stock`, `location`, `product`)
- Replace `{DomainObject}` with actual domain object name (e.g., `StockConsignment`, `Location`, `Product`)

**Package Responsibilities:**

| Package           | Responsibility        | Contains                                                                                                 |
|-------------------|-----------------------|----------------------------------------------------------------------------------------------------------|
| `command`         | Command handlers      | Use case handlers for write operations, annotated with `@Component` and `@Transactional`                 |
| `command.dto`     | Command DTOs          | Command objects and result objects for write operations                                                  |
| `query`           | Query handlers        | Use case handlers for read operations, annotated with `@Component` and `@Transactional(readOnly = true)` |
| `query.dto`       | Query DTOs            | Query objects, result objects, and view objects for read operations                                      |
| `port.repository` | Repository ports      | Interfaces for aggregate persistence (write model), implemented by data access adapters                  |
| `port.data`       | Data ports            | Interfaces for read model access (projections/views), implemented by data access adapters                |
| `port.service`    | Service ports         | Interfaces for external service integrations, implemented by infrastructure adapters                     |
| `port.messaging`  | Event publisher ports | Interfaces for event publishing, implemented by messaging adapters                                       |

**Important Package Rules:**

- **Port interfaces** are defined here, NOT in domain core
- **Command handlers** use repository ports (write model)
- **Query handlers** use data ports (read model)
- **Ports** define contracts that infrastructure adapters implement
- **Dependency direction**: Application Service → Data Access (proper hexagonal architecture)

---

## Important Domain Driven Design, Clean Hexagonal Architecture principles, CQRS and Event Driven Design Notes

**Domain-Driven Design (DDD) Principles:**

- Application services orchestrate use cases
- Use cases coordinate domain objects and infrastructure
- Application services are thin - delegate to domain for business logic
- One use case handler per business operation

**Clean Hexagonal Architecture Principles:**

- Port interfaces defined in application service layer
- Adapters implement ports in infrastructure layers
- Dependency inversion: high-level depends on abstractions (ports)
- Application service layer defines contracts

**CQRS Principles:**

- **Command handlers**: Use repository ports for aggregate persistence (write model)
- **Query handlers**: Use data ports for read model access (eventual consistency)
- **Separation**: Commands and queries use different ports and models
- **Optimization**: Read models denormalized for query performance

**Event-Driven Design Principles:**

- Events published after successful transaction commit
- Event publisher ports abstract messaging infrastructure
- Events enable eventual consistency between write and read models
- Event handlers process events asynchronously
- Correlation ID available via `CorrelationContext` for event publishing
- Event publishers automatically inject correlation ID into event metadata

---

## Table of Contents

1. [Command Handler Template](#command-handler-template)
2. [Query Handler Template](#query-handler-template)
3. [Repository Port Template](#repository-port-template)
4. [Service Port Template](#service-port-template)
5. [Event Publisher Port Template](#event-publisher-port-template)
6. [Command/Query DTO Templates](#commandquery-dto-templates)
7. [Common Patterns](#common-patterns)

---

## Command Handler Template

### Complete Command Handler Template

```java
package com.ccbsa.wms.{service}.application.service.command;

import com.ccbsa.wms.{service}.application.service.port.repository.{DomainObject}Repository;
import com.ccbsa.wms.{service}.application.service.port.messaging.{Service}EventPublisher;
import com.ccbsa.wms.{service}.domain.core.entity.{DomainObject};
import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import com.ccbsa.wms.{service}.domain.core.exception.{DomainObject}NotFoundException;
import com.ccbsa.common.domain.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Command Handler: {Action}{DomainObject}CommandHandler
 *
 * Handles {action description} use case for {DomainObject}.
 *
 * Responsibilities:
 * - Load aggregate from repository
 * - Execute business logic via aggregate
 * - Persist aggregate changes
 * - Publish domain events
 * - Return command result
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class {Action}{DomainObject}CommandHandler {

    private final {DomainObject}Repository repository;
    private final {Service}EventPublisher eventPublisher;
    
    /**
     * Handles the {Action}{DomainObject}Command.
     * 
     * Transaction boundary: One transaction per command execution.
     * Events published after successful commit.
     * 
     * @param command Command to execute
     * @return Command result
     * @throws {DomainObject}NotFoundException if aggregate not found
     * @throws IllegalStateException if business rule violation
     */
    @Transactional
    public {Action}{DomainObject}Result handle({Action}{DomainObject}Command command) {
        // 1. Validate command
        validateCommand(command);
        
        // 2. Load aggregate
        {DomainObject} {domainObject} = repository.findById(command.get{DomainObject}Id())
            .orElseThrow(() -> new {DomainObject}NotFoundException(
                command.get{DomainObject}Id().getValueAsString(),
                "{DomainObject} not found"
            ));
        
        // 3. Execute business logic (via aggregate)
        {domainObject}.{businessAction}(command.get{Parameter}());
        
        // 4. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = List.copyOf({domainObject}.getDomainEvents());
        
        // 5. Persist aggregate
        repository.save({domainObject});
        
        // 6. Publish events after transaction commit
        // Note: Correlation ID is automatically injected by event publisher from CorrelationContext
        // Events are published using TransactionSynchronizationManager to ensure they are only
        // published after the database transaction has successfully committed. This prevents
        // race conditions where event listeners consume events before the aggregate is visible
        // in the database.
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            {domainObject}.clearDomainEvents();
        }
        
        // 7. Return command-specific result (NOT domain entity)
        return {Action}{DomainObject}Result.builder()
            .{domainObject}Id({domainObject}.getId())
            .status({domainObject}.getStatus())
            .{resultField}({domainObject}.get{Field}())
            .build();
    }
    
    /**
     * Validates command before execution.
     * 
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     * <p>
     * Events are published using TransactionSynchronizationManager to ensure they are only published after the database transaction has successfully committed. This prevents race
     * conditions where event listeners consume events before the aggregate is visible in the database.
     *
     * @param domainEvents Domain events to publish
     */
    private void publishEventsAfterCommit(List<DomainEvent<?>> domainEvents) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // No active transaction - publish immediately
            logger.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        // Register synchronization to publish events after transaction commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    logger.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    logger.error("Failed to publish domain events after transaction commit", e);
                    // Don't throw - transaction already committed, event publishing failure
                    // should be handled by retry mechanisms or dead letter queue
                }
            }
        });
    }
    
    private void validateCommand({Action}{DomainObject}Command command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.get{DomainObject}Id() == null) {
            throw new IllegalArgumentException("{DomainObject}Id is required");
        }
        // Add other validations
    }
}
```

### Create Command Handler Template

```java
package com.ccbsa.wms.{service}.application.service.command;

import com.ccbsa.wms.{service}.application.service.port.repository.{DomainObject}Repository;
import com.ccbsa.wms.{service}.application.service.port.messaging.{Service}EventPublisher;
import com.ccbsa.wms.{service}.domain.core.entity.{DomainObject};
import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Command Handler: Create{DomainObject}CommandHandler
 * 
 * Handles creation of new {DomainObject} aggregate.
 */
@Component
public class Create{DomainObject}CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(Create{DomainObject}CommandHandler.class);
    
    private final {DomainObject}Repository repository;
    private final {Service}EventPublisher eventPublisher;
    
    @Transactional
    public Create{DomainObject}Result handle(Create{DomainObject}Command command) {
        // 1. Validate command
        validateCommand(command);
        
        // 2. Create aggregate using builder
        {DomainObject} {domainObject} = {DomainObject}.builder()
            .{domainObject}Id({DomainObject}Id.generate())
            .tenantId(command.getTenantId())
            .{attribute}(command.get{Attribute}())
            .status({DomainObject}Status.CREATED)
            .build();
        
        // 3. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = List.copyOf({domainObject}.getDomainEvents());
        
        // 4. Persist aggregate
        repository.save({domainObject});
        
        // 5. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            {domainObject}.clearDomainEvents();
        }
        
        // 6. Return result
        return Create{DomainObject}Result.builder()
            .{domainObject}Id({domainObject}.getId())
            .status({domainObject}.getStatus())
            .createdAt({domainObject}.getCreatedAt())
            .build();
    }
    
    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     * <p>
     * Events are published using TransactionSynchronizationManager to ensure they are only published after the database transaction has successfully committed. This prevents race
     * conditions where event listeners consume events before the aggregate is visible in the database.
     *
     * @param domainEvents Domain events to publish
     */
    private void publishEventsAfterCommit(List<DomainEvent<?>> domainEvents) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // No active transaction - publish immediately
            logger.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        // Register synchronization to publish events after transaction commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    logger.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    logger.error("Failed to publish domain events after transaction commit", e);
                    // Don't throw - transaction already committed, event publishing failure
                    // should be handled by retry mechanisms or dead letter queue
                }
            }
        });
    }
    
    private void validateCommand(Create{DomainObject}Command command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        // Add other validations
    }
}
```

---

## Query Handler Template

### Complete Query Handler Template (Using Read Model)

```java
package com.ccbsa.wms.{service}.application.service.query;

import com.ccbsa.wms.{service}.application.service.port.data.{DomainObject}ViewRepository;
import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import com.ccbsa.wms.{service}.domain.core.exception.{DomainObject}NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Handler: Get{DomainObject}QueryHandler
 * 
 * Handles query for {DomainObject} by ID.
 * Uses read model (data port) for optimized queries.
 * Read-only transaction for query optimization.
 */
@Component
public class Get{DomainObject}QueryHandler {
    
    private final {DomainObject}ViewRepository viewRepository;
    
    /**
     * Handles the Get{DomainObject}Query.
     * 
     * Read-only transaction for query optimization.
     * 
     * @param query Query to execute
     * @return Query result
     * @throws {DomainObject}NotFoundException if not found
     */
    @Transactional(readOnly = true)
    public {DomainObject}QueryResult handle(Get{DomainObject}Query query) {
        // 1. Validate query
        validateQuery(query);
        
        // 2. Load from read model (data port) - optimized for queries
        {DomainObject}View view = viewRepository.findById(query.get{DomainObject}Id())
            .orElseThrow(() -> new {DomainObject}NotFoundException(
                query.get{DomainObject}Id().getValueAsString(),
                "{DomainObject} not found"
            ));
        
        // 3. Map to query result (read model already optimized)
        return {DomainObject}QueryResult.builder()
            .id(view.getId())
            .{attribute}(view.get{Attribute}())
            .status(view.getStatus())
            .createdAt(view.getCreatedAt())
            .lastModifiedAt(view.getLastModifiedAt())
            .build();
    }
    
    private void validateQuery(Get{DomainObject}Query query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.get{DomainObject}Id() == null) {
            throw new IllegalArgumentException("{DomainObject}Id is required");
        }
    }
}
```

### List Query Handler Template (Using Read Model)

```java
package com.ccbsa.wms.{service}.application.service.query;

import com.ccbsa.wms.{service}.application.service.port.data.{DomainObject}ViewRepository;
import com.ccbsa.common.domain.valueobject.TenantId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Handler: List{DomainObject}sQueryHandler
 * 
 * Handles query for list of {DomainObject}s with filtering.
 * Uses read model (data port) for optimized queries.
 */
@Component
public class List{DomainObject}sQueryHandler {
    
    private final {DomainObject}ViewRepository viewRepository;
    
    @Transactional(readOnly = true)
    public List{DomainObject}sQueryResult handle(List{DomainObject}sQuery query) {
        // 1. Validate query
        validateQuery(query);
        
        // 2. Load from read model (data port) - optimized for queries
        List<{DomainObject}View> views = viewRepository.findByTenantIdAndStatus(
            query.getTenantId(),
            query.getStatus()
        );
        
        // 3. Map to query results (read model already optimized)
        List<{DomainObject}QueryResult> results = views.stream()
            .map(this::toQueryResult)
            .collect(Collectors.toList());
        
        // 4. Return result with pagination info
        return List{DomainObject}sQueryResult.builder()
            .items(results)
            .totalCount(results.size())
            .build();
    }
    
    private {DomainObject}QueryResult toQueryResult({DomainObject}View view) {
        return {DomainObject}QueryResult.builder()
            .id(view.getId())
            .{attribute}(view.get{Attribute}())
            .status(view.getStatus())
            .build();
    }
    
    private void validateQuery(List{DomainObject}sQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
    }
}
```

---

## Repository Port Template

### Repository Port Interface Template

```java
package com.ccbsa.wms.{service}.application.service.port.repository;

import com.ccbsa.wms.{service}.domain.core.entity.{DomainObject};
import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import com.ccbsa.common.domain.valueobject.TenantId;
import java.util.List;
import java.util.Optional;

/**
 * Repository Port: {DomainObject}Repository
 * 
 * Defines the contract for {DomainObject} aggregate persistence (write model).
 * Implemented by data access adapters.
 * 
 * IMPORTANT: 
 * - This interface is in the application service layer, NOT in the domain core
 * - Used by COMMAND handlers for aggregate persistence
 * - For QUERY handlers, use Data Ports (port/data) instead
 * - Domain core has no infrastructure dependencies
 */
public interface {DomainObject}Repository {
    
    /**
     * Saves or updates the {DomainObject} aggregate.
     * 
     * @param {domainObject} Aggregate to save
     */
    void save({DomainObject} {domainObject});
    
    /**
     * Finds {DomainObject} by ID.
     * 
     * @param id Aggregate identifier
     * @return Optional containing aggregate if found
     */
    Optional<{DomainObject}> findById({DomainObject}Id id);
    
    /**
     * Finds {DomainObject} by tenant ID and ID.
     * 
     * @param tenantId Tenant identifier
     * @param id Aggregate identifier
     * @return Optional containing aggregate if found
     */
    Optional<{DomainObject}> findByTenantIdAndId(TenantId tenantId, {DomainObject}Id id);
    
    /**
     * Finds all {DomainObject}s by tenant ID.
     * 
     * @param tenantId Tenant identifier
     * @return List of aggregates (empty if none found)
     */
    List<{DomainObject}> findByTenantId(TenantId tenantId);
    
    /**
     * Finds {DomainObject}s by tenant ID and status.
     * 
     * @param tenantId Tenant identifier
     * @param status Status filter
     * @return List of aggregates (empty if none found)
     */
    List<{DomainObject}> findByTenantIdAndStatus(TenantId tenantId, {DomainObject}Status status);
    
    /**
     * Deletes {DomainObject} by ID.
     * 
     * @param id Aggregate identifier
     */
    void deleteById({DomainObject}Id id);
    
    /**
     * Checks if {DomainObject} exists by ID.
     * 
     * @param id Aggregate identifier
     * @return true if exists
     */
    boolean existsById({DomainObject}Id id);
}
```

---

## Data Port Template

### Data Port Interface Template (For Read Models)

```java
package com.ccbsa.wms.{service}.application.service.port.data;

import com.ccbsa.wms.{service}.application.service.query.dto.{DomainObject}View;
import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import com.ccbsa.common.domain.valueobject.TenantId;
import java.util.List;
import java.util.Optional;

/**
 * Data Port: {DomainObject}ViewRepository
 * 
 * Defines the contract for {DomainObject} read model data access.
 * Implemented by data access adapters for projections/views.
 * 
 * IMPORTANT:
 * - Used by QUERY handlers for read model access
 * - Read models are denormalized projections optimized for queries
 * - Separate from Repository Ports which are for aggregate persistence
 * - Read models updated via event projections (asynchronously)
 */
public interface {DomainObject}ViewRepository {
    
    /**
     * Finds {DomainObject} view by ID.
     * 
     * @param id View identifier
     * @return Optional containing view if found
     */
    Optional<{DomainObject}View> findById({DomainObject}Id id);
    
    /**
     * Finds {DomainObject} views by tenant ID and ID.
     * 
     * @param tenantId Tenant identifier
     * @param id View identifier
     * @return Optional containing view if found
     */
    Optional<{DomainObject}View> findByTenantIdAndId(TenantId tenantId, {DomainObject}Id id);
    
    /**
     * Finds all {DomainObject} views by tenant ID.
     * 
     * @param tenantId Tenant identifier
     * @return List of views (empty if none found)
     */
    List<{DomainObject}View> findByTenantId(TenantId tenantId);
    
    /**
     * Finds {DomainObject} views by tenant ID and status.
     * 
     * @param tenantId Tenant identifier
     * @param status Status filter
     * @return List of views (empty if none found)
     */
    List<{DomainObject}View> findByTenantIdAndStatus(TenantId tenantId, {DomainObject}Status status);
    
    /**
     * Finds {DomainObject} views with pagination.
     * 
     * @param tenantId Tenant identifier
     * @param page Page number (0-based)
     * @param size Page size
     * @return List of views
     */
    List<{DomainObject}View> findByTenantIdWithPagination(TenantId tenantId, int page, int size);
}
```

---

## Service Port Template

### External Service Port Template

```java
package com.ccbsa.wms.{service}.application.service.port.service;

import com.ccbsa.wms.{service}.domain.core.valueobject.{Parameter};
import java.util.Optional;

/**
 * Service Port: {ExternalSystem}Service
 * 
 * Defines the contract for external system integration.
 * Implemented by infrastructure adapters.
 */
public interface {ExternalSystem}Service {
    
    /**
     * {Operation description}.
     * 
     * @param {parameter} Parameter for operation
     * @return Result of operation
     * @throws {Exception} if operation fails
     */
    {Result} {operation}({Parameter} {parameter});
    
    /**
     * {Query operation description}.
     * 
     * @param {parameter} Query parameter
     * @return Optional result
     */
    Optional<{Result}> {queryOperation}({Parameter} {parameter});
}
```

---

## Event Publisher Port Template

### Event Publisher Port Template

```java
package com.ccbsa.wms.{service}.application.service.port.messaging;

import com.ccbsa.common.messaging.EventPublisher;
import com.ccbsa.wms.{service}.domain.core.event.{Service}Event;

/**
 * Event Publisher Port: {Service}EventPublisher
 * 
 * Extends common EventPublisher interface.
 * Service-specific event publishing contract.
 * Implemented by messaging adapters.
 */
public interface {Service}EventPublisher extends EventPublisher {
    
    /**
     * Publishes a {Service} domain event.
     * 
     * @param event Domain event to publish
     */
    void publish({Service}Event<?> event);
    
    /**
     * Publishes multiple {Service} domain events.
     * 
     * @param events List of domain events to publish
     */
    void publish(List<{Service}Event<?>> events);
}
```

---

## Command/Query DTO Templates

### Command DTO Template

```java
package com.ccbsa.wms.{service}.application.service.command.dto;

import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import com.ccbsa.wms.{service}.domain.core.valueobject.{Attribute};
import com.ccbsa.common.domain.valueobject.TenantId;
import java.util.Objects;

/**
 * Command DTO: {Action}{DomainObject}Command
 * 
 * Represents command to {action description}.
 */
public class {Action}{DomainObject}Command {
    
    private final {DomainObject}Id {domainObject}Id;
    private final TenantId tenantId;
    private final {Attribute} {attribute};
    
    private {Action}{DomainObject}Command(Builder builder) {
        this.{domainObject}Id = builder.{domainObject}Id;
        this.tenantId = builder.tenantId;
        this.{attribute} = builder.{attribute};
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private {DomainObject}Id {domainObject}Id;
        private TenantId tenantId;
        private {Attribute} {attribute};
        
        public Builder {domainObject}Id({DomainObject}Id {domainObject}Id) {
            this.{domainObject}Id = {domainObject}Id;
            return this;
        }
        
        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }
        
        public Builder {attribute}({Attribute} {attribute}) {
            this.{attribute} = {attribute};
            return this;
        }
        
        public {Action}{DomainObject}Command build() {
            return new {Action}{DomainObject}Command(this);
        }
    }
    
    // Getters
    
    public {DomainObject}Id get{DomainObject}Id() {
        return {domainObject}Id;
    }
    
    public TenantId getTenantId() {
        return tenantId;
    }
    
    public {Attribute} get{Attribute}() {
        return {attribute};
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        {Action}{DomainObject}Command that = ({Action}{DomainObject}Command) o;
        return Objects.equals({domainObject}Id, that.{domainObject}Id) &&
               Objects.equals(tenantId, that.tenantId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash({domainObject}Id, tenantId);
    }
}
```

### Command DTO Template (Lombok Version - RECOMMENDED)

**NOTE**: Use this Lombok-based approach for new code to reduce boilerplate.

```java
package com.ccbsa.wms.{service}.application.service.command.dto;

import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import com.ccbsa.wms.{service}.domain.core.valueobject.{Attribute};
import com.ccbsa.common.domain.valueobject.TenantId;
import lombok.Builder;
import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Command DTO: {Action}{DomainObject}Command
 *
 * Represents command to {action description}.
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class {Action}{DomainObject}Command {

    @EqualsAndHashCode.Include
    private final {DomainObject}Id {domainObject}Id;

    @EqualsAndHashCode.Include
    private final TenantId tenantId;

    private final {Attribute} {attribute};
}
```

**Key Lombok Annotations Explained:**

- `@Getter` - Generates getters for all fields
- `@Builder` - Generates builder pattern (use: `Command.builder().field(value).build()`)
- `@ToString` - Generates toString() for debugging
- `@EqualsAndHashCode` - Generates equals/hashCode based on included fields only

---

### Command Result DTO Template

```java
package com.ccbsa.wms.{service}.application.service.command.dto;

import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Command Result DTO: {Action}{DomainObject}Result
 * 
 * Represents result of {action} command execution.
 * Contains only essential information, not full domain entity.
 */
public class {Action}{DomainObject}Result {
    
    private final {DomainObject}Id {domainObject}Id;
    private final {DomainObject}Status status;
    private final LocalDateTime {timestamp};
    
    private {Action}{DomainObject}Result(Builder builder) {
        this.{domainObject}Id = builder.{domainObject}Id;
        this.status = builder.status;
        this.{timestamp} = builder.{timestamp};
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private {DomainObject}Id {domainObject}Id;
        private {DomainObject}Status status;
        private LocalDateTime {timestamp};
        
        public Builder {domainObject}Id({DomainObject}Id {domainObject}Id) {
            this.{domainObject}Id = {domainObject}Id;
            return this;
        }
        
        public Builder status({DomainObject}Status status) {
            this.status = status;
            return this;
        }
        
        public Builder {timestamp}(LocalDateTime {timestamp}) {
            this.{timestamp} = {timestamp};
            return this;
        }
        
        public {Action}{DomainObject}Result build() {
            return new {Action}{DomainObject}Result(this);
        }
    }
    
    // Getters
    
    public {DomainObject}Id get{DomainObject}Id() {
        return {domainObject}Id;
    }
    
    public {DomainObject}Status getStatus() {
        return status;
    }
    
    public LocalDateTime get{Timestamp}() {
        return {timestamp};
    }
}
```

### Command Result DTO Template (Lombok Version - RECOMMENDED)

**NOTE**: Use this Lombok-based approach for new code to reduce boilerplate.

```java
package com.ccbsa.wms.{service}.application.service.command.dto;

import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

/**
 * Command Result DTO: {Action}{DomainObject}Result
 *
 * Represents result of {action} command execution.
 * Contains only essential information, not full domain entity.
 */
@Getter
@Builder
public class {Action}{DomainObject}Result {

    private final {DomainObject}Id {domainObject}Id;
    private final {DomainObject}Status status;
    private final LocalDateTime {timestamp};
}
```

---

### Query Result DTO Template

```java
package com.ccbsa.wms.{service}.application.service.query.dto;

import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Query Result DTO: {DomainObject}QueryResult
 * 
 * Optimized read model for {DomainObject} queries.
 * Contains denormalized data for fast reads.
 */
public class {DomainObject}QueryResult {
    
    private final {DomainObject}Id id;
    private final {Attribute} {attribute};
    private final {DomainObject}Status status;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;
    
    private {DomainObject}QueryResult(Builder builder) {
        this.id = builder.id;
        this.{attribute} = builder.{attribute};
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.lastModifiedAt = builder.lastModifiedAt;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private {DomainObject}Id id;
        private {Attribute} {attribute};
        private {DomainObject}Status status;
        private LocalDateTime createdAt;
        private LocalDateTime lastModifiedAt;
        
        public Builder id({DomainObject}Id id) {
            this.id = id;
            return this;
        }
        
        public Builder {attribute}({Attribute} {attribute}) {
            this.{attribute} = {attribute};
            return this;
        }
        
        public Builder status({DomainObject}Status status) {
            this.status = status;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }
        
        public {DomainObject}QueryResult build() {
            return new {DomainObject}QueryResult(this);
        }
    }
    
    // Getters
    
    public {DomainObject}Id getId() {
        return id;
    }
    
    public {Attribute} get{Attribute}() {
        return {attribute};
    }
    
    public {DomainObject}Status getStatus() {
        return status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }
}
```

---

## Common Patterns

### Transactional Outbox Pattern (Optional)

For guaranteed event publishing:

```java
@Component
public class {Action}{DomainObject}CommandHandler {
    
    private final {DomainObject}Repository repository;
    private final OutboxRepository outboxRepository;
    
    @Transactional
    public {Action}{DomainObject}Result handle({Action}{DomainObject}Command command) {
        // 1-3. Load, execute, persist (same as before)
        
        // 4. Store events in outbox (same transaction)
        List<DomainEvent<?>> domainEvents = {domainObject}.getDomainEvents();
        if (!domainEvents.isEmpty()) {
            outboxRepository.saveEvents(domainEvents);
            {domainObject}.clearDomainEvents();
        }
        
        // 5. Return result
        // Events published asynchronously by outbox processor
    }
}
```

---

## Port Usage Guidelines

### CQRS Port Separation

**Command Handlers:**

- Use **Repository Ports** (`port/repository`) for aggregate persistence
- Use **Event Publisher Ports** (`port/messaging`) for event publishing
- Do NOT use Data Ports (those are for queries)

**Query Handlers:**

- **Primary:** Use **Data Ports** (`port/data`) for read model access (eventual consistency)
- **Exception:** Use **Repository Ports** (`port/repository`) only when immediate consistency is required
- Read models are denormalized projections optimized for queries
- Most queries can tolerate eventual consistency (use data ports)
- Critical queries requiring immediate consistency may query write model (use repository ports)

**Application Services:**

- Use **Service Ports** (`port/service`) for external service calls
- Use appropriate ports based on operation type (command vs query)

### Port Placement Summary

| Port Type             | Package           | Used By              | Purpose                             |
|-----------------------|-------------------|----------------------|-------------------------------------|
| Repository Ports      | `port.repository` | Command handlers     | Aggregate persistence (write model) |
| Data Ports            | `port.data`       | Query handlers       | Read model queries (projections)    |
| Service Ports         | `port.service`    | Application services | External service integration        |
| Event Publisher Ports | `port.messaging`  | Command handlers     | Event publishing                    |

## Implementation Checklist

- [ ] Command handlers annotated with `@Component` and `@Transactional`
- [ ] Query handlers annotated with `@Component` and `@Transactional(readOnly = true)`
- [ ] **Command/Query handlers use `@Slf4j` and `@RequiredArgsConstructor` (Lombok)**
- [ ] Repository interfaces defined in `port.repository` package (for aggregates)
- [ ] Data interfaces defined in `port.data` package (for read models)
- [ ] Service port interfaces defined in `port.service` package
- [ ] Event publisher port interfaces defined in `port.messaging` package
- [ ] Command handlers use repository ports (NOT data ports)
- [ ] Query handlers use data ports (NOT repository ports)
- [ ] Events published after successful commit
- [ ] Domain events cleared after publishing
- [ ] Command results return DTOs, not domain entities
- [ ] Query results return optimized DTOs
- [ ] **All DTOs use Lombok** (`@Getter`, `@Builder`, etc.) - **RECOMMENDED**
- [ ] Validation in command/query handlers

---

**Document Control**

- **Version History:**
    - v1.1 (2025-01) - Added Lombok usage guidelines and templates for DTOs and handlers
    - v1.0 (2025-01) - Initial template creation
- **Review Cycle:** Review when application service patterns change
- **Distribution:** All development team members

