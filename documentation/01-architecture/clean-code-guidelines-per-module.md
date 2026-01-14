# Clean Code Guidelines Per Module

## Overview

This document provides comprehensive clean code guidelines and things to look out for in each module of the microservice architecture. These guidelines are based on Domain-Driven
Design (DDD), Clean Hexagonal Architecture, CQRS, and Event-Driven Design principles as defined in the Service Architecture Document.

**Related Documents:**

- [Mandated Implementation Template Guide](../guide/mandated-Implementation-template-guide.md)
- [Service Architecture Document](Service_Architecture_Document.md)
- [Domain Core Templates](../guide/01-mandated-domain-core-templates.md)
- [Application Service Templates](../guide/02-mandated-application-service-templates.md)
- [Application Layer Templates](../guide/03-mandated-application-layer-templates.md)
- [Data Access Templates](../guide/04-mandated-data-access-templates.md)
- [Messaging Templates](../guide/05-mandated-messaging-templates.md)
- [Container Templates](../guide/06-mandated-container-templates.md)
- [Frontend Templates](../guide/07-mandated-frontend-templates.md)

## Table of Contents

1. [Lombok Usage Policy](#lombok-usage-policy)
2. [Domain Core Module](#domain-core-module)
3. [Application Service Module](#application-service-module)
4. [Data Access Module](#data-access-module)
5. [Application Module](#application-module)
6. [Messaging Module](#messaging-module)
7. [Container Module](#container-module)
8. [Frontend Module](#frontend-module)
9. [General Clean Code Principles](#general-clean-code-principles)

---

## Lombok Usage Policy

**CRITICAL DISTINCTION**: This project mandates a clear separation between domain core (pure Java) and infrastructure layers (Spring-enabled).

### Domain Core Modules (`*-domain-core`)

**NO LOMBOK** - Pure Java implementation required:

- Domain core must remain framework-agnostic
- Manual builder patterns required
- Manual getters/setters required
- Manual `equals()`, `hashCode()`, and `toString()` implementations
- Rationale: Domain purity, framework independence, explicit business logic

### All Other Modules

**LOMBOK RECOMMENDED** - Use Lombok to reduce boilerplate:

- Application Service (`*-application-service`) - Commands, queries, results
- Application Layer (`*-application`) - DTOs, mappers
- Data Access (`*-dataaccess`) - JPA entities, adapters
- Messaging (`*-messaging`) - Event listeners, publishers
- Container (`*-container`) - Configuration classes

**Recommended Lombok Annotations:**

- `@Getter` / `@Setter` - For fields requiring accessors
- `@Builder` - For complex object construction
- `@NoArgsConstructor` / `@AllArgsConstructor` - For constructors
- `@ToString` - For debugging and logging
- `@EqualsAndHashCode` - For value comparison (use with caution for entities)
- `@Slf4j` - For logging (preferred over manual logger declaration)
- `@RequiredArgsConstructor` - For dependency injection

**Lombok Anti-Patterns to Avoid:**

- `@Data` - Too broad, use specific annotations instead
- `@Value` - Use for truly immutable classes only
- Avoid Lombok in classes with complex business logic validation

**Example Comparison:**

```java
// Domain Core (NO Lombok)
public class CreateConsignmentResult {
    private final ConsignmentId consignmentId;
    private final ConsignmentStatus status;

    private CreateConsignmentResult(Builder builder) {
        this.consignmentId = builder.consignmentId;
        this.status = builder.status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ConsignmentId getConsignmentId() { return consignmentId; }
    public ConsignmentStatus getStatus() { return status; }

    public static class Builder {
        private ConsignmentId consignmentId;
        private ConsignmentStatus status;

        public Builder consignmentId(ConsignmentId id) {
            this.consignmentId = id;
            return this;
        }

        public CreateConsignmentResult build() {
            return new CreateConsignmentResult(this);
        }
    }
}

// Application Layer (USE Lombok)
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateConsignmentResultDTO {
    private final UUID consignmentId;
    private final String status;
    private final LocalDateTime createdAt;
}
```

---

## Domain Core Module

### Module Purpose

The domain core module (`{service}-domain/{service}-domain-core`) contains pure business logic and domain entities. It is the innermost layer with **NO external dependencies**
except `common-domain`.

### Package Structure

```
com.ccbsa.wms.{service}.domain.core/
├── entity/                    # Aggregate roots and domain entities
├── valueobject/               # Immutable value objects
├── event/                     # Domain events
├── exception/                 # Domain exceptions
└── service/                   # Domain services (cross-aggregate logic)
```

### Clean Code Principles

#### ✅ **DO: Pure Java Implementation**

- Use **only pure Java** with no framework dependencies
- **NO Lombok annotations** - implement manually (Builder, getters, toString)
- **NO Spring Boot dependencies** in domain core
- **NO validation framework dependencies** (Jakarta Validation, Spring Validation)
- Use `String.format()` for `toString()` methods instead of Lombok

**Example:**

```java
// ✅ CORRECT: Manual Builder pattern
public static Builder builder() {
    return new Builder();
}

public static class Builder {
    private ConsignmentId id;
    private TenantId tenantId;

    public Builder id(ConsignmentId id) {
        this.id = id;
        return this;
    }

    public StockConsignment build() {
        // Validation logic
        return new StockConsignment(this);
    }
}

// ✅ CORRECT: Manual toString()
@Override
public String toString() {
    return String.format("StockConsignment{id=%s, status=%s}", id, status);
}
```

#### ✅ **DO: Rich Domain Model**

- Encapsulate business logic within entities
- Use value objects for complex data types
- Implement business invariants in domain objects
- Use domain events for state changes
- Business methods should use **ubiquitous language** from the domain

**Example:**

```java
// ✅ CORRECT: Rich domain entity with business logic
public class StockConsignment extends TenantAwareAggregateRoot<ConsignmentId> {

    // Business logic method using ubiquitous language
    public void confirm() {
        if (!canBeConfirmed()) {
            throw new IllegalStateException("Cannot confirm consignment in status: " + status);
        }
        this.status = ConsignmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();

        // Record domain event
        this.addDomainEvent(new ConsignmentConfirmedEvent(
            this.getId(), this.getTenantId(), LocalDateTime.now()
        ));
    }

    public boolean canBeConfirmed() {
        return status == ConsignmentStatus.PENDING && !items.isEmpty();
    }
}
```

#### ✅ **DO: Correct Entity Inheritance**

- Extend `TenantAwareAggregateRoot<ID>` for tenant-aware aggregates
- Extend `AggregateRoot<ID>` for non-tenant aggregates
- **Use inherited `getId()` and `setId(ID id)` methods from BaseEntity**
- Call `super(tenantId)` in constructor and use `super.setId(builder.id)` for ID
- Use `this.getId().getValue().toString()` in domain events
- **NEVER declare `private final ID id` field** - ID is managed by BaseEntity

**Example:**

```java
// ✅ CORRECT: Proper entity inheritance
public class StockConsignment extends TenantAwareAggregateRoot<ConsignmentId> {
    // NO private ConsignmentId id field - inherited from BaseEntity!

    private final List<ConsignmentLine> lines;
    private ConsignmentStatus status;

    private StockConsignment(Builder builder) {
        super(builder.tenantId); // Call parent constructor
        super.setId(Objects.requireNonNull(builder.id, "Consignment ID cannot be null"));
        this.lines = new ArrayList<>(Objects.requireNonNull(builder.lines, "Lines cannot be null"));
        this.status = builder.status != null ? builder.status : ConsignmentStatus.PENDING;

        // Use inherited getId() method
        this.addDomainEvent(new ConsignmentCreatedEvent(
            this.getId().getValue().toString(),
            this.getTenantId(),
            Instant.now()
        ));
    }

    // getId() is inherited from BaseEntity - don't override unless necessary
}
```

#### ❌ **AVOID: Incorrect Entity Inheritance**

- **Problem**: Wrong inheritance pattern or ID field management
- **Signs**:
    - `extends AggregateRoot<ID>` instead of `TenantAwareAggregateRoot<ID>` for tenant-aware entities
    - `private final ConsignmentId id` field declaration
    - Implementing custom `getId()` method instead of using inherited one
    - Using `this.id` directly instead of `this.getId()`
    - Setting `this.id = builder.id` instead of `super.setId(builder.id)`
- **Solution**: Follow correct inheritance pattern with proper ID management

**Example:**

```java
// ❌ WRONG: Incorrect entity inheritance
public class StockConsignment extends AggregateRoot<ConsignmentId> {
    private final ConsignmentId id; // ❌ WRONG: Don't declare ID field

    private StockConsignment(Builder builder) {
        this.id = builder.id; // ❌ WRONG: Don't set ID directly
    }

    @Override
    public ConsignmentId getId() { // ❌ WRONG: Don't override getId()
        return this.id;
    }
}
```

#### ❌ **AVOID: God Entities**

- **Problem**: Entities with too many responsibilities
- **Signs**: Large classes with 20+ fields, multiple concerns
- **Solution**: Split into multiple focused entities or use composition

#### ❌ **AVOID: Primitive Obsession**

- **Problem**: Using primitives instead of value objects
- **Signs**: `String email`, `String phoneNumber`, `int quantity`, `LocalDate expirationDate`
- **Solution**: Create value objects like `Email`, `PhoneNumber`, `Quantity`, `ExpirationDate`

**Example:**

```java
// ❌ WRONG: Primitive obsession
public class StockConsignment {
    private int quantity;
    private LocalDate expirationDate;
    private String batchNumber;
}

// ✅ CORRECT: Value objects
public class StockConsignment {
    private Quantity quantity;
    private ExpirationDate expirationDate;
    private BatchNumber batchNumber;
}
```

#### ❌ **AVOID: Anemic Domain Model**

- **Problem**: Entities with only getters/setters, no business logic
- **Signs**: Data containers without behavior
- **Solution**: Move business logic into entities

**Example:**

```java
// ❌ WRONG: Anemic domain model
public class StockConsignment {
    private String id;
    private String status;

    // Only getters and setters - no business logic
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
```

#### ❌ **AVOID: Leaky Abstractions**

- **Problem**: Domain objects exposing implementation details
- **Signs**:
    - Public setters for internal state
    - Exposing collections directly (not defensive copies)
    - JPA annotations in domain entities
- **Solution**: Use proper encapsulation and business methods

**Example:**

```java
// ❌ WRONG: Leaky abstraction
public List<ConsignmentLine> getLines() {
    return lines; // Exposes mutable collection
}

// ✅ CORRECT: Defensive copy
public List<ConsignmentLine> getLines() {
    return new ArrayList<>(lines);
}
```

### Code Examples

#### ✅ **Good: Rich Domain Entity**

```java
public class StockConsignment extends TenantAwareAggregateRoot<ConsignmentId> {
    // NO ID field - inherited from BaseEntity
    private final List<ConsignmentLine> lines;
    private ConsignmentStatus status;
    private final BatchNumber batchNumber;
    private ExpirationDate expirationDate;

    private StockConsignment(Builder builder) {
        super(builder.tenantId);
        super.setId(Objects.requireNonNull(builder.id, "Consignment ID cannot be null"));
        this.lines = new ArrayList<>(Objects.requireNonNull(builder.lines, "Lines cannot be null"));
        this.batchNumber = Objects.requireNonNull(builder.batchNumber, "Batch number cannot be null");
        this.expirationDate = builder.expirationDate;
        this.status = ConsignmentStatus.PENDING;
        validateBusinessRules();

        this.addDomainEvent(new ConsignmentCreatedEvent(
            this.getId().getValue().toString(),
            this.getTenantId(),
            Instant.now()
        ));
    }

    public void confirm() {
        if (!canBeConfirmed()) {
            throw new IllegalStateException("Cannot confirm " + status + " consignment");
        }
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot confirm consignment without lines");
        }
        this.status = ConsignmentStatus.CONFIRMED;
        this.setUpdatedAt(Instant.now());

        this.addDomainEvent(new ConsignmentConfirmedEvent(
            this.getId().getValue().toString(),
            this.getTenantId(),
            Instant.now()
        ));
    }

    public void updateExpirationDate(ExpirationDate newExpirationDate) {
        if (newExpirationDate == null) {
            throw new IllegalArgumentException("Expiration date cannot be null");
        }
        if (this.status == ConsignmentStatus.EXPIRED) {
            throw new IllegalStateException("Cannot update expiration date of expired consignment");
        }
        this.expirationDate = newExpirationDate;
        this.setUpdatedAt(Instant.now());

        this.addDomainEvent(new ConsignmentExpirationDateUpdatedEvent(
            this.getId().getValue().toString(),
            this.getTenantId(),
            newExpirationDate,
            Instant.now()
        ));
    }

    private boolean canBeConfirmed() {
        return status == ConsignmentStatus.PENDING;
    }

    private void validateBusinessRules() {
        if (batchNumber == null) {
            throw new IllegalArgumentException("Batch number is required");
        }
    }

    // Getters
    public List<ConsignmentLine> getLines() {
        return new ArrayList<>(lines); // Defensive copy
    }

    public ConsignmentStatus getStatus() {
        return status;
    }

    public BatchNumber getBatchNumber() {
        return batchNumber;
    }

    public ExpirationDate getExpirationDate() {
        return expirationDate;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ConsignmentId id;
        private TenantId tenantId;
        private List<ConsignmentLine> lines;
        private BatchNumber batchNumber;
        private ExpirationDate expirationDate;

        public Builder id(ConsignmentId id) {
            this.id = id;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder lines(List<ConsignmentLine> lines) {
            this.lines = lines;
            return this;
        }

        public Builder batchNumber(BatchNumber batchNumber) {
            this.batchNumber = batchNumber;
            return this;
        }

        public Builder expirationDate(ExpirationDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public StockConsignment build() {
            if (id == null) throw new IllegalArgumentException("ID is required");
            if (tenantId == null) throw new IllegalArgumentException("Tenant ID is required");
            if (lines == null) throw new IllegalArgumentException("Lines are required");
            if (batchNumber == null) throw new IllegalArgumentException("Batch number is required");

            return new StockConsignment(this);
        }
    }
}
```

#### ✅ **Good: Value Object**

```java
public final class ExpirationDate {
    private final LocalDate value;

    private ExpirationDate(LocalDate value) {
        if (value == null) {
            throw new IllegalArgumentException("Expiration date cannot be null");
        }
        if (value.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiration date cannot be in the past");
        }
        this.value = value;
    }

    public static ExpirationDate of(LocalDate value) {
        return new ExpirationDate(value);
    }

    public LocalDate getValue() {
        return value;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(value);
    }

    public boolean isExpiringSoon(int days) {
        return LocalDate.now().plusDays(days).isAfter(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ExpirationDate other = (ExpirationDate) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.format("ExpirationDate{value=%s}", value);
    }
}
```

### Common Anti-Patterns to Avoid

1. **God Entity**: Single entity handling multiple concerns
2. **Primitive Obsession**: Using primitives instead of value objects
3. **Anemic Domain Model**: Entities without business logic
4. **Leaky Abstractions**: Exposing implementation details
5. **Feature Envy**: Methods that use more data from other classes than their own
6. **Data Clumps**: Groups of data that always appear together but aren't grouped
7. **Shotgun Surgery**: Making changes that require modifications in many places
8. **Incorrect Inheritance**: Wrong base class or manual ID field management

---

## Application Service Module

### Module Purpose

The application service module (`{service}-domain/{service}-application-service`) orchestrates use cases and defines port interfaces. It handles transaction boundaries and
coordinates between different layers.

### Package Structure

```
com.ccbsa.wms.{service}.application.service/
├── command/                           # Command handlers (write operations)
│   └── dto/                           # Command DTOs
├── query/                             # Query handlers (read operations)
│   └── dto/                           # Query DTOs
└── port/                              # Port interfaces (contracts)
    ├── repository/                    # Repository ports (aggregate persistence)
    ├── data/                          # Data ports (read model access)
    ├── service/                       # Service ports (external integrations)
    └── messaging/                     # Event publisher ports
```

### Clean Code Principles

#### ✅ **DO: Lombok Usage (RECOMMENDED)**

- **Use Lombok for all DTOs** (Commands, Queries, Results) to reduce boilerplate
- **Handlers**: Use `@Slf4j` for logging and `@RequiredArgsConstructor` for dependency injection
- **DTOs**: Use `@Getter`, `@Builder`, `@ToString`, `@EqualsAndHashCode` as appropriate
- **Anti-Patterns**: Avoid `@Data` (too broad), avoid `@Value` (only for truly immutable classes)

**Example:**

```java
// ✅ CORRECT: Command handler with Lombok
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfirmConsignmentCommandHandler {

    private final StockConsignmentRepository repository;
    private final StockManagementEventPublisher eventPublisher;

    @Transactional
    public ConfirmConsignmentResult handle(ConfirmConsignmentCommand command) {
        // Handler implementation
    }
}

// ✅ CORRECT: Command DTO with Lombok
@Getter
@Builder
@ToString
@EqualsAndHashCode
public class ConfirmConsignmentCommand {
    private final ConsignmentId consignmentId;
    private final TenantId tenantId;
}

// ✅ CORRECT: Result DTO with Lombok
@Getter
@Builder
public class ConfirmConsignmentResult {
    private final ConsignmentId consignmentId;
    private final ConsignmentStatus status;
    private final LocalDateTime confirmedAt;
}
```

#### ✅ **DO: Single Responsibility**

- Each handler should handle one use case
- Clear separation of concerns (command vs query)
- Focused, cohesive functionality
- One transaction per handler method

**Example:**

```java
// ✅ CORRECT: Focused handler with single responsibility
@Component
public class ConfirmConsignmentCommandHandler {

    private final StockConsignmentRepository repository;
    private final StockManagementEventPublisher eventPublisher;

    @Transactional
    public ConfirmConsignmentResult handle(ConfirmConsignmentCommand command) {
        // 1. Load aggregate
        StockConsignment consignment = repository.findByTenantIdAndId(
            command.getTenantId(), command.getConsignmentId()
        ).orElseThrow(() -> new ConsignmentNotFoundException(
            command.getConsignmentId().getValueAsString()
        ));

        // 2. Execute business logic (via aggregate)
        consignment.confirm();

        // 3. Persist changes
        repository.save(consignment);

        // 4. Publish events (correlation ID injected automatically from CorrelationContext)
        List<DomainEvent<?>> events = consignment.getDomainEvents();
        if (!events.isEmpty()) {
            eventPublisher.publish(events);
            consignment.clearDomainEvents();
        }

        // 5. Return command result
        return ConfirmConsignmentResult.builder()
            .consignmentId(consignment.getId())
            .status(consignment.getStatus())
            .confirmedAt(LocalDateTime.now())
            .build();
    }
}
```

#### ✅ **DO: Use Case Naming**

- Use "Handler" suffix for implementation classes
- Use descriptive names that reflect the use case
- Follow CQRS naming conventions:
    - Commands: `{Action}{DomainObject}CommandHandler`
    - Queries: `Get{DomainObject}QueryHandler` or `List{DomainObject}sQueryHandler`

#### ✅ **DO: Transaction Management**

- Keep transactions as small as possible
- Publish events **after successful commits**
- Handle rollback scenarios properly
- Use `@Transactional` for command handlers
- Use `@Transactional(readOnly = true)` for query handlers

#### ✅ **DO: Error Handling**

- Use specific exception types
- Provide meaningful error messages
- Log errors appropriately
- Don't swallow exceptions

#### ✅ **DO: CQRS Separation**

- **Command handlers**:
    - Use **repository ports** for aggregate persistence (write model)
    - Return command-specific results (NOT full domain entities)
    - Publish domain events after successful commit
- **Query handlers**:
    - Use **data ports** for read model access (projections)
    - Return optimized query results (denormalized DTOs)
    - Read-only transactions
    - NO event publishing

**Example:**

```java
// ✅ CORRECT: Query handler using data port (read model)
@Component
public class GetConsignmentQueryHandler {

    private final StockConsignmentViewRepository viewRepository; // Data port, not repository port

    @Transactional(readOnly = true)
    public ConsignmentQueryResult handle(GetConsignmentQuery query) {
        StockConsignmentView view = viewRepository.findByTenantIdAndId(
            query.getTenantId(), query.getConsignmentId()
        ).orElseThrow(() -> new ConsignmentNotFoundException(
            query.getConsignmentId().getValueAsString()
        ));

        return ConsignmentQueryResult.builder()
            .id(view.getId())
            .batchNumber(view.getBatchNumber())
            .status(view.getStatus())
            .createdAt(view.getCreatedAt())
            .build();
    }
}
```

#### ❌ **AVOID: Fat Handlers**

- **Problem**: Handlers with too many responsibilities
- **Signs**: Large classes with multiple concerns, complex logic
- **Solution**: Split into multiple focused handlers

#### ❌ **AVOID: Business Logic in Handlers**

- **Problem**: Business logic in application layer
- **Signs**: Complex calculations, validation logic (use ApplicationValidator instead)
- **Solution**: Move to domain layer

**Example:**

```java
// ❌ WRONG: Business logic in handler
@Component
public class ConfirmConsignmentCommandHandler {

    @Transactional
    public ConfirmConsignmentResult handle(ConfirmConsignmentCommand command) {
        StockConsignment consignment = repository.findById(command.getConsignmentId())
            .orElseThrow();

        // ❌ WRONG: Business logic in handler
        if (consignment.getLines().isEmpty()) {
            throw new IllegalStateException("Cannot confirm empty consignment");
        }
        if (consignment.getStatus() != ConsignmentStatus.PENDING) {
            throw new IllegalStateException("Can only confirm pending consignments");
        }

        consignment.setStatus(ConsignmentStatus.CONFIRMED);
        // ...
    }
}

// ✅ CORRECT: Business logic in domain
@Component
public class ConfirmConsignmentCommandHandler {

    @Transactional
    public ConfirmConsignmentResult handle(ConfirmConsignmentCommand command) {
        StockConsignment consignment = repository.findById(command.getConsignmentId())
            .orElseThrow();

        // Business logic delegated to aggregate
        consignment.confirm(); // Aggregate validates and changes state

        repository.save(consignment);
        // ...
    }
}
```

#### ❌ **AVOID: Direct Database Access**

- **Problem**: Bypassing domain layer
- **Signs**: Direct JPA repository calls, SQL queries
- **Solution**: Use domain services and repositories

#### ❌ **AVOID: Tight Coupling**

- **Problem**: Hard dependencies on concrete implementations
- **Signs**: Direct instantiation, concrete class references
- **Solution**: Use dependency injection and interfaces (ports)

#### ❌ **AVOID: Returning Domain Entities from Commands**

- **Problem**: Violates CQRS principles
- **Signs**: Command handlers returning full aggregates
- **Solution**: Return command-specific result DTOs

**Example:**

```java
// ❌ WRONG: Returning domain entity from command
@Transactional
public StockConsignment handle(ConfirmConsignmentCommand command) {
    StockConsignment consignment = repository.findById(command.getConsignmentId())
        .orElseThrow();
    consignment.confirm();
    repository.save(consignment);
    return consignment; // ❌ Returning full aggregate
}

// ✅ CORRECT: Returning command result DTO
@Transactional
public ConfirmConsignmentResult handle(ConfirmConsignmentCommand command) {
    StockConsignment consignment = repository.findById(command.getConsignmentId())
        .orElseThrow();
    consignment.confirm();
    repository.save(consignment);

    // ✅ Return focused result
    return ConfirmConsignmentResult.builder()
        .consignmentId(consignment.getId())
        .status(consignment.getStatus())
        .confirmedAt(LocalDateTime.now())
        .build();
}
```

#### ❌ **AVOID: Query Handlers Using Repository Ports**

- **Problem**: Queries accessing write model directly
- **Signs**: Query handlers injecting repository ports instead of data ports
- **Solution**: Use data ports for read models (projections)
- **Exception**: Critical queries requiring immediate consistency may use repository ports

**Example:**

```java
// ❌ WRONG: Query handler using repository port (write model)
@Component
public class GetConsignmentQueryHandler {
    private final StockConsignmentRepository repository; // ❌ Repository port (write model)

    @Transactional(readOnly = true)
    public ConsignmentQueryResult handle(GetConsignmentQuery query) {
        return repository.findById(query.getConsignmentId()) // ❌ Querying aggregate
            .map(this::toQueryResult)
            .orElseThrow();
    }
}

// ✅ CORRECT: Query handler using data port (read model)
@Component
public class GetConsignmentQueryHandler {
    private final StockConsignmentViewRepository viewRepository; // ✅ Data port (read model)

    @Transactional(readOnly = true)
    public ConsignmentQueryResult handle(GetConsignmentQuery query) {
        return viewRepository.findById(query.getConsignmentId()) // ✅ Querying view
            .map(this::toQueryResult)
            .orElseThrow();
    }
}
```

### Port Placement Guidelines

**CRITICAL**: Repository interfaces belong in **Application Service Layer**, NOT in Domain Core.

- **Repository Ports** (`port.repository`): For aggregate persistence (write model)
- **Data Ports** (`port.data`): For read model queries (projections/views)
- **Service Ports** (`port.service`): For external service integrations
- **Event Publisher Ports** (`port.messaging`): For event publishing

**Example:**

```java
// ✅ CORRECT: Repository port in application service layer
package com.ccbsa.wms.stock.application.service.port.repository;

public interface StockConsignmentRepository {
    void save(StockConsignment consignment);
    Optional<StockConsignment> findById(ConsignmentId id);
    Optional<StockConsignment> findByTenantIdAndId(TenantId tenantId, ConsignmentId id);
}
```

### Common Anti-Patterns to Avoid

1. **Fat Handlers**: Handlers with too many responsibilities
2. **Business Logic in Application Layer**: Complex logic outside domain
3. **Direct Database Access**: Bypassing domain layer
4. **Tight Coupling**: Hard dependencies on concrete implementations
5. **Transaction Anemia**: Too large transaction boundaries
6. **Error Swallowing**: Catching exceptions without proper handling
7. **God Handlers**: Single handler for multiple use cases
8. **Returning Domain Entities**: Commands returning full aggregates instead of result DTOs
9. **Repository Port Misuse**: Query handlers using repository ports instead of data ports

---

## Data Access Module

### Module Purpose

The data access module (`{service}-dataaccess`) contains database adapters and implements repository interfaces. It handles data persistence and retrieval with **mandatory**
distributed caching.

### Package Structure

```
com.ccbsa.wms.{service}.dataaccess/
├── adapter/                           # Repository and data adapters
│   ├── {DomainObject}RepositoryAdapter.java              # Base JPA repository adapter
│   ├── Cached{DomainObject}RepositoryAdapter.java        # MANDATORY: Cached decorator (@Primary)
│   └── {DomainObject}ViewRepositoryAdapter.java          # Data adapter (read model)
├── entity/                            # JPA entities
│   ├── {DomainObject}Entity.java                         # Aggregate entity
│   └── {DomainObject}ViewEntity.java                     # View entity (read model)
├── mapper/                            # Entity mappers
│   ├── {DomainObject}EntityMapper.java
│   └── {DomainObject}ViewEntityMapper.java
└── jpa/                               # JPA repositories
    ├── {DomainObject}JpaRepository.java
    └── {DomainObject}ViewJpaRepository.java
```

### Clean Code Principles

#### ✅ **DO: Lombok Usage (RECOMMENDED)**

- **JPA Entities**: Use `@Getter`, `@Setter`, `@NoArgsConstructor` for JPA entities
- **Adapters and Mappers**: Use `@Slf4j` for logging and `@RequiredArgsConstructor` for dependency injection
- **Anti-Patterns**: Avoid `@Data` on JPA entities (can cause issues with lazy loading and equals/hashCode)

**Example:**

```java
// ✅ CORRECT: JPA entity with Lombok
@Entity
@Table(name = "stock_consignments", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class StockConsignmentEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
}

// ✅ CORRECT: Repository adapter with Lombok
@Slf4j
@Repository
@RequiredArgsConstructor
public class StockConsignmentRepositoryAdapter implements StockConsignmentRepository {
    private final StockConsignmentJpaRepository jpaRepository;
    private final StockConsignmentEntityMapper mapper;
}

// ✅ CORRECT: Entity mapper with Lombok
@Slf4j
@Component
@RequiredArgsConstructor
public class StockConsignmentEntityMapper {
    // Mapper implementation
}
```

#### ✅ **DO: Repository Pattern**

- Implement repository interfaces from application service layer
- Use proper naming conventions (`{DomainObject}RepositoryAdapter`)
- Handle exceptions appropriately
- **CRITICAL**: All repository adapters MUST set PostgreSQL `search_path` for schema-per-tenant pattern

**Example:**

```java
@Repository
public class StockConsignmentRepositoryAdapter implements StockConsignmentRepository {

    private final StockConsignmentJpaRepository jpaRepository;
    private final StockConsignmentEntityMapper mapper;
    private final TenantSchemaResolver schemaResolver;
    private final TenantSchemaProvisioner schemaProvisioner;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(StockConsignment consignment) {
        // 1. Verify TenantContext is set
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext must be set before saving");
        }

        // 2. Resolve schema name
        String schemaName = schemaResolver.resolveSchema();

        // 3. Ensure schema exists
        schemaProvisioner.ensureSchemaReady(schemaName);

        // 4. Validate schema name (prevent SQL injection)
        validateSchemaName(schemaName);

        // 5. Set search_path
        Session session = entityManager.unwrap(Session.class);
        setSearchPath(session, schemaName);

        // 6. Save entity
        StockConsignmentEntity entity = mapper.toEntity(consignment);
        jpaRepository.save(entity);
    }

    private void setSearchPath(Session session, String schemaName) {
        session.doWork(connection -> executeSetSearchPath(connection, schemaName));
    }

    @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
            justification = "Schema name validated against expected patterns and escaped")
    private void executeSetSearchPath(Connection connection, String schemaName) {
        try (Statement stmt = connection.createStatement()) {
            String sql = String.format("SET search_path TO %s", escapeIdentifier(schemaName));
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set database schema", e);
        }
    }

    private String escapeIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }
        if ("public".equals(schemaName)) {
            return;
        }
        if (!schemaName.matches("^tenant_[a-zA-Z0-9_]+_schema$")) {
            throw new IllegalArgumentException(
                String.format("Invalid schema name format: '%s'", schemaName)
            );
        }
    }
}
```

#### ✅ **DO: Entity Mapping**

- Use proper JPA annotations
- Implement proper equals/hashCode
- Handle lazy loading correctly
- Separate JPA entities from domain entities
- Use `@Table(schema = "tenant_schema")` for tenant-aware entities

**Example:**

```java
@Entity
@Table(name = "stock_consignments", schema = "tenant_schema")
public class StockConsignmentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConsignmentStatus status;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    // Getters and setters
}
```

#### ✅ **DO: Optimistic Locking with Version Fields**

- Use `@Version` annotation for optimistic locking
- **CRITICAL**: Never set `version = 0` for new entities in mapper
- Always check entity existence before saving
- Preserve JPA managed state when updating existing entities

**Example:**

```java
// ✅ CORRECT: Entity mapper handling version correctly
@Component
public class StockConsignmentEntityMapper {

    public StockConsignmentEntity toEntity(StockConsignment consignment) {
        StockConsignmentEntity entity = new StockConsignmentEntity();
        entity.setId(consignment.getId().getValue());
        entity.setTenantId(consignment.getTenantId().getValue());
        entity.setBatchNumber(consignment.getBatchNumber().getValue());
        entity.setStatus(consignment.getStatus());
        entity.setCreatedAt(consignment.getCreatedAt());
        entity.setLastModifiedAt(consignment.getLastModifiedAt());

        // ✅ Only set version if entity already exists (version > 0)
        int domainVersion = consignment.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }
        // For new entities (version == 0), don't set version - let Hibernate manage it

        return entity;
    }

    public StockConsignment toDomain(StockConsignmentEntity entity) {
        return StockConsignment.builder()
            .id(ConsignmentId.of(entity.getId()))
            .tenantId(TenantId.of(entity.getTenantId()))
            .batchNumber(BatchNumber.of(entity.getBatchNumber()))
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .lastModifiedAt(entity.getLastModifiedAt())
            .version(entity.getVersion() != null ? entity.getVersion().intValue() : 0)
            .buildWithoutEvents();
    }
}
```

#### ✅ **DO: Cached Repository Adapters (MANDATORY)**

- **ALL repository adapters MUST have cached decorator**
- Use decorator pattern extending `CachedRepositoryDecorator`
- Annotate with `@Primary` to ensure injection over base adapter
- Configure TTL in `application.yml`
- Implement event-driven cache invalidation

**Example:**

```java
@Repository
@Primary  // ✅ Ensures this adapter is injected instead of base adapter
public class CachedStockConsignmentRepositoryAdapter
        extends CachedRepositoryDecorator<StockConsignment, ConsignmentId>
        implements StockConsignmentRepository {

    private final StockConsignmentRepositoryAdapter baseRepository;

    public CachedStockConsignmentRepositoryAdapter(
            StockConsignmentRepositoryAdapter baseRepository,
            RedisTemplate<String, Object> redisTemplate,
            MeterRegistry meterRegistry
    ) {
        super(
            baseRepository,
            redisTemplate,
            CacheNamespace.STOCK_CONSIGNMENTS.getValue(),
            Duration.ofMinutes(30),
            meterRegistry
        );
        this.baseRepository = baseRepository;
    }

    @Override
    public Optional<StockConsignment> findByTenantIdAndId(TenantId tenantId, ConsignmentId id) {
        return findWithCache(
            tenantId,
            id.getValue(),
            entityId -> baseRepository.findByTenantIdAndId(tenantId, id)
        );
    }

    @Override
    public void save(StockConsignment consignment) {
        // Write-through: Save to database + update cache
        baseRepository.save(consignment);

        saveWithCache(
            consignment.getTenantId(),
            consignment.getId().getValue(),
            consignment,
            obj -> obj
        );
    }
}
```

#### ✅ **DO: Query Optimization**

- Use appropriate query methods
- Implement pagination for large datasets
- Use projections for read-only operations
- Create indexes for common queries

#### ✅ **DO: Transaction Management**

- Use appropriate transaction boundaries
- Handle rollback scenarios
- Use read-only transactions for queries

#### ❌ **AVOID: Anemic Repositories**

- **Problem**: Repositories with only CRUD operations
- **Signs**: Generic save/find/delete methods only
- **Solution**: Add domain-specific query methods

#### ❌ **AVOID: N+1 Query Problem**

- **Problem**: Multiple database calls for related data
- **Signs**: Loops with database calls, lazy loading issues
- **Solution**: Use joins, projections, or batch loading

#### ❌ **AVOID: Leaky Abstractions**

- **Problem**: Exposing database-specific details
- **Signs**: JPA annotations in domain objects, SQL in repositories
- **Solution**: Use proper mapping and abstraction

#### ❌ **AVOID: Fat Repositories**

- **Problem**: Repositories with too many responsibilities
- **Signs**: Large classes with multiple concerns
- **Solution**: Split into focused repositories

#### ❌ **AVOID: Missing Schema-Per-Tenant Support**

- **Problem**: Not setting `search_path` for tenant-specific queries
- **Signs**: Queries returning data from wrong tenant
- **Solution**: Always set `search_path` before querying by tenantId

#### ❌ **AVOID: Missing Cached Decorator**

- **Problem**: Repository adapter without cached decorator
- **Signs**: No `Cached{DomainObject}RepositoryAdapter` class
- **Solution**: Create cached decorator with `@Primary` annotation

#### ❌ **AVOID: Version Field Errors**

- **Problem**: Setting `version = 0` for new entities causes optimistic locking errors
- **Signs**: "Row was updated or deleted by another transaction" errors
- **Solution**: Don't set version for new entities, let Hibernate manage it

### Common Anti-Patterns to Avoid

1. **Anemic Repositories**: Only CRUD operations
2. **N+1 Query Problem**: Multiple database calls
3. **Leaky Abstractions**: Exposing database details
4. **Fat Repositories**: Too many responsibilities
5. **SQL in Repositories**: Database-specific code
6. **Lazy Loading Issues**: Improper lazy loading handling
7. **Transaction Anemia**: Too large transaction boundaries
8. **Missing Schema Support**: Not setting search_path for tenant isolation
9. **No Caching**: Missing cached decorator implementation
10. **Version Field Misuse**: Setting version = 0 for new entities

---

## Application Module

### Module Purpose

The application module (`{service}-application`) contains REST API controllers and handles HTTP requests. It implements CQRS patterns with separate command and query controllers.

### Package Structure

```
com.ccbsa.wms.{service}.application/
├── command/                           # Command controllers (write endpoints)
├── query/                             # Query controllers (read endpoints)
├── dto/                               # DTOs and mappers
│   ├── command/                       # Command DTOs
│   ├── query/                         # Query DTOs
│   ├── common/                        # Shared DTOs used by both command and query
│   └── mapper/                        # DTO mappers
└── exception/                         # Exception handlers
```

### Clean Code Principles

#### ✅ **DO: Lombok Usage (MANDATORY)**

- **Controllers**: Use `@Slf4j` for logging and `@RequiredArgsConstructor` for dependency injection
- **DTOs**: Use `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` for all DTOs
- **Mappers**: Use `@Component` and `@RequiredArgsConstructor` if mapper has dependencies
- **Anti-Patterns**: Avoid `@Data` (too broad), prefer specific annotations

**Example:**

```java
// ✅ CORRECT: Controller with Lombok
@Slf4j
@RestController
@RequestMapping("/api/v1/stock/consignments")
@Tag(name = "Stock Consignment Commands")
@RequiredArgsConstructor
public class StockConsignmentCommandController {
    private final ConfirmConsignmentCommandHandler confirmHandler;
    private final StockConsignmentDTOMapper mapper;
}

// ✅ CORRECT: DTO with Lombok
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmConsignmentCommandDTO {
    @NotNull(message = "Consignment ID is required")
    private String consignmentId;
    
    @NotNull(message = "Tenant ID is required")
    private String tenantId;
}

// ✅ CORRECT: Mapper with Lombok
@Component
@RequiredArgsConstructor
public class StockConsignmentDTOMapper {
    private final ObjectMapper objectMapper;
}
```

#### ✅ **DO: CQRS Separation**

- Separate command and query controllers
- Use appropriate HTTP methods (POST/PUT/DELETE for commands, GET for queries)
- Follow REST conventions
- Return standardized API responses

**Example:**

```java
// ✅ CORRECT: Separate command and query controllers
@RestController
@RequestMapping("/api/v1/stock/consignments")
@Tag(name = "Stock Consignment Commands")
public class StockConsignmentCommandController {

    private final ConfirmConsignmentCommandHandler confirmHandler;
    private final StockConsignmentDTOMapper mapper;

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm consignment")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<ConfirmConsignmentResultDTO>> confirmConsignment(
            @PathVariable String id,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        ConfirmConsignmentCommand command = mapper.toConfirmCommand(id, tenantId);
        ConfirmConsignmentResult result = confirmHandler.handle(command);
        ConfirmConsignmentResultDTO dto = mapper.toDTO(result);
        return ApiResponseBuilder.ok(dto);
    }
}

@RestController
@RequestMapping("/api/v1/stock/consignments")
@Tag(name = "Stock Consignment Queries")
public class StockConsignmentQueryController {

    private final GetConsignmentQueryHandler queryHandler;
    private final StockConsignmentDTOMapper mapper;

    @GetMapping("/{id}")
    @Operation(summary = "Get consignment by ID")
    @PreAuthorize("hasRole('VIEWER')")
    public ResponseEntity<ApiResponse<ConsignmentQueryResultDTO>> getConsignment(
            @PathVariable String id,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        GetConsignmentQuery query = mapper.toQuery(id, tenantId);
        ConsignmentQueryResult result = queryHandler.handle(query);
        ConsignmentQueryResultDTO dto = mapper.toDTO(result);
        return ApiResponseBuilder.ok(dto);
    }
}
```

#### ✅ **DO: Response Standardization**

- Use standardized `ApiResponse<T>` format
- Include proper HTTP status codes
- Provide consistent error handling
- Use `ApiResponseBuilder` for creating responses

**Example:**

```java
// ✅ CORRECT: Standardized responses
return ApiResponseBuilder.ok(dto);                    // 200 OK
return ApiResponseBuilder.created(dto);               // 201 Created
return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error); // 404 Not Found
```

#### ✅ **DO: Security Implementation**

- Use method-level security with `@PreAuthorize`
- Validate user permissions
- Extract tenant context from headers (`X-Tenant-Id`)
- Implement correlation ID tracking (`X-Correlation-Id`)

#### ✅ **DO: Global Exception Handling**

- **MANDATORY**: Extend `BaseGlobalExceptionHandler` from `common-application`
- Add service-specific exception handlers
- Return consistent error responses using `ApiError` and `ApiResponse`
- Include request ID and path in error responses

**Example:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(ConsignmentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleConsignmentNotFound(
            ConsignmentNotFoundException ex, HttpServletRequest request) {
        String requestId = RequestContext.getRequestId(request);
        String path = RequestContext.getRequestPath(request);

        logger.warn("Consignment not found: {} - RequestId: {}, Path: {}",
                ex.getMessage(), requestId, path);

        ApiError error = ApiError.builder("RESOURCE_NOT_FOUND", ex.getMessage())
                .path(path)
                .requestId(requestId)
                .build();
        return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error);
    }
}
```

#### ✅ **DO: API Documentation**

- Use OpenAPI annotations (`@Tag`, `@Operation`)
- Document all endpoints with meaningful descriptions
- Include parameter descriptions
- Document response types

#### ❌ **AVOID: Fat Controllers**

- **Problem**: Controllers with too many responsibilities
- **Signs**: Large classes with multiple concerns
- **Solution**: Split into focused controllers (command vs query)

#### ❌ **AVOID: Business Logic in Controllers**

- **Problem**: Business logic in presentation layer
- **Signs**: Complex calculations, validation logic
- **Solution**: Move to application service layer

**Example:**

```java
// ❌ WRONG: Business logic in controller
@PostMapping("/{id}/confirm")
public ResponseEntity<?> confirmConsignment(@PathVariable String id) {
    StockConsignment consignment = repository.findById(UUID.fromString(id)).orElseThrow();

    // ❌ Business logic in controller
    if (consignment.getLines().isEmpty()) {
        throw new IllegalStateException("Cannot confirm empty consignment");
    }

    consignment.setStatus(ConsignmentStatus.CONFIRMED);
    repository.save(consignment);
    return ResponseEntity.ok(consignment);
}

// ✅ CORRECT: Delegate to handler
@PostMapping("/{id}/confirm")
public ResponseEntity<ApiResponse<ConfirmConsignmentResultDTO>> confirmConsignment(
        @PathVariable String id,
        @RequestHeader("X-Tenant-Id") String tenantId
) {
    ConfirmConsignmentCommand command = mapper.toConfirmCommand(id, tenantId);
    ConfirmConsignmentResult result = confirmHandler.handle(command); // Business logic in handler
    ConfirmConsignmentResultDTO dto = mapper.toDTO(result);
    return ApiResponseBuilder.ok(dto);
}
```

#### ❌ **AVOID: Direct Database Access**

- **Problem**: Bypassing application service layer
- **Signs**: Direct repository calls, SQL queries
- **Solution**: Use application service layer

#### ❌ **AVOID: Exposing Domain Entities**

- **Problem**: Returning domain entities directly in API responses
- **Signs**: Controllers returning aggregates
- **Solution**: Use DTOs as anti-corruption layer

**Example:**

```java
// ❌ WRONG: Exposing domain entity
@GetMapping("/{id}")
public ResponseEntity<StockConsignment> getConsignment(@PathVariable String id) {
    return ResponseEntity.ok(repository.findById(UUID.fromString(id)).orElseThrow());
}

// ✅ CORRECT: Using DTO
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<ConsignmentQueryResultDTO>> getConsignment(
        @PathVariable String id,
        @RequestHeader("X-Tenant-Id") String tenantId
) {
    GetConsignmentQuery query = mapper.toQuery(id, tenantId);
    ConsignmentQueryResult result = queryHandler.handle(query);
    ConsignmentQueryResultDTO dto = mapper.toDTO(result);
    return ApiResponseBuilder.ok(dto);
}
```

#### ❌ **AVOID: Inconsistent Error Handling**

- **Problem**: Different error handling patterns
- **Signs**: Inconsistent error responses, different status codes
- **Solution**: Use centralized global exception handler extending `BaseGlobalExceptionHandler`

#### ✅ **DO: Shared DTOs Pattern**

- **Use `dto.common` package** for DTOs that are **identical in structure** and used by **both command and query** DTOs
- **Avoid duplicate DTOs** with the same name in both `dto.command` and `dto.query` packages
- **Keep DTOs separate** when they have different structures (e.g., validation annotations, different field types) even if they represent the same domain concept

**When to use `dto.common`:**
- DTOs that are **identical in structure** and used by both command and query DTOs
- Nested/value DTOs that represent the same domain concept without variation
- Examples: `LocationCoordinatesDTO` (zone, aisle, rack, level - same in both contexts)

**When to keep separate:**
- DTOs with **different structures** for command vs query (e.g., validation annotations, different field types)
- DTOs used **only in one package** (command or query)
- DTOs that represent **different concerns** even if they share a name

**Example:**

```java
// ✅ CORRECT: Shared DTO in dto.common package
package com.ccbsa.wms.location.application.dto.common;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class LocationCoordinatesDTO {
    private String zone;
    private String aisle;
    private String rack;
    private String level;
}

// ✅ CORRECT: Used in both command and query DTOs
// In CreateLocationResultDTO (command):
import com.ccbsa.wms.location.application.dto.common.LocationCoordinatesDTO;
private LocationCoordinatesDTO coordinates;

// In LocationQueryResultDTO (query):
import com.ccbsa.wms.location.application.dto.common.LocationCoordinatesDTO;
private LocationCoordinatesDTO coordinates;
```

**Anti-Pattern:**

```java
// ❌ WRONG: Duplicate DTOs with same name in different packages
// dto.command.LocationCoordinatesDTO
public final class LocationCoordinatesDTO { ... }

// dto.query.LocationCoordinatesDTO  
public final class LocationCoordinatesDTO { ... }

// Forces FQCN usage in mapper:
private com.ccbsa.wms.location.application.dto.query.LocationCoordinatesDTO toQueryCoordinatesDTO(...) {
    return new com.ccbsa.wms.location.application.dto.query.LocationCoordinatesDTO(...);
}
```

### Correlation ID Handling

**CRITICAL**: All incoming requests must extract and set correlation ID for traceability.

1. **Request Interceptor**: Extract `X-Correlation-Id` header from incoming requests
2. **Context Setting**: Set correlation ID in `CorrelationContext` (ThreadLocal)
3. **Event Publishing**: Correlation ID automatically available via `CorrelationContext`
4. **Context Cleanup**: Clear `CorrelationContext` after request completion

### Common Anti-Patterns to Avoid

1. **Fat Controllers**: Controllers with too many responsibilities
2. **Business Logic in Controllers**: Complex logic in presentation layer
3. **Direct Database Access**: Bypassing application service layer
4. **Inconsistent Error Handling**: Different error patterns
5. **Missing Validation**: No input validation
6. **Security Bypass**: Missing security annotations
7. **Tight Coupling**: Hard dependencies on concrete implementations
8. **Exposing Domain Entities**: Returning aggregates instead of DTOs
9. **Missing Exception Handler**: Not extending `BaseGlobalExceptionHandler`
10. **Duplicate Shared DTOs**: Same DTO class in both `dto.command` and `dto.query` packages - use `dto.common` instead

---

## Messaging Module

### Module Purpose

The messaging module (`{service}-messaging`) handles event publishing and consumption. It implements event-driven patterns and ensures loose coupling between services.

### Package Structure

```
com.ccbsa.wms.{service}.messaging/
├── publisher/                         # Event publishers
│   └── {Service}EventPublisherImpl.java
├── listener/                          # Event listeners
│   ├── {ExternalEvent}Listener.java
│   ├── {DomainObject}ProjectionListener.java
│   └── {DomainObject}CacheInvalidationListener.java
├── mapper/                            # Event mappers (optional)
│   └── {DomainObject}EventMapper.java
└── config/                            # Kafka configuration
    └── {Service}Configuration.java
```

### Clean Code Principles

#### ✅ **DO: Lombok Usage (RECOMMENDED)**

- **Event Listeners and Publishers**: Use `@Slf4j` for logging and `@RequiredArgsConstructor` for dependency injection
- **Event Mappers**: Use `@Component` and `@RequiredArgsConstructor` if mapper has dependencies
- **Configuration Classes**: Use `@Configuration` with `@RequiredArgsConstructor` for dependency injection

**Example:**

```java
// ✅ CORRECT: Event listener with Lombok
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationAssignedEventListener {
    private final StockConsignmentRepository repository;
    private final EventStore eventStore;

    @KafkaListener(topics = "location-events", groupId = "stock-service")
    public void handle(LocationAssignedEvent event, Acknowledgment acknowledgment) {
        // Listener implementation
    }
}

// ✅ CORRECT: Event publisher with Lombok
@Slf4j
@Component
@RequiredArgsConstructor
public class StockManagementEventPublisherImpl implements StockManagementEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
}

// ✅ CORRECT: Configuration with Lombok
@Configuration
@Import({KafkaConfig.class})
@RequiredArgsConstructor
public class StockManagementConfiguration {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
}
```

#### ✅ **DO: Event-Driven Design**

- Use domain events for communication between services
- Implement proper event serialization
- Handle event versioning
- Include event metadata for traceability

**Example:**

```java
@Component
public class StockManagementEventPublisherImpl implements StockManagementEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "stock-events";

    @Override
    public void publish(DomainEvent<?> event) {
        // Inject event metadata for traceability
        injectEventMetadata(event);

        String key = event.getAggregateId().toString();
        kafkaTemplate.send(TOPIC, key, event);
    }

    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(this::publish);
    }

    private void injectEventMetadata(DomainEvent<?> event) {
        String correlationId = CorrelationContext.getCorrelationId();
        String userId = TenantContext.getUserId() != null
                ? TenantContext.getUserId().getValue()
                : null;

        if (correlationId != null || userId != null) {
            EventMetadata.Builder metadataBuilder = EventMetadata.builder();
            if (correlationId != null) {
                metadataBuilder.correlationId(correlationId);
            }
            if (userId != null) {
                metadataBuilder.userId(userId);
            }
            event.setMetadata(metadataBuilder.build());
        }
    }
}
```

#### ✅ **DO: Error Handling**

- Implement retry mechanisms with exponential backoff
- Use dead letter queues for failed events
- Handle poison messages
- Implement idempotency checks

**Example:**

```java
@Component
public class LocationAssignedEventListener {

    private final StockConsignmentRepository repository;
    private final EventStore eventStore;

    @KafkaListener(
        topics = "location-events",
        groupId = "stock-service",
        containerFactory = "externalEventKafkaListenerContainerFactory"
    )
    public void handle(LocationAssignedEvent event, Acknowledgment acknowledgment) {
        try {
            // Extract and set correlation ID
            extractAndSetCorrelationId(event);

            // Check idempotency
            if (eventStore.exists(event.getEventId())) {
                acknowledgment.acknowledge();
                return;
            }

            // Process event
            processEvent(event);

            // Store event ID for idempotency
            eventStore.store(event.getEventId());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Failed to process event", e);
            throw e; // Will trigger retry
        } finally {
            CorrelationContext.clear();
        }
    }

    private void extractAndSetCorrelationId(LocationAssignedEvent event) {
        EventMetadata metadata = event.getMetadata();
        if (metadata != null && metadata.getCorrelationId() != null) {
            CorrelationContext.setCorrelationId(metadata.getCorrelationId());
        }
    }
}
```

#### ✅ **DO: Event Versioning**

- Support event schema evolution
- Maintain backward compatibility
- Use proper event naming (`{DomainObject}{Action}Event`)
- Include version field in events

#### ✅ **DO: Idempotency**

- Ensure events can be processed multiple times
- Use idempotency keys (event IDs)
- Handle duplicate events gracefully
- Store processed event IDs

#### ✅ **DO: Kafka Configuration**

- **CRITICAL**: Import `KafkaConfig` via `@Import(KafkaConfig.class)`
- **CRITICAL**: Use `@Qualifier("kafkaObjectMapper")` for all Kafka ObjectMapper injection
- Configure consumer factories with proper deserialization
- Set up error handlers with retry logic
- Configure concurrency and batch sizes

**Example:**

```java
@Configuration
@Import({ServiceSecurityConfig.class, MultiTenantDataAccessConfig.class, KafkaConfig.class})
public class StockManagementConfiguration {

    @Bean("externalEventConsumerFactory")
    public ConsumerFactory<String, Object> externalEventConsumerFactory(
            @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        // Configuration using kafkaObjectMapper for type information
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(Object.class, kafkaObjectMapper);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);

        DefaultKafkaConsumerFactory<String, Object> factory =
                new DefaultKafkaConsumerFactory<>(configProps);
        factory.setValueDeserializer(new ErrorHandlingDeserializer<>(jsonDeserializer));
        return factory;
    }
}
```

#### ✅ **DO: Cache Invalidation Listeners (MANDATORY)**

- All services MUST implement cache invalidation listeners
- Listen to domain events and invalidate affected caches
- Extend `CacheInvalidationEventListener` from `common-cache`

**Example:**

```java
@Component
public class StockConsignmentCacheInvalidationListener extends CacheInvalidationEventListener {

    public StockConsignmentCacheInvalidationListener(LocalCacheInvalidator cacheInvalidator) {
        super(cacheInvalidator);
    }

    @KafkaListener(
        topics = "stock-events",
        groupId = "stock-cache-invalidation",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleStockEvent(Object event) {
        if (event instanceof ConsignmentCreatedEvent created) {
            // No invalidation - cache-aside pattern
            log.debug("Consignment created, no cache invalidation needed");
        } else if (event instanceof ConsignmentUpdatedEvent updated) {
            // Invalidate entity cache
            invalidateForEvent(updated, CacheNamespace.STOCK_CONSIGNMENTS.getValue());
        }
    }
}
```

#### ❌ **AVOID: Tight Coupling**

- **Problem**: Direct dependencies between services
- **Signs**: Hard-coded service names, direct calls
- **Solution**: Use events for communication

#### ❌ **AVOID: Event Anemia**

- **Problem**: Events with no meaningful data
- **Signs**: Empty events, generic event types
- **Solution**: Include relevant business data in events

#### ❌ **AVOID: Missing Error Handling**

- **Problem**: No error handling for message processing
- **Signs**: Unhandled exceptions, message loss
- **Solution**: Implement proper error handling with retries and DLQ

#### ❌ **AVOID: Event Spaghetti**

- **Problem**: Too many events with unclear purposes
- **Signs**: Confusing event flow, hard to understand
- **Solution**: Simplify event model

#### ❌ **AVOID: Missing ObjectMapper Qualifier**

- **Problem**: Using wrong ObjectMapper for Kafka serialization
- **Signs**: Type information missing from events, deserialization errors
- **Solution**: Always use `@Qualifier("kafkaObjectMapper")`

### Event Metadata and Traceability

**Event Publishing Flow:**

1. Command handler executes business logic
2. Domain events generated by aggregate
3. Event publisher extracts correlation ID from `CorrelationContext`
4. Event publisher extracts user ID from `TenantContext`
5. Event publisher creates `EventMetadata` and sets on event
6. Event published to Kafka with metadata

**Event Consumption Flow:**

1. Event listener receives event from Kafka
2. Event listener extracts correlation ID from event metadata
3. Event listener sets correlation ID in `CorrelationContext`
4. Event listener processes event (may publish new events)
5. New events published will include same correlation ID
6. Correlation context cleared after processing

### Common Anti-Patterns to Avoid

1. **Tight Coupling**: Direct service dependencies
2. **Event Anemia**: Events without meaningful data
3. **Missing Error Handling**: No error handling
4. **Event Spaghetti**: Too many confusing events
5. **Missing Versioning**: No event versioning
6. **No Idempotency**: Events not idempotent
7. **Synchronous Events**: Blocking event processing
8. **Missing Cache Invalidation**: No cache invalidation listeners
9. **Wrong ObjectMapper**: Not using `kafkaObjectMapper` for Kafka beans

---

## Container Module

### Module Purpose

The container module (`{service}-container`) provides application bootstrap and configuration. It wires together all modules and provides runtime configuration.

### Package Structure

```
com.ccbsa.wms.{service}.container/
├── config/                            # Configuration classes
│   ├── {Service}Configuration.java
│   ├── DatabaseConfig.java
│   ├── SecurityConfig.java
│   └── WebMvcConfig.java
├── health/                            # Health indicators
│   └── {Component}HealthIndicator.java
└── {Service}Application.java          # Main application class
```

### Clean Code Principles

#### ✅ **DO: Lombok Usage (RECOMMENDED)**

- **Configuration Classes**: Use `@Configuration` with `@RequiredArgsConstructor` for dependency injection
- **Health Indicators**: Use `@Component` with `@RequiredArgsConstructor` for dependency injection
- **Anti-Patterns**: Avoid field injection, prefer constructor injection via `@RequiredArgsConstructor`

**Example:**

```java
// ✅ CORRECT: Configuration with Lombok
@Configuration
@Import({ServiceSecurityConfig.class, MultiTenantDataAccessConfig.class, KafkaConfig.class})
@RequiredArgsConstructor
public class StockManagementConfiguration {
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> externalEventConsumerFactory(
            @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        // Configuration implementation
    }
}

// ✅ CORRECT: Health indicator with Lombok
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {
    private final DataSource dataSource;

    @Override
    public Health health() {
        // Health check implementation
    }
}
```

#### ✅ **DO: Configuration Management**

- Use centralized configuration
- Support environment-specific configs
- Validate configuration on startup
- Import necessary configurations (`@Import`)

**Example:**

```java
@Configuration
@Import({ServiceSecurityConfig.class, MultiTenantDataAccessConfig.class, KafkaConfig.class})
public class StockManagementConfiguration {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> externalEventConsumerFactory(
            @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        // Configuration implementation
        return factory;
    }
}
```

#### ✅ **DO: ObjectMapper Separation (CRITICAL)**

- **REST API ObjectMapper** (`@Primary`):
    - Built from `Jackson2ObjectMapperBuilder` in `WebMvcConfig`
    - NO type information included (clean JSON for frontend)
    - Used automatically by Spring MVC
- **Kafka ObjectMapper** (`kafkaObjectMapper`):
    - Provided by `KafkaConfig` in `common-messaging` module
    - Includes type information (@class property)
    - MUST be imported via `@Import(KafkaConfig.class)`
    - MUST be injected with `@Qualifier("kafkaObjectMapper")`
- **Redis Cache ObjectMapper** (`redisCacheObjectMapper`):
    - Provided by `CacheConfiguration` in `common-cache` module
    - Includes type information for polymorphic cache values
    - MUST be injected with `@Qualifier("redisCacheObjectMapper")`

**Example:**

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    @Primary  // Default ObjectMapper for REST API
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        // NO type information - clean JSON for REST API
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}
```

#### ✅ **DO: Dependency Injection**

- Use constructor injection (not field injection)
- Use proper bean scopes
- Avoid circular dependencies

#### ✅ **DO: Health Checks**

- Implement health check endpoints
- Monitor critical dependencies
- Provide meaningful health status

**Example:**

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connected")
                    .build();
            }
        } catch (SQLException e) {
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down().build();
    }
}
```

#### ✅ **DO: Logging Configuration**

- Use structured logging (Logback)
- Configure appropriate log levels
- Include correlation IDs in logs

#### ❌ **AVOID: Configuration Anemia**

- **Problem**: Hard-coded configuration values
- **Signs**: Magic numbers, hard-coded strings
- **Solution**: Use external configuration (application.yml)

#### ❌ **AVOID: Fat Configuration**

- **Problem**: Too many configuration classes
- **Signs**: Large configuration files, multiple concerns
- **Solution**: Split into focused configuration classes

#### ❌ **AVOID: Missing Validation**

- **Problem**: No configuration validation
- **Signs**: Runtime failures due to invalid config
- **Solution**: Validate configuration on startup

#### ❌ **AVOID: Tight Coupling**

- **Problem**: Hard dependencies on concrete implementations
- **Signs**: Direct instantiation, concrete class references
- **Solution**: Use dependency injection and interfaces

#### ❌ **AVOID: ObjectMapper Confusion**

- **Problem**: Using wrong ObjectMapper for specific use case
- **Signs**: Type information in REST responses, missing type info in Kafka events
- **Solution**: Use explicit `@Qualifier` for named ObjectMappers

### Common Anti-Patterns to Avoid

1. **Configuration Anemia**: Hard-coded values
2. **Fat Configuration**: Too many responsibilities
3. **Missing Validation**: No configuration validation
4. **Tight Coupling**: Hard dependencies
5. **Missing Health Checks**: No monitoring
6. **Poor Logging**: Inadequate logging configuration
7. **Magic Numbers**: Hard-coded values
8. **ObjectMapper Leakage**: Type information in REST API responses

---

## Frontend Module

### Module Purpose

The frontend module (`frontend-app`) provides the user interface with CQRS compliance and real-time event streaming.

### Package Structure

```
frontend-app/src/
├── features/                          # Feature modules
│   └── {feature-name}/
│       ├── components/                # React components
│       ├── pages/                     # Page components
│       ├── services/                  # API services
│       ├── types/                     # TypeScript types
│       └── hooks/                     # Custom hooks
├── services/                          # Common services
│   ├── api/                           # API client
│   ├── correlation/                   # Correlation ID service
│   └── logger/                        # Logging service
└── utils/                             # Utilities
```

### Clean Code Principles

#### ✅ **DO: CQRS-Compliant API Integration**

- Separate command and query API calls
- Use proper HTTP methods
- Handle responses appropriately

**Example:**

```typescript
// ✅ CORRECT: Separate command and query services
class ConsignmentCommandService {
  async confirmConsignment(id: string): Promise<ConfirmConsignmentResult> {
    const response = await apiClient.post(
      `/stock/consignments/${id}/confirm`,
      null,
      { headers: { 'X-Correlation-Id': correlationIdService.getCorrelationId() } }
    );
    return response.data;
  }
}

class ConsignmentQueryService {
  async getConsignment(id: string): Promise<ConsignmentQueryResult> {
    const response = await apiClient.get(
      `/stock/consignments/${id}`,
      { headers: { 'X-Correlation-Id': correlationIdService.getCorrelationId() } }
    );
    return response.data;
  }
}
```

#### ✅ **DO: Correlation ID Management**

- Generate correlation ID per user session
- Include in all API requests via `X-Correlation-Id` header
- Include in all log entries
- Clear on logout

**Example:**

```typescript
class CorrelationIdService {
  private correlationId: string | null = null;

  getOrCreateCorrelationId(): string {
    if (!this.correlationId) {
      this.correlationId = `web-${uuidv4()}`;
    }
    return this.correlationId;
  }

  clear(): void {
    this.correlationId = null;
  }
}
```

#### ✅ **DO: TypeScript Types**

- Define types for all DTOs
- Use strict type checking
- Avoid `any` type

**Example:**

```typescript
interface ConsignmentQueryResult {
  id: string;
  batchNumber: string;
  status: ConsignmentStatus;
  createdAt: string;
  lastModifiedAt: string;
}

enum ConsignmentStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  EXPIRED = 'EXPIRED'
}
```

#### ✅ **DO: Error Handling**

- Handle API errors gracefully
- Provide user feedback
- Log errors with correlation ID

**Example:**

```typescript
try {
  const result = await consignmentService.confirmConsignment(id);
  toast.success('Consignment confirmed successfully');
} catch (error) {
  logger.error('Failed to confirm consignment', {
    correlationId: correlationIdService.getCorrelationId(),
    error
  });
  toast.error('Failed to confirm consignment');
}
```

#### ❌ **AVOID: Missing Correlation ID**

- **Problem**: API requests without correlation ID
- **Signs**: No traceability for user actions
- **Solution**: Include correlation ID in all API requests

#### ❌ **AVOID: Type Unsafe Code**

- **Problem**: Using `any` type everywhere
- **Signs**: No type safety, runtime errors
- **Solution**: Define proper TypeScript types

---

## General Clean Code Principles

### Java Implementation Standards Guideline

This section provides coding standards and best practices for Java implementation to ensure consistency, readability, and maintainability across all modules.

#### Field Naming Conventions

- Use **camelCase** for field names
- Prefer **descriptive** names
- Boolean fields should be named without the `is` prefix unless necessary for clarity
    - Example: `enabled`, `active`, `visible`

#### Getter and Setter Methods

- Getter methods should follow the `get*()` pattern
- For boolean fields, getter methods should follow the `is*()` pattern
- Setter methods should follow the `set*()` pattern

**Example:**

```java
private boolean enabled;

public boolean isEnabled() {
    return enabled;
}

public void setEnabled(boolean enabled) {
    this.enabled = enabled;
}
```

### SOLID Principles

#### 1. **Single Responsibility Principle (SRP)**

- Each class should have only one reason to change
- Focus on one concern per class
- Keep methods small and focused

#### 2. **Open/Closed Principle (OCP)**

- Open for extension, closed for modification
- Use interfaces and abstractions
- Implement strategy patterns

#### 3. **Liskov Substitution Principle (LSP)**

- Derived classes must be substitutable for base classes
- Maintain behavioral contracts
- Avoid breaking inheritance hierarchies

#### 4. **Interface Segregation Principle (ISP)**

- Clients should not depend on interfaces they don't use
- Create focused, cohesive interfaces
- Avoid fat interfaces

#### 5. **Dependency Inversion Principle (DIP)**

- Depend on abstractions, not concretions
- Use dependency injection
- Implement proper layering

### Additional Principles

#### 6. **Don't Repeat Yourself (DRY)**

- Avoid code duplication
- Extract common functionality
- Use shared components

#### 7. **Keep It Simple, Stupid (KISS)**

- Prefer simple solutions
- Avoid over-engineering
- Make code readable

#### 8. **You Aren't Gonna Need It (YAGNI)**

- Don't implement features until needed
- Avoid premature optimization
- Focus on current requirements

## Architectural Anti-Patterns Summary

### Repository Interface Misplacement

**❌ WRONG:**

```java
// Domain core with repository interface
package com.ccbsa.wms.stock.domain.core.repository;
public interface StockConsignmentRepository { }
```

**✅ CORRECT:**

```java
// Application service with repository port
package com.ccbsa.wms.stock.application.service.port.repository;
public interface StockConsignmentRepository { }
```

### Infrastructure Concerns in Domain

**❌ WRONG:**

```java
@Entity
@Table(name = "stock_consignments")
public class StockConsignment extends AggregateRoot<ConsignmentId> {
    @Id private String id;
    @Column(name = "batch_number") private String batchNumber;
}
```

**✅ CORRECT:**

```java
// Domain entity (pure Java)
public class StockConsignment extends TenantAwareAggregateRoot<ConsignmentId> {
    private BatchNumber batchNumber;
}

// JPA entity (infrastructure)
@Entity
@Table(name = "stock_consignments", schema = "tenant_schema")
public class StockConsignmentEntity {
    @Id private UUID id;
    @Column(name = "batch_number") private String batchNumber;
}
```

### Missing Anti-Corruption Layer

**❌ WRONG:**

```java
@RestController
public class ConsignmentController {
    @GetMapping("/{id}")
    public ResponseEntity<StockConsignment> get(@PathVariable String id) {
        return ResponseEntity.ok(repository.findById(id).orElseThrow());
    }
}
```

**✅ CORRECT:**

```java
@RestController
public class ConsignmentQueryController {
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConsignmentQueryDTO>> get(@PathVariable String id) {
        GetConsignmentQuery query = mapper.toQuery(id, tenantId);
        ConsignmentQueryResult result = queryHandler.handle(query);
        ConsignmentQueryDTO dto = mapper.toDTO(result);
        return ApiResponseBuilder.ok(dto);
    }
}
```

## Conclusion

These clean code guidelines ensure that each module in the microservice architecture maintains high quality, readability, and maintainability. By following these principles and
avoiding the common anti-patterns, developers can create production-grade code that aligns with the architectural principles of Domain-Driven Design, Clean Hexagonal Architecture,
CQRS, and Event-Driven Design.

**Remember**: Clean code is not just about following rules—it's about writing code that is easy to understand, maintain, and extend. Always prioritize readability and
maintainability over cleverness or brevity.

### Implementation Checklists

#### Domain Core Module

- [ ] Entity extends `TenantAwareAggregateRoot<ID>` (not manual ID field)
- [ ] Entity uses public static `Builder builder()` pattern
- [ ] Builder validates all required fields in `build()` method
- [ ] Value objects are immutable with `final` fields
- [ ] Value objects implement `equals()`, `hashCode()`, `toString()`
- [ ] Domain events extend `{Service}Event<T>`
- [ ] Business logic encapsulated in entity methods
- [ ] No Lombok annotations
- [ ] No Spring/JPA annotations
- [ ] No external dependencies (except `common-domain`)

#### Application Service Module

- [ ] Command handlers annotated with `@Component` and `@Transactional`
- [ ] Query handlers annotated with `@Component` and `@Transactional(readOnly = true)`
- [ ] Repository interfaces defined in `port.repository` package
- [ ] Data interfaces defined in `port.data` package
- [ ] Service port interfaces defined in `port.service` package
- [ ] Events published after successful commit
- [ ] Domain events cleared after publishing
- [ ] Command results return DTOs, not domain entities
- [ ] Query results return optimized DTOs
- [ ] Command handlers use repository ports (NOT data ports)
- [ ] Query handlers use data ports (NOT repository ports)
- [ ] **Lombok used for Command/Query/Result DTOs** (`@Getter`, `@Builder`, etc.)
- [ ] **Handlers use `@Slf4j` for logging** (instead of manual logger declaration)
- [ ] **Handlers use `@RequiredArgsConstructor` for dependency injection**

#### Application Layer Module

- [ ] Command controllers separate from query controllers
- [ ] Controllers annotated with `@RestController` and `@Tag`
- [ ] Endpoints annotated with `@Operation` for OpenAPI
- [ ] Security annotations (`@PreAuthorize`) on endpoints
- [ ] Tenant context extracted from headers (`X-Tenant-Id`)
- [ ] DTOs used for all API communication
- [ ] DTO mappers convert between DTOs and domain objects
- [ ] Global exception handler extends `BaseGlobalExceptionHandler`
- [ ] **Lombok used for all DTOs** (`@Getter`, `@Setter`, `@Builder`, etc.)
- [ ] **Controllers use `@Slf4j` for logging** (instead of manual logger declaration)
- [ ] **Controllers use `@RequiredArgsConstructor` for dependency injection**

#### Data Access Module

- [ ] Repository adapters implement application service repository interfaces
- [ ] JPA entities separate from domain entities
- [ ] Entity mappers convert between JPA and domain entities
- [ ] Multi-tenant schema resolution using `search_path`
- [ ] All repository adapters have cached decorators with `@Primary`
- [ ] Cache invalidation listeners implemented
- [ ] Version field handled correctly (never set to 0 for new entities)
- [ ] **Lombok used for JPA entities** (`@Getter`, `@Setter`, `@NoArgsConstructor`)
- [ ] **Lombok used for adapters and mappers** (`@Slf4j`, `@RequiredArgsConstructor`)

#### Messaging Module

- [ ] Event publisher implements `EventPublisher` interface
- [ ] Event listeners annotated with `@KafkaListener`
- [ ] Event correlation IDs tracked in event metadata
- [ ] Event publishers inject EventMetadata (correlation ID, user ID)
- [ ] Event listeners extract correlation ID and set in CorrelationContext
- [ ] Idempotency checks before processing events
- [ ] Error handling with dead letter queue
- [ ] Cache invalidation listeners implemented
- [ ] Kafka configuration imports `KafkaConfig`
- [ ] All Kafka beans use `@Qualifier("kafkaObjectMapper")`
- [ ] **Lombok used for event listeners and publishers** (`@Slf4j`, `@RequiredArgsConstructor`)

#### Container Module

- [ ] Main application class annotated with `@SpringBootApplication`
- [ ] Configuration classes for database, Kafka, security
- [ ] Health indicators for monitoring
- [ ] `WebMvcConfig` with `@Primary ObjectMapper` for REST API
- [ ] `KafkaConfig` imported via `@Import(KafkaConfig.class)`
- [ ] All Kafka beans use `@Qualifier("kafkaObjectMapper")`
- [ ] Metrics exposed via Actuator
- [ ] Multi-tenant configuration resolver
- [ ] **Lombok used for configuration classes** (`@Configuration` with `@RequiredArgsConstructor`)

---

**Document Version:** 1.1  
**Date:** 2025-01-22  
**Status:** Approved

**Version History:**

- v1.1 (2025-01-22): Added comprehensive Lombok usage policy and guidelines for all modules
- v1.0 (2025-12-22): Initial creation based on warehouse management system templates

**Review Cycle:** This document will be reviewed quarterly or when patterns change

**Distribution:** All development team members

**Related Documents:**

- [Mandated Implementation Template Guide](../guide/mandated-Implementation-template-guide.md)
- [Service Architecture Document](Service_Architecture_Document.md)
- [Production-Grade Caching Strategy](../caching/README.md)
- [ObjectMapper Separation Strategy](../05-development/ObjectMapper_Separation_Strategy.md)

