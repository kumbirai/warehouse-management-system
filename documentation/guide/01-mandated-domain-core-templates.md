# Mandated Domain Core Templates

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Approved  
**Related Documents:**

- [Mandated Implementation Template Guide](mandated-Implementation-template-guide.md)
- [Service Architecture Document](../01-project-planning/architecture/Service_Architecture_Document.md)
- [Clean Code Guidelines](../01-project-planning/development/clean-code-guidelines-per-module.md)

---

## Overview

This document provides comprehensive code templates for the **Domain Core** module (`{service}-domain/{service}-domain-core`). The domain core contains pure Java business logic
with **NO external dependencies**.

**Key Principles:**

- Pure Java only (no Spring, JPA, or other frameworks)
- Manual implementation (NO Lombok)
- Rich domain model with encapsulated business logic
- Immutable value objects
- Domain events for state changes

---

## Package Structure

The Domain Core module (`{service}-domain/{service}-domain-core`) follows a strict package structure to maintain domain purity:

```
com.ccbsa.wms.{service}.domain.core/
├── entity/                    # Aggregate roots and domain entities
│   └── {DomainObject}.java
├── valueobject/               # Immutable value objects
│   ├── {DomainObject}Id.java
│   ├── {Attribute}.java
│   └── {DomainObject}Status.java
├── event/                     # Domain events
│   ├── {Service}Event.java
│   └── {DomainObject}{Action}Event.java
├── exception/                 # Domain exceptions
│   └── {DomainObject}{Violation}Exception.java
└── service/                   # Domain services (cross-aggregate logic)
    └── {ServiceName}DomainService.java
```

**Package Naming Convention:**

- Base package: `com.ccbsa.wms.{service}.domain.core`
- Replace `{service}` with actual service name (e.g., `stock`, `location`, `product`)
- Replace `{DomainObject}` with actual domain object name (e.g., `StockConsignment`, `Location`, `Product`)

**Package Responsibilities:**

| Package       | Responsibility                      | Contains                                                                                                 |
|---------------|-------------------------------------|----------------------------------------------------------------------------------------------------------|
| `entity`      | Aggregate roots and domain entities | Business entities with encapsulated logic, extends `AggregateRoot<ID>` or `TenantAwareAggregateRoot<ID>` |
| `valueobject` | Immutable value objects             | IDs, quantities, dates, status enums, and other immutable business concepts                              |
| `event`       | Domain events                       | Event classes extending `{Service}Event<T>`, published on state changes                                  |
| `exception`   | Domain exceptions                   | Business rule violation exceptions                                                                       |
| `service`     | Domain services                     | Cross-aggregate business logic that doesn't belong to a single aggregate                                 |

**Important Package Rules:**

- **NO** infrastructure dependencies (Spring, JPA, etc.)
- **NO** framework annotations
- **NO** Lombok annotations
- **ONLY** pure Java and common-domain base classes
- All classes must be in appropriate packages based on their responsibility

---

## Important Domain Driven Design, Clean Hexagonal Architecture principles, CQRS and Event Driven Design Notes

**Domain-Driven Design (DDD) Principles:**

- Rich domain model with business logic encapsulated in entities
- Value objects represent business concepts, not just data containers
- Aggregates maintain consistency boundaries
- Domain events capture business-relevant state changes
- Ubiquitous language reflected in class and method names

**Clean Hexagonal Architecture Principles:**

- Domain core is the innermost layer with no external dependencies
- Domain core defines business rules and invariants
- No infrastructure concerns leak into domain core
- Domain core is framework-agnostic and testable in isolation

**CQRS Principles:**

- Domain core focuses on write model (aggregates)
- Domain events enable read model projections
- Commands modify aggregates, queries read from projections
- Event-driven updates maintain eventual consistency

**Event-Driven Design Principles:**

- Domain events represent business facts that occurred
- Events are immutable and versioned for schema evolution
- Events enable loose coupling between bounded contexts
- Event handlers must be idempotent

---

## Table of Contents

