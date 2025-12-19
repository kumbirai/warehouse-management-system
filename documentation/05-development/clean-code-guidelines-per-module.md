# Clean Code Guidelines Per Module

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-11  
**Status:** Draft  
**Related Documents:**

- [Service Architecture Document](../architecture/Service_Architecture_Document.md)
- [Domain Model Design](../architecture/Domain_Model_Design.md)
- [Mandated Implementation Template Guide](../../guide/mandated-Implementation-template-guide.md)
- [Project Roadmap](../project-management/project-roadmap.md)

---

## Table of Contents

1. [Overview](#overview)
2. [General Principles](#general-principles)
3. [Domain Core Module Guidelines](#domain-core-module-guidelines)
4. [Application Service Module Guidelines](#application-service-module-guidelines)
5. [Application Layer Module Guidelines](#application-layer-module-guidelines)
6. [Data Access Module Guidelines](#data-access-module-guidelines)
7. [Messaging Module Guidelines](#messaging-module-guidelines)
8. [Container Module Guidelines](#container-module-guidelines)
9. [Frontend Module Guidelines](#frontend-module-guidelines)
10. [Common Module Guidelines](#common-module-guidelines)

---

## Overview

### Purpose

This document defines coding standards and best practices for each module in the Warehouse Management System Integration. These guidelines ensure consistency, maintainability, and
adherence to Domain-Driven Design, Clean Hexagonal Architecture, CQRS, and Event-Driven Design principles.

### Scope

These guidelines apply to all code written for the Warehouse Management System Integration, including:

- Backend services (Java/Spring Boot)
- Frontend application (React/TypeScript)
- Common modules
- Infrastructure components

### Key Principles

1. **Clean Code** - Code should be readable, maintainable, and self-documenting
2. **SOLID Principles** - Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion
3. **Domain-Driven Design** - Business logic drives architecture decisions
4. **Clean Hexagonal Architecture** - Clear separation of concerns with domain at center
5. **CQRS** - Separate command and query operations
6. **Event-Driven Design** - Loose coupling through domain events

---

## General Principles

### Code Organization

- **Package Structure** - Follow standard package structure per module
- **Naming Conventions** - Use clear, descriptive names that reflect business domain
- **File Organization** - One class per file, file name matches class name
- **Package Naming** - Use reverse domain notation: `com.ccbsa.wms.{service}.{module}.{layer}`

### Naming Conventions

#### Classes

- **Entities**: `{DomainObject}` (e.g., `StockConsignment`, `Location`)
- **Value Objects**: `{Attribute}` (e.g., `ConsignmentId`, `Quantity`, `ExpirationDate`)
- **Events**: `{DomainObject}{Action}Event` (e.g., `StockConsignmentReceivedEvent`)
- **Repositories**: `{DomainObject}Repository` (interface), `{DomainObject}RepositoryAdapter` (implementation)
- **Services**: `{DomainObject}Service` (domain service), `{DomainObject}ApplicationService` (application service)
- **Controllers**: `{DomainObject}CommandController`, `{DomainObject}QueryController`
- **DTOs**: `{DomainObject}DTO`, `Create{DomainObject}Command`, `{DomainObject}QueryResult`

#### Methods

- **Commands**: `create{Object}`, `update{Object}`, `delete{Object}`, `{action}{Object}` (e.g., `confirmConsignment`)
- **Queries**: `get{Object}`, `find{Object}`, `list{Object}s`, `query{Object}s`
- **Domain Logic**: Use business language (e.g., `assignLocation`, `checkExpiration`, `generateRestockRequest`)

#### Variables

- **camelCase** for variables and methods
- **PascalCase** for classes and interfaces
- **UPPER_SNAKE_CASE** for constants
- **Descriptive names** - Avoid abbreviations, use full words

### Code Formatting

- **Indentation**: 4 spaces (no tabs)
- **Line Length**: Maximum 120 characters
- **Braces**: Opening brace on same line, closing brace on new line
- **Blank Lines**: One blank line between methods, two between classes
- **Imports**: Organize imports (static imports last), remove unused imports

### String Formatting

- **String.format** - Always use `String.format()` for string concatenation instead of the `+` operator
- **Benefits**: Better readability, performance, and maintainability
- **Examples**:
    - ✅ `String.format("User ID is not a valid UUID: %s", value)`
    - ✅ `String.format("%s{eventId=%s, aggregateId=%s}", className, eventId, aggregateId)`
    - ❌ `"User ID is not a valid UUID: " + value`
    - ❌ `className + "{eventId=" + eventId + ", aggregateId=" + aggregateId + "}"`

### Imports and Fully Qualified Class Names

- **No FQCNs** - Fully Qualified Class Names (FQCNs) are not allowed. Always use imports instead
- **Benefits**: Better readability, easier refactoring, consistent code style
- **Examples**:
    - ✅ `import java.util.Map;` then use `Map<String, Object>`
    - ✅ `import java.time.Instant;` then use `Instant.now()`
    - ✅ `import java.net.InetSocketAddress;` then use `InetSocketAddress`
    - ❌ `java.util.Map<String, Object>`
    - ❌ `java.time.Instant.now()`
    - ❌ `java.net.InetSocketAddress remoteAddr`
- **Exception**: Only use FQCNs when necessary to resolve ambiguity between classes with the same simple name from different packages

### Documentation

- **JavaDoc** - All public classes, methods, and fields must have JavaDoc
- **Comments** - Use comments to explain "why", not "what"
- **README** - Each module should have a README explaining its purpose

### Testing

- **Unit Tests** - Test all business logic, aim for 80%+ coverage
- **Integration Tests** - Test service integration points
- **Test Naming** - `{methodName}_{scenario}_{expectedResult}` (e.g., `confirmConsignment_whenValidConsignment_shouldPublishEvent`)

---

## Domain Core Module Guidelines

### Module: `{service}-domain/{service}-domain-core`

**Purpose:** Pure Java domain entities with no external dependencies.

### Key Principles

1. **Pure Java** - No framework dependencies (Spring, JPA, etc.)
2. **No Lombok** - Implement builders, getters, setters manually
3. **Rich Domain Model** - Business logic encapsulated in entities
4. **Value Objects** - Use value objects for complex types
5. **Domain Events** - Publish events for state changes
6. **Immutable Value Objects** - Value objects must be immutable

### Package Structure

```
com.ccbsa.wms.{service}.domain.core
├── entity/                          # Domain entities (aggregate roots)
│   └── {DomainObject}.java
├── valueobject/                     # Value objects
│   ├── {DomainObject}Id.java
│   └── {Attribute}.java
├── event/                          # Domain events
│   ├── {Service}Event.java        # Base service event
│   └── {DomainObject}{Action}Event.java
├── exception/                      # Domain exceptions
│   └── {DomainObject}DomainException.java
└── specification/                  # Domain specifications
    └── {DomainObject}Specification.java
```

### Entity Implementation Rules

#### 1. Builder Pattern

**MANDATORY:** All entities must use public static Builder pattern:

```java
public class StockConsignment extends AggregateRoot<ConsignmentId> {
    
    private ConsignmentReference consignmentReference;
    private TenantId tenantId;
    // ... other fields
    
    private StockConsignment() {
        // Private constructor for builder
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private StockConsignment consignment = new StockConsignment();
        
        public Builder consignmentId(ConsignmentId id) {
            consignment.id = id;
            return this;
        }
        
        public Builder consignmentReference(ConsignmentReference reference) {
            consignment.consignmentReference = reference;
            return this;
        }
        
        // ... other builder methods
        
        public StockConsignment build() {
            validate();
            return consignment;
        }
        
        private void validate() {
            if (consignment.id == null) {
                throw new IllegalArgumentException("ConsignmentId is required");
            }
            // ... other validations
        }
    }
}
```

#### 2. Value Object Implementation

**MANDATORY:** Value objects must be immutable:

```java
public final class ConsignmentId {
    private final UUID value;
    
    private ConsignmentId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("ConsignmentId value cannot be null");
        }
        this.value = value;
    }
    
    public static ConsignmentId of(UUID value) {
        return new ConsignmentId(value);
    }
    
    public static ConsignmentId generate() {
        return new ConsignmentId(UUID.randomUUID());
    }
    
    public UUID getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsignmentId that = (ConsignmentId) o;
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

#### 3. Domain Event Implementation

**MANDATORY:** All domain events must extend `{Service}Event`:

```java
public class StockConsignmentReceivedEvent extends StockManagementEvent<StockConsignment> {
    
    private final ConsignmentReference consignmentReference;
    private final WarehouseId warehouseId;
    private final List<ConsignmentLineItem> lineItems;
    
    public StockConsignmentReceivedEvent(
            ConsignmentId aggregateId,
            ConsignmentReference consignmentReference,
            TenantId tenantId,
            WarehouseId warehouseId,
            List<ConsignmentLineItem> lineItems
    ) {
        super(aggregateId, tenantId);
        this.consignmentReference = consignmentReference;
        this.warehouseId = warehouseId;
        this.lineItems = new ArrayList<>(lineItems);
    }
    
    // Getters
    public ConsignmentReference getConsignmentReference() {
        return consignmentReference;
    }
    
    // ... other getters
}
```

#### 4. Business Logic Encapsulation

**MANDATORY:** Business logic must be in domain entities:

```java
public class StockConsignment extends AggregateRoot<ConsignmentId> {
    
    public void confirm() {
        if (this.status != ConsignmentStatus.RECEIVED) {
            throw new IllegalStateException("Can only confirm received consignments");
        }
        this.status = ConsignmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        
        addDomainEvent(new StockConsignmentConfirmedEvent(
            this.id,
            this.consignmentReference,
            this.tenantId,
            this.warehouseId,
            this.lineItems
        ));
    }
}
```

### Prohibited in Domain Core

- ❌ Lombok annotations (`@Getter`, `@Setter`, `@Builder`, etc.)
- ❌ Spring annotations (`@Component`, `@Service`, `@Autowired`, etc.)
- ❌ JPA annotations (`@Entity`, `@Table`, `@Column`, etc.)
- ❌ Validation annotations (`@NotNull`, `@Valid`, etc.)
- ❌ Framework dependencies (Spring, Hibernate, etc.)
- ❌ Infrastructure concerns (database, messaging, etc.)

### Allowed in Domain Core

- ✅ Pure Java (java.lang, java.util, java.time)
- ✅ Common domain base classes (`AggregateRoot`, `DomainEvent`)
- ✅ Business logic and validation
- ✅ Domain events
- ✅ Value objects
- ✅ Domain exceptions

---

## Application Service Module Guidelines

### Module: `{service}-domain/{service}-application-service`

**Purpose:** Orchestrate use cases and define port interfaces.

### Key Principles

1. **Use Case Handlers** - One handler per use case
2. **Port Interfaces** - Define repository and service interfaces
3. **CQRS Separation** - Separate command and query handlers
4. **Event Publishing** - Publish events after successful commits
5. **Transaction Management** - One transaction per use case

### Package Structure

```
com.ccbsa.wms.{service}.application.service
├── command/                         # Command handlers
│   ├── {Action}{DomainObject}CommandHandler.java
│   └── dto/                        # Command DTOs
│       └── {Action}{DomainObject}Command.java
├── query/                          # Query handlers
│   ├── Get{DomainObject}QueryHandler.java
│   └── dto/                       # Query DTOs
│       └── {DomainObject}QueryResult.java
├── port/                          # Port interfaces
│   ├── repository/               # Repository ports
│   │   └── {DomainObject}Repository.java
│   └── service/                  # Service ports
│       └── {DomainObject}ServicePort.java
└── exception/                    # Application exceptions
    └── {DomainObject}ApplicationException.java
```

### Command Handler Implementation

```java
@Component
public class ConfirmConsignmentCommandHandler {
    
    private final StockConsignmentRepository repository;
    private final EventPublisher eventPublisher;
    
    @Transactional
    public ConfirmConsignmentResult handle(ConfirmConsignmentCommand command) {
        // 1. Load aggregate
        StockConsignment consignment = repository.findById(command.getConsignmentId())
            .orElseThrow(() -> new ConsignmentNotFoundException(command.getConsignmentId()));
        
        // 2. Execute business logic
        consignment.confirm();
        
        // 3. Persist
        repository.save(consignment);
        
        // 4. Publish events
        eventPublisher.publish(consignment.getDomainEvents());
        consignment.clearDomainEvents();
        
        // 5. Return result
        return ConfirmConsignmentResult.builder()
            .consignmentId(consignment.getId())
            .status(consignment.getStatus())
            .confirmedAt(consignment.getConfirmedAt())
            .build();
    }
}
```

### Query Handler Implementation

```java
@Component
public class GetStockConsignmentQueryHandler {
    
    private final StockConsignmentRepository repository;
    
    @Transactional(readOnly = true)
    public StockConsignmentQueryResult handle(GetStockConsignmentQuery query) {
        StockConsignment consignment = repository.findById(query.getConsignmentId())
            .orElseThrow(() -> new ConsignmentNotFoundException(query.getConsignmentId()));
        
        return StockConsignmentQueryResult.builder()
            .id(consignment.getId())
            .consignmentReference(consignment.getConsignmentReference())
            .status(consignment.getStatus())
            .lineItems(mapLineItems(consignment.getLineItems()))
            .build();
    }
}
```

### Repository Port Interface

```java
public interface StockConsignmentRepository {
    void save(StockConsignment consignment);
    Optional<StockConsignment> findById(ConsignmentId id);
    Optional<StockConsignment> findByConsignmentReference(ConsignmentReference reference);
    List<StockConsignment> findByStatus(ConsignmentStatus status);
    void delete(ConsignmentId id);
}
```

---

## Application Layer Module Guidelines

### Module: `{service}-application`

**Purpose:** REST API endpoints with CQRS compliance.

### Key Principles

1. **CQRS Controllers** - Separate command and query controllers
2. **DTO Mapping** - Map between DTOs and domain objects
3. **Input Validation** - Validate all inputs
4. **Error Handling** - Consistent error responses
5. **OpenAPI Documentation** - Document all endpoints

### Package Structure

```
com.ccbsa.wms.{service}.application
├── command/                       # Command controllers
│   └── {DomainObject}CommandController.java
├── query/                        # Query controllers
│   └── {DomainObject}QueryController.java
├── dto/                         # DTOs
│   ├── command/                # Command DTOs
│   │   └── {Action}{DomainObject}CommandDTO.java
│   ├── query/                  # Query DTOs
│   │   └── {DomainObject}QueryResultDTO.java
│   └── mapper/                 # DTO mappers
│       └── {DomainObject}DTOMapper.java
└── exception/                   # Exception handlers
    └── GlobalExceptionHandler.java
```

### Command Controller Implementation

```java
@RestController
@RequestMapping("/api/v1/stock-management/consignments")
@Tag(name = "Stock Consignment Commands", description = "Stock consignment command operations")
public class StockConsignmentCommandController {
    
    private final ConfirmConsignmentCommandHandler commandHandler;
    private final StockConsignmentDTOMapper mapper;
    
    @PostMapping("/{consignmentId}/confirm")
    @Operation(summary = "Confirm consignment receipt")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ConfirmConsignmentResultDTO> confirmConsignment(
            @PathVariable String consignmentId,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        ConfirmConsignmentCommand command = ConfirmConsignmentCommand.builder()
            .consignmentId(ConsignmentId.of(UUID.fromString(consignmentId)))
            .tenantId(TenantId.of(tenantId))
            .build();
        
        ConfirmConsignmentResult result = commandHandler.handle(command);
        
        return ResponseEntity.ok(mapper.toDTO(result));
    }
}
```

### Query Controller Implementation

```java
@RestController
@RequestMapping("/api/v1/stock-management/consignments")
@Tag(name = "Stock Consignment Queries", description = "Stock consignment query operations")
public class StockConsignmentQueryController {
    
    private final GetStockConsignmentQueryHandler queryHandler;
    private final StockConsignmentDTOMapper mapper;
    
    @GetMapping("/{consignmentId}")
    @Operation(summary = "Get consignment by ID")
    @PreAuthorize("hasRole('VIEWER')")
    public ResponseEntity<StockConsignmentQueryResultDTO> getConsignment(
            @PathVariable String consignmentId,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        GetStockConsignmentQuery query = GetStockConsignmentQuery.builder()
            .consignmentId(ConsignmentId.of(UUID.fromString(consignmentId)))
            .tenantId(TenantId.of(tenantId))
            .build();
        
        StockConsignmentQueryResult result = queryHandler.handle(query);
        
        return ResponseEntity.ok(mapper.toDTO(result));
    }
}
```

---

## Data Access Module Guidelines

### Module: `{service}-dataaccess`

**Purpose:** Implement repository adapters with JPA.

### Key Principles

1. **Adapter Pattern** - Implement repository port interfaces
2. **JPA Entities** - Separate JPA entities from domain entities
3. **Mapping** - Map between JPA and domain entities
4. **Caching** - Use decorator pattern for caching
5. **Multi-Tenant** - Support tenant isolation

### Package Structure

```
com.ccbsa.wms.{service}.dataaccess
├── adapter/                      # Repository adapters
│   └── {DomainObject}RepositoryAdapter.java
├── entity/                      # JPA entities
│   └── {DomainObject}Entity.java
├── mapper/                     # Entity mappers
│   └── {DomainObject}EntityMapper.java
└── cache/                      # Cache decorators
    └── Cached{DomainObject}Repository.java
```

### Repository Adapter Implementation

**CRITICAL: Version Field Handling with Optimistic Locking**

When using JPA `@Version` annotation, always check if entity exists before saving to handle version correctly:

```java
@Repository
public class StockConsignmentRepositoryAdapter implements StockConsignmentRepository {
    
    private final StockConsignmentJpaRepository jpaRepository;
    private final StockConsignmentEntityMapper mapper;
    
    @Override
    public void save(StockConsignment consignment) {
        // Check if entity already exists to handle version correctly for optimistic locking
        Optional<StockConsignmentEntity> existingEntity = 
            jpaRepository.findByTenantIdAndId(
                consignment.getTenantId().getValue(),
                consignment.getId().getValue()
            );

        StockConsignmentEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            updateEntityFromDomain(entity, consignment);
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity(consignment);
        }

        jpaRepository.save(entity);
    }
    
    /**
     * Updates an existing entity with values from the domain model.
     * Preserves JPA managed state and version for optimistic locking.
     */
    private void updateEntityFromDomain(StockConsignmentEntity entity, StockConsignment consignment) {
        // Update all mutable fields from domain model
        entity.setStatus(consignment.getStatus());
        entity.setLastModifiedAt(consignment.getLastModifiedAt());
        // ... update other mutable fields
        
        // Version is managed by JPA - don't update it manually
        // Hibernate will automatically increment version on update
    }
    
    @Override
    public Optional<StockConsignment> findById(ConsignmentId id) {
        return jpaRepository.findById(id.getValue())
            .map(mapper::toDomain);
    }
    
    @Override
    public Optional<StockConsignment> findByConsignmentReference(ConsignmentReference reference) {
        return jpaRepository.findByConsignmentReference(reference.getValue())
            .map(mapper::toDomain);
    }
}
```

**Entity Mapper Version Handling:**

```java
@Component
public class StockConsignmentEntityMapper {
    
    public StockConsignmentEntity toEntity(StockConsignment consignment) {
        StockConsignmentEntity entity = new StockConsignmentEntity();
        entity.setId(consignment.getId().getValue());
        entity.setTenantId(consignment.getTenantId().getValue());
        entity.setStatus(consignment.getStatus());
        entity.setCreatedAt(consignment.getCreatedAt());
        entity.setLastModifiedAt(consignment.getLastModifiedAt());
        
        // For new entities, version will be set by Hibernate when persisting
        // For existing entities loaded from DB, version is already set
        // We only set version when mapping from domain if it's > 0 (existing entity)
        int domainVersion = consignment.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }
        // For new entities (version == 0), don't set version - let Hibernate manage it
        
        return entity;
    }
    
    public StockConsignment toDomain(StockConsignmentEntity entity) {
        return StockConsignment.builder()
            .consignmentId(ConsignmentId.of(entity.getId()))
            .tenantId(TenantId.of(entity.getTenantId()))
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .lastModifiedAt(entity.getLastModifiedAt())
            .version(entity.getVersion() != null ? entity.getVersion().intValue() : 0)
            .buildWithoutEvents();
    }
}
```

**Key Rules:**

- ✅ Always check entity existence in repository adapter before saving
- ✅ For new entities (version == 0): Don't set version field in mapper
- ✅ For existing entities (version > 0): Set version for optimistic locking
- ✅ When updating existing entities: Preserve JPA managed state, don't manually update version
- ❌ Never set `version = 0` for new entities (causes optimistic locking errors)

### JPA Entity Implementation

**Important:** Do not use SpEL expressions in `@Table` annotations. Hibernate does not evaluate them. Use the placeholder schema `"tenant_schema"` instead, which will be
dynamically resolved by `TenantAwarePhysicalNamingStrategy`.

```java
@Entity
@Table(name = "stock_consignments", schema = "tenant_schema")
public class StockConsignmentEntity {
    // The schema "tenant_schema" is a placeholder that will be dynamically 
    // replaced with the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy
    
    @Id
    private UUID id;
    
    @Column(name = "consignment_reference", nullable = false, unique = true)
    private String consignmentReference;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConsignmentStatus status;
    
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
    
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    @OneToMany(mappedBy = "consignment", cascade = CascadeType.ALL)
    private List<ConsignmentLineItemEntity> lineItems;
    
    // Getters and setters
}
```

---

## Messaging Module Guidelines

### Module: `{service}-messaging`

**Purpose:** Handle event-driven choreography.

### Key Principles

1. **Event Publisher** - Implement event publisher port
2. **Event Listener** - Listen to external events
3. **Idempotency** - Handle duplicate events
4. **Correlation** - Track event correlation and causation
5. **Error Handling** - Handle event processing errors

### Package Structure

```
com.ccbsa.wms.{service}.messaging
├── publisher/                    # Event publishers
│   └── {Service}EventPublisher.java
├── listener/                    # Event listeners
│   └── {ExternalEvent}Listener.java
└── config/                     # Kafka configuration (optional, uses common-messaging)
    └── KafkaConfig.java
```

**Note:** Services should use the standardized Kafka configuration from `common-messaging` module. Service-specific Kafka configuration is only needed for custom overrides.

### Event Publisher Implementation

```java
@Component
public class StockManagementEventPublisher implements EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(event -> {
            String topic = determineTopic(event);
            kafkaTemplate.send(topic, event.getAggregateId().toString(), event);
        });
    }
    
    private String determineTopic(DomainEvent<?> event) {
        return "stock-management-events";
    }
}
```

### Event Listener Implementation

```java
@Component
public class LocationAssignedEventListener {
    
    private final StockConsignmentRepository repository;
    
    @KafkaListener(
        topics = "location-management-events",
        groupId = "stock-management-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(LocationAssignedEvent event, Acknowledgment acknowledgment) {
        try {
            // Handle event
            StockConsignment consignment = repository.findById(event.getStockConsignmentId())
                .orElseThrow();
            
            consignment.updateLocation(event.getLocationId());
            repository.save(consignment);
            
            // Acknowledge message after successful processing
            acknowledgment.acknowledge();
        } catch (Exception e) {
            // Don't acknowledge - will retry or go to DLQ
            throw e;
        }
    }
}
```

### Kafka Configuration Standards

All services use standardized production-grade Kafka configuration from `common-messaging` module:

**Application Configuration (`application.yml`):**

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    
    # Producer Configuration (Production-Grade)
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      enable-idempotence: true
      compression-type: snappy
      batch-size: 16384
      linger-ms: 10
      buffer-memory: 33554432
      max-in-flight-requests-per-connection: 5
      properties:
        spring.json.add.type.headers: false
    
    # Consumer Configuration (Production-Grade)
    consumer:
      group-id: ${spring.application.name}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      max-poll-records: 500
      max-poll-interval-ms: 300000
      session-timeout-ms: 30000
      heartbeat-interval-ms: 3000
      properties:
        spring.json.trusted.packages: "*"
    
    # Listener Configuration
    listener:
      ack-mode: manual_immediate
      type: batch
      concurrency: 3
      poll-timeout: 3000
    
    # Error Handling Configuration
    error-handling:
      max-retries: 3
      initial-interval: 1000
      multiplier: 2.0
      max-interval: 10000
      dead-letter-topic-suffix: .dlq
```

**Key Configuration Principles:**

1. **Idempotent Producers:** All producers use `enable-idempotence: true` to prevent duplicate messages
2. **Manual Acknowledgment:** Consumers use `enable-auto-commit: false` and `ack-mode: manual_immediate` for reliable processing
3. **Dead Letter Queue:** Failed messages after max retries are published to `{topic}.dlq`
4. **Error Handling:** Exponential backoff retry mechanism with configurable intervals
5. **Health Monitoring:** Kafka health indicator available via Actuator `/actuator/health`
6. **Type Safety:** JSON serialization/deserialization with Jackson ObjectMapper and type mappings

---

## Container Module Guidelines

### Module: `{service}-container`

**Purpose:** Bootstrap application and manage configuration.

### Key Principles

1. **Configuration** - Externalize all configuration
2. **Dependency Injection** - Use Spring dependency injection
3. **Health Checks** - Implement health endpoints
4. **Metrics** - Expose metrics for monitoring
5. **Security** - Configure security

### Package Structure

```
com.ccbsa.wms.{service}.container
├── config/                      # Configuration classes
│   ├── DatabaseConfig.java
│   ├── KafkaConfig.java
│   └── SecurityConfig.java
├── health/                     # Health checks
│   └── {Service}HealthIndicator.java
└── {Service}Application.java  # Main application class
```

### Application Bootstrap

```java
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.ccbsa.wms.stocks")
@EntityScan(basePackages = "com.ccbsa.wms.stocks.entity")
public class StockManagementApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(StockManagementApplication.class, args);
    }
}
```

---

## Frontend Module Guidelines

### Module: `frontend-app`

**Purpose:** User interface with CQRS compliance.

### Key Principles

1. **Component-Based** - Reusable React components
2. **CQRS Separation** - Separate command and query operations
3. **State Management** - Use Redux Toolkit for state
4. **Type Safety** - Use TypeScript for type safety
5. **Offline Support** - Support offline operations

### File Structure

```
src/
├── components/           # Reusable components
├── features/            # Feature modules
├── hooks/              # Custom hooks
├── services/           # API services
├── store/             # Redux store
└── types/             # TypeScript types
```

### Component Implementation

```typescript
interface StockConsignmentFormProps {
  onSubmit: (data: CreateConsignmentCommandDTO) => void;
  isLoading: boolean;
}

export const StockConsignmentForm: React.FC<StockConsignmentFormProps> = ({
  onSubmit,
  isLoading
}) => {
  const { register, handleSubmit, formState: { errors } } = useForm<CreateConsignmentCommandDTO>();
  
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      {/* Form fields */}
    </form>
  );
};
```

---

## Common Module Guidelines

### Module: `common-domain`, `common-messaging`

**Purpose:** Shared domain and messaging infrastructure.

### Key Principles

1. **Base Classes** - Provide base classes for aggregates and events
2. **Value Objects** - Shared value objects (TenantId, UserId, etc.)
3. **Event Infrastructure** - Event publisher/consumer interfaces
4. **No Business Logic** - Only infrastructure, no business logic

### Common Domain Base Classes

```java
public abstract class AggregateRoot<ID> {
    protected ID id;
    private List<DomainEvent<?>> domainEvents = new ArrayList<>();
    private int version = 0;
    
    protected void addDomainEvent(DomainEvent<?> event) {
        domainEvents.add(event);
    }
    
    public List<DomainEvent<?>> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }
    
    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
```

---

## Summary

### Module-Specific Rules Summary

| Module                  | Key Rules                                                      |
|-------------------------|----------------------------------------------------------------|
| **Domain Core**         | Pure Java, no dependencies, manual builders, rich domain model |
| **Application Service** | Use case handlers, port interfaces, CQRS separation            |
| **Application Layer**   | CQRS controllers, DTO mapping, input validation                |
| **Data Access**         | Adapter pattern, JPA entities, entity mapping                  |
| **Messaging**           | Event publisher/listener, idempotency, correlation             |
| **Container**           | Configuration, dependency injection, health checks             |
| **Frontend**            | Component-based, CQRS separation, TypeScript                   |
| **Common**              | Base classes, shared infrastructure, no business logic         |

---

**Document Control**

- **Version History:** This document will be version controlled with change tracking
- **Review Cycle:** This document will be reviewed monthly or when patterns change
- **Distribution:** This document will be distributed to all development team members