1. [Aggregate Root Template](#aggregate-root-template)
2. [Value Object Templates](#value-object-templates)
3. [Domain Event Templates](#domain-event-templates)
4. [Domain Exception Templates](#domain-exception-templates)
5. [Domain Service Templates](#domain-service-templates)
6. [Common Patterns](#common-patterns)
7. [Anti-Patterns to Avoid](#anti-patterns-to-avoid)

---

## Aggregate Root Template

### Complete Aggregate Root Template

```java
package com.ccbsa.wms.{service}.domain.core.entity;

import com.ccbsa.common.domain.AggregateRoot;
import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import com.ccbsa.wms.{service}.domain.core.valueobject.{Attribute};
import com.ccbsa.wms.{service}.domain.core.event.{DomainObject}CreatedEvent;
import com.ccbsa.wms.{service}.domain.core.event.{DomainObject}UpdatedEvent;
import com.ccbsa.common.domain.valueobject.TenantId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate Root: {DomainObject}
 * 
 * Represents {business concept description}.
 * 
 * Business Rules:
 * - {Rule 1}
 * - {Rule 2}
 * - {Rule 3}
 */
public class {DomainObject} extends AggregateRoot<{DomainObject}Id> {
    
    // Value Objects
    private {Attribute} {attribute};
    private TenantId tenantId;
    
    // Enums
    private {DomainObject}Status status;
    
    // Primitives (use sparingly, prefer value objects)
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    
    // Collections (if needed)
    private List<{ChildEntity}> {childEntities};
    
    /**
     * Private constructor for builder pattern.
     * Prevents direct instantiation.
     */
    private {DomainObject}() {
        this.{childEntities} = new ArrayList<>();
    }
    
    /**
     * Factory method to create builder instance.
     * 
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for constructing {DomainObject} instances.
     * Ensures all required fields are set and validated.
     */
    public static class Builder {
        private {DomainObject} {domainObject} = new {DomainObject}();
        
        public Builder {domainObject}Id({DomainObject}Id id) {
            {domainObject}.id = id;
            return this;
        }
        
        public Builder tenantId(TenantId tenantId) {
            {domainObject}.tenantId = tenantId;
            return this;
        }
        
        public Builder {attribute}({Attribute} {attribute}) {
            {domainObject}.{attribute} = {attribute};
            return this;
        }
        
        public Builder status({DomainObject}Status status) {
            {domainObject}.status = status;
            return this;
        }
        
        public Builder {childEntity}({ChildEntity} {childEntity}) {
            if ({domainObject}.{childEntities} == null) {
                {domainObject}.{childEntities} = new ArrayList<>();
            }
            {domainObject}.{childEntities}.add({childEntity});
            return this;
        }
        
        /**
         * Builds and validates the {DomainObject} instance.
         * 
         * @return Validated {DomainObject} instance
         * @throws IllegalArgumentException if validation fails
         */
        public {DomainObject} build() {
            validate();
            initializeDefaults();
            {domainObject}.createdAt = LocalDateTime.now();
            {domainObject}.lastModifiedAt = LocalDateTime.now();
            
            // Publish creation event
            {domainObject}.addDomainEvent(new {DomainObject}CreatedEvent(
                {domainObject}.id,
                {domainObject}.tenantId,
                {domainObject}.{attribute}
            ));
            
            return {domainObject};
        }
        
        /**
         * Validates all required fields are set.
         * 
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if ({domainObject}.id == null) {
                throw new IllegalArgumentException("{DomainObject}Id is required");
            }
            if ({domainObject}.tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if ({domainObject}.{attribute} == null) {
                throw new IllegalArgumentException("{Attribute} is required");
            }
            // Add other required field validations
        }
        
        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if ({domainObject}.status == null) {
                {domainObject}.status = {DomainObject}Status.{DEFAULT_STATUS};
            }
        }
    }
    
    /**
     * Business logic method: {business action description}
     * 
     * Business Rules:
     * - {Rule 1}
     * - {Rule 2}
     * 
     * @throws IllegalStateException if business rule violation
     */
    public void {businessAction}() {
        // Validate preconditions
        if (!can{businessAction}()) {
            throw new IllegalStateException(
                "Cannot {businessAction}: {reason}"
            );
        }
        
        // Execute business logic
        this.status = {DomainObject}Status.{NEW_STATUS};
        this.lastModifiedAt = LocalDateTime.now();
        
        // Publish domain event
        addDomainEvent(new {DomainObject}UpdatedEvent(
            this.id,
            this.tenantId,
            this.status,
            "{businessAction} completed"
        ));
    }
    
    /**
     * Business logic method: {another business action}
     * 
     * @param {parameter} {parameter description}
     * @throws IllegalArgumentException if parameter invalid
     */
    public void {anotherBusinessAction}({ParameterType} {parameter}) {
        // Validate parameter
        if ({parameter} == null) {
            throw new IllegalArgumentException("{Parameter} cannot be null");
        }
        
        // Execute business logic
        this.{attribute} = {parameter};
        this.lastModifiedAt = LocalDateTime.now();
        
        // Publish domain event
        addDomainEvent(new {DomainObject}UpdatedEvent(
            this.id,
            this.tenantId,
            this.status,
            "{anotherBusinessAction} completed"
        ));
    }
    
    /**
     * Query method: Checks if {business action} can be performed.
     * 
     * @return true if {business action} is allowed
     */
    public boolean can{businessAction}() {
        return this.status == {DomainObject}Status.{REQUIRED_STATUS}
            && {otherCondition};
    }
    
    /**
     * Query method: Checks current state.
     * 
     * @return true if in specified state
     */
    public boolean is{State}() {
        return this.status == {DomainObject}Status.{STATE};
    }
    
    // Getters (read-only access)
    
    public {DomainObject}Id getId() {
        return id;
    }
    
    public TenantId getTenantId() {
        return tenantId;
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
    
    public List<{ChildEntity}> get{ChildEntities}() {
        return new ArrayList<>({childEntities}); // Defensive copy
    }
}
```

### Tenant-Aware Aggregate Root Template

For aggregates that require tenant awareness:

```java
package com.ccbsa.wms.{service}.domain.core.entity;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;

public class {DomainObject} extends TenantAwareAggregateRoot<{DomainObject}Id> {
    
    // Same structure as above, but extends TenantAwareAggregateRoot
    // TenantId is automatically available via inheritance
    
    // Builder and methods same as above
}
```

---

## Value Object Templates

### ID Value Object Template

```java
package com.ccbsa.wms.{service}.domain.core.valueobject;

import java.util.UUID;

/**
 * Value Object: {DomainObject}Id
 * 
 * Represents the unique identifier for {DomainObject}.
 * Immutable and validated on construction.
 */
public final class {DomainObject}Id {
    private final UUID value;
    
    /**
     * Private constructor to enforce immutability.
     * 
     * @param value UUID value
     * @throws IllegalArgumentException if value is null
     */
    private {DomainObject}Id(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("{DomainObject}Id value cannot be null");
        }
        this.value = value;
    }
    
    /**
     * Factory method to create {DomainObject}Id from UUID.
     * 
     * @param value UUID value
     * @return {DomainObject}Id instance
     * @throws IllegalArgumentException if value is null
     */
    public static {DomainObject}Id of(UUID value) {
        return new {DomainObject}Id(value);
    }
    
    /**
     * Factory method to create {DomainObject}Id from String.
     * 
     * @param value UUID string representation
     * @return {DomainObject}Id instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static {DomainObject}Id of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("{DomainObject}Id string cannot be null or empty");
        }
        try {
            return new {DomainObject}Id(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format: %s", value), e);
        }
    }
    
    /**
     * Generates a new {DomainObject}Id with random UUID.
     * 
     * @return New {DomainObject}Id instance
     */
    public static {DomainObject}Id generate() {
        return new {DomainObject}Id(UUID.randomUUID());
    }
    
    /**
     * Returns the UUID value.
     * 
     * @return UUID value
     */
    public UUID getValue() {
        return value;
    }
    
    /**
     * Returns the string representation of the UUID.
     * 
     * @return UUID string
     */
    public String getValueAsString() {
        return value.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        {DomainObject}Id that = ({DomainObject}Id) o;
        return value.equals(that.value);
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}
```

### Complex Value Object Template

```java
package com.ccbsa.wms.{service}.domain.core.valueobject;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Value Object: {Attribute}
 * 
 * Represents {business concept description}.
 * Immutable and self-validating.
 */
public final class {Attribute} {
    private final {Type} value;
    
    /**
     * Private constructor to enforce immutability.
     * 
     * @param value {Type} value
     * @throws IllegalArgumentException if value is invalid
     */
    private {Attribute}({Type} value) {
        validate(value);
        this.value = value;
    }
    
    /**
     * Factory method to create {Attribute} instance.
     * 
     * @param value {Type} value
     * @return {Attribute} instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static {Attribute} of({Type} value) {
        return new {Attribute}(value);
    }
    
    /**
     * Validates the value according to business rules.
     * 
     * @param value Value to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validate({Type} value) {
        if (value == null) {
            throw new IllegalArgumentException("{Attribute} cannot be null");
        }
        // Add specific validation rules
        // Example: if (value < 0) throw new IllegalArgumentException("Value must be positive");
    }
    
    /**
     * Returns the value.
     * 
     * @return {Type} value
     */
    public {Type} getValue() {
        return value;
    }
    
    /**
     * Business logic method: {business operation}
     * 
     * @param other Other {Attribute} to compare/operate with
     * @return Result of operation
     */
    public {Attribute} {operation}({Attribute} other) {
        if (other == null) {
            throw new IllegalArgumentException("Other {Attribute} cannot be null");
        }
        // Perform operation and return new instance
        return {Attribute}.of(this.value {operator} other.value);
    }
    
    /**
     * Query method: Checks if value meets condition.
     * 
     * @return true if condition met
     */
    public boolean is{Condition}() {
        return {condition check};
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        {Attribute} that = ({Attribute}) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
```

---

## Domain Event Templates

### Base Service Event Template

```java
package com.ccbsa.wms.{service}.domain.core.event;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.TenantId;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all {Service} domain events.
 * 
 * All service-specific events extend this class.
 * 
 * @param <T> The aggregate root type
 */
public abstract class {Service}Event<T> extends DomainEvent<T> {
    
    private final TenantId tenantId;
    private final int eventVersion;
    
    /**
     * Constructor for {Service} events.
     * 
     * @param aggregateId Aggregate identifier
     * @param tenantId Tenant identifier
     */
    protected {Service}Event(Object aggregateId, TenantId tenantId) {
        super(aggregateId);
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        this.tenantId = tenantId;
        this.eventVersion = 1; // Explicit version for schema evolution
    }
    
    /**
     * Constructor with explicit version.
     * 
     * @param aggregateId Aggregate identifier
     * @param tenantId Tenant identifier
     * @param eventVersion Event version
     */
    protected {Service}Event(Object aggregateId, TenantId tenantId, int eventVersion) {
        super(aggregateId);
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        this.tenantId = tenantId;
        this.eventVersion = eventVersion;
    }
    
    public TenantId getTenantId() {
        return tenantId;
    }
    
    public int getEventVersion() {
        return eventVersion;
    }
}
```

### EventMetadata Value Object Template

```java
package com.ccbsa.common.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable value object representing metadata for domain events.
 * Contains traceability information for distributed tracing and event correlation.
 *
 * <p>Event metadata includes:
 * <ul>
 *   <li>correlationId - Tracks entire business flow across services</li>
 *   <li>causationId - Tracks immediate cause of event (parent event ID)</li>
 *   <li>userId - User identifier who triggered the event</li>
 * </ul>
 * </p>
 */
public final class EventMetadata {
    private final String correlationId;
    private final UUID causationId;
    private final String userId;

    private EventMetadata(Builder builder) {
        this.correlationId = builder.correlationId;
        this.causationId = builder.causationId;
        this.userId = builder.userId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public UUID getCausationId() {
        return causationId;
    }

    public String getUserId() {
        return userId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String correlationId;
        private UUID causationId;
        private String userId;

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder causationId(UUID causationId) {
            this.causationId = causationId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public EventMetadata build() {
            return new EventMetadata(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventMetadata that = (EventMetadata) o;
        return Objects.equals(correlationId, that.correlationId)
                && Objects.equals(causationId, that.causationId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId, causationId, userId);
    }
}
```

**Note:** EventMetadata is set by the infrastructure layer (event publisher), not by domain code. Domain events support optional metadata via `DomainEvent.getMetadata()` and
`DomainEvent.setMetadata()`.

### Domain Event Template

```java
package com.ccbsa.wms.{service}.domain.core.event;

import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import com.ccbsa.wms.{service}.domain.core.valueobject.{Attribute};
import com.ccbsa.common.domain.valueobject.TenantId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain Event: {DomainObject}{Action}Event
 * 
 * Published when {business event description}.
 * 
 * Event Version: 1.0
 */
public class {DomainObject}{Action}Event extends {Service}Event<{DomainObject}> {
    
    private final {Attribute} {attribute};
    private final {OtherAttribute} {otherAttribute};
    private final List<{Item}> items;
    private final LocalDateTime occurredAt;
    
    /**
     * Constructor for {DomainObject}{Action}Event.
     * 
     * @param aggregateId Aggregate identifier
     * @param tenantId Tenant identifier
     * @param {attribute} {Attribute description}
     * @param {otherAttribute} {Other attribute description}
     * @param items List of items (if applicable)
     */
    public {DomainObject}{Action}Event(
            {DomainObject}Id aggregateId,
            TenantId tenantId,
            {Attribute} {attribute},
            {OtherAttribute} {otherAttribute},
            List<{Item}> items
    ) {
        super(aggregateId, tenantId);
        if ({attribute} == null) {
            throw new IllegalArgumentException("{Attribute} cannot be null");
        }
        this.{attribute} = {attribute};
        this.{otherAttribute} = {otherAttribute};
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.occurredAt = LocalDateTime.now();
    }
    
    // Getters (immutable access)
    
    public {Attribute} get{Attribute}() {
        return {attribute};
    }
    
    public {OtherAttribute} get{OtherAttribute}() {
        return {otherAttribute};
    }
    
    public List<{Item}> getItems() {
        return Collections.unmodifiableList(items);
    }
    
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String toString() {
        return String.format(
            "{DomainObject}{Action}Event{aggregateId=%s, tenantId=%s, {attribute}=%s}",
            getAggregateId(),
            getTenantId(),
            {attribute}
        );
    }
}
```

---

## Domain Exception Templates

### Domain Exception Template

```java
package com.ccbsa.wms.{service}.domain.core.exception;

/**
 * Domain Exception: {DomainObject}{Violation}Exception
 * 
 * Thrown when {violation description}.
 */
public class {DomainObject}{Violation}Exception extends RuntimeException {
    
    private final String {domainObject}Id;
    private final String reason;
    
    /**
     * Constructor for {DomainObject}{Violation}Exception.
     * 
     * @param {domainObject}Id {DomainObject} identifier
     * @param reason Reason for violation
     */
    public {DomainObject}{Violation}Exception(String {domainObject}Id, String reason) {
        super(String.format("{DomainObject} violation: %s (ID: %s)", reason, {domainObject}Id));
        this.{domainObject}Id = {domainObject}Id;
        this.reason = reason;
    }
    
    /**
     * Constructor with cause.
     * 
     * @param {domainObject}Id {DomainObject} identifier
     * @param reason Reason for violation
     * @param cause Underlying cause
     */
    public {DomainObject}{Violation}Exception(String {domainObject}Id, String reason, Throwable cause) {
        super(String.format("{DomainObject} violation: %s (ID: %s)", reason, {domainObject}Id), cause);
        this.{domainObject}Id = {domainObject}Id;
        this.reason = reason;
    }
    
    public String get{DomainObject}Id() {
        return {domainObject}Id;
    }
    
    public String getReason() {
        return reason;
    }
}
```

---

## Domain Service Templates

### Domain Service Template

```java
package com.ccbsa.wms.{service}.domain.core.service;

import com.ccbsa.wms.{service}.domain.core.entity.{DomainObject1};
import com.ccbsa.wms.{service}.domain.core.entity.{DomainObject2};
import com.ccbsa.wms.{service}.domain.core.valueobject.{Attribute};

/**
 * Domain Service: {ServiceName}DomainService
 * 
 * Handles business logic that spans multiple aggregates within the same bounded context.
 * Pure domain logic with no infrastructure dependencies.
 */
public class {ServiceName}DomainService {
    
    /**
     * Business logic method: {business operation description}
     * 
     * Coordinates operations across multiple aggregates.
     * 
     * @param {domainObject1} First aggregate
     * @param {domainObject2} Second aggregate
     * @param {attribute} Attribute for operation
     * @throws IllegalArgumentException if parameters invalid
     * @throws IllegalStateException if business rule violation
     */
    public void {businessOperation}(
            {DomainObject1} {domainObject1},
            {DomainObject2} {domainObject2},
            {Attribute} {attribute}
    ) {
        // Validate parameters
        if ({domainObject1} == null) {
            throw new IllegalArgumentException("{DomainObject1} cannot be null");
        }
        if ({domainObject2} == null) {
            throw new IllegalArgumentException("{DomainObject2} cannot be null");
        }
        if ({attribute} == null) {
            throw new IllegalArgumentException("{Attribute} cannot be null");
        }
        
        // Validate business rules
        if (!{domainObject1}.can{Operation}()) {
            throw new IllegalStateException(
                "{DomainObject1} cannot perform {operation}"
            );
        }
        
        // Execute business logic spanning aggregates
        {domainObject1}.{method1}({attribute});
        {domainObject2}.{method2}({attribute});
        
        // Note: Domain events are published by aggregates, not domain service
    }
}
```

---

## Common Patterns

### Enum Status Pattern

```java
package com.ccbsa.wms.{service}.domain.core.valueobject;

/**
 * Enum: {DomainObject}Status
 * 
 * Represents the possible states of {DomainObject}.
 */
public enum {DomainObject}Status {
    CREATED("Created"),
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");
    
    private final String description;
    
    {DomainObject}Status(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if status allows transition to target status.
     * 
     * @param targetStatus Target status
     * @return true if transition allowed
     */
    public boolean canTransitionTo({DomainObject}Status targetStatus) {
        // Define valid transitions
        return switch (this) {
            case CREATED -> targetStatus == PENDING || targetStatus == CANCELLED;
            case PENDING -> targetStatus == IN_PROGRESS || targetStatus == CANCELLED;
            case IN_PROGRESS -> targetStatus == COMPLETED || targetStatus == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
```

---

## Anti-Patterns to Avoid

### ❌ DO NOT: Use Lombok in Domain Core

```java
// ❌ WRONG
import lombok.Getter;
import lombok.Builder;

@Getter
@Builder
public class StockConsignment extends AggregateRoot<ConsignmentId> {
    // ...
}
```

```java
// ✅ CORRECT: Manual implementation
public class StockConsignment extends AggregateRoot<ConsignmentId> {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        // Manual builder implementation
    }
    
    // Manual getters
}
```

### ❌ DO NOT: Add Framework Annotations

```java
// ❌ WRONG
import jakarta.persistence.*;
import org.springframework.stereotype.Component;

@Entity
@Table(name = "stock_consignments")
@Component
public class StockConsignment extends AggregateRoot<ConsignmentId> {
    // ...
}
```

```java
// ✅ CORRECT: Pure Java, no annotations
public class StockConsignment extends AggregateRoot<ConsignmentId> {
    // Pure domain logic
}
```

### ❌ DO NOT: Expose Mutable Collections

```java
// ❌ WRONG
public List<LineItem> getLineItems() {
    return lineItems; // Mutable reference exposed
}
```

```java
// ✅ CORRECT: Defensive copy
public List<LineItem> getLineItems() {
    return new ArrayList<>(lineItems); // Immutable copy
}
```

---

## Implementation Checklist

- [ ] Aggregate extends `AggregateRoot<ID>` or `TenantAwareAggregateRoot<ID>`
- [ ] Builder pattern implemented manually (no Lombok)
- [ ] All value objects are immutable (`final` class, `final` fields)
- [ ] Value objects implement `equals()`, `hashCode()`, `toString()`
- [ ] Domain events extend `{Service}Event<T>`
- [ ] Domain events support optional EventMetadata (set by infrastructure layer)
- [ ] Business logic encapsulated in entity methods
- [ ] No Lombok annotations
- [ ] No Spring/JPA annotations
- [ ] No external dependencies (except `common-domain`)
- [ ] All collections returned as defensive copies
- [ ] Validation in value object constructors
- [ ] Validation in aggregate builder `build()` method

---

**Document Control**

- **Version History:** v1.0 (2025-01) - Initial template creation
- **Review Cycle:** Review when domain patterns change
- **Distribution:** All development team members

