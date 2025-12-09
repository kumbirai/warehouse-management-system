# Mandated Implementation Template Guide

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 2.0  
**Date:** 2025-01  
**Status:** Approved  
**Related Documents:**

- [Service Architecture Document](../01-project-planning/architecture/Service_Architecture_Document.md)
- [Domain Model Design](../01-project-planning/architecture/Domain_Model_Design.md)
- [Clean Code Guidelines](../01-project-planning/development/clean-code-guidelines-per-module.md)
- [Project Roadmap](../01-project-planning/project-management/project-roadmap.md)

---

## Overview

This document provides the overarching guide for implementing production-grade microservices following **Domain-Driven Design**, **Clean Hexagonal Architecture**, **CQRS**, and *
*Event-Driven Choreography** principles. All templates have been systematically designed to ensure strict adherence to these principles.

## Template Document Structure

This guide references comprehensive implementation templates for each layer of the microservices architecture:

### Domain Core (Pure Java, No Dependencies) (`{service}-domain/{service}-domain-core`)

**Template Document**: `@01-mandated-domain-core-templates.md`

The domain core contains pure business logic with no external dependencies. This layer implements:

- Rich domain entities with business logic encapsulation
- Value objects for complex data types
- Domain events for state changes
- Business rule validation and invariants
- Manual Builder pattern (NO Lombok annotations)

### Application Service (`{service}-domain/{service}-application-service`)

**Template Document**: `@02-mandated-application-service-templates.md`

The application service layer orchestrates use cases and defines port interfaces. This layer implements:

- Use case handlers following CQRS principles
- Repository port interfaces (NOT in domain core)
- Command and query separation
- Event publishing after successful commits
- Centralized validation and error handling

### Application Layer (`{service}-application`)

**Template Document**: `@03-mandated-application-layer-templates.md`

The application layer provides REST API endpoints with CQRS compliance. This layer implements:

- Command and query controllers with proper separation
- Anti-corruption layer with DTO mappers
- Input validation and error handling
- Security annotations and tenant context
- OpenAPI documentation

### Data Access (`{service}-dataaccess`)

**Template Document**: `@04-mandated-data-access-templates.md`

The data access layer implements repository adapters with caching. This layer implements:

- JPA entities (infrastructure layer only)
- Repository adapters implementing application service interfaces
- Mappers between JPA and domain entities
- Decorator pattern for Redis caching
- Multi-tenant data isolation

### Messaging (`{service}-messaging`)

**Template Document**: `@05-mandated-messaging-templates.md`

The messaging layer handles event-driven choreography. This layer implements:

- Event publishers implementing port interfaces
- Event listeners for external events
- Event correlation and causation tracking
- Idempotency support and compensation events
- Kafka integration for event choreography

### Container (`{service}-container`)

**Template Document**: `@06-mandated-container-templates.md`

The container layer bootstraps the application and manages configuration. This layer implements:

- Application bootstrap with comprehensive configuration
- Dependency injection with decorator pattern
- Health monitoring and metrics
- Gateway-centric security model
- Multi-tenant configuration management

### Front End (`frontend-app`)

**Template Document**: `@07-mandated-frontend-templates.md`

The frontend application provides the user interface with CQRS compliance. This layer implements:

- CQRS-compliant API integration
- Real-time event streaming via WebSocket
- Optimistic UI updates
- Event correlation tracking
- Comprehensive error handling and user feedback

## Critical Architectural Principles

### 1. Repository Interface Placement

- **Repository interfaces** defined in application service layer (`{service}-application-service`)
- **Dependency Direction**: Application Service → Data Access (proper hexagonal architecture)
- **Domain Purity**: Domain core remains completely pure with no infrastructure dependencies

### 2. Pure Domain Implementation

- **NO** Lombok annotations in domain core - implement manually
- **NO** Spring Boot dependencies in domain core
- **NO** JPA annotations in domain core
- **NO** external dependencies except common domain base classes
- **Rich Domain Model**: Proper business logic encapsulation within entities

### 3. Proper CQRS Implementation

- **Command Operations**: Return command-specific results, not domain data
- **Query Operations**: Return optimized query results for read operations
- **Separate Models**: Command and query models are distinct and optimized
- **Event Publishing**: Commands publish events for query model updates
- **Transaction Boundaries**: Keep command and query transactions separate

### 4. Event-Driven Choreography

- **No SAGA Pattern**: Use event-driven choreography instead
- **Loose Coupling**: Services communicate through events only
- **Event Sourcing**: Critical aggregates maintain event logs
- **Idempotency**: All event handlers must be idempotent
- **Event Ordering**: Events within aggregate processed in order

### 5. Service Event Pattern

- **Service-Specific Events**: Each service has `{Service}Event<T>` base class
- **Event Hierarchy**: `{Service}Event` extends `DomainEvent<T>`
- **Event Naming**: `{DomainObject}{Action}Event` (e.g., `StockConsignmentReceivedEvent`)
- **Event Immutability**: Events are immutable value objects
- **Event Versioning**: Events support schema evolution

### 6. Traceability and Correlation ID Flow

- **Frontend**: Generates correlation ID per user session, sends in `X-Correlation-Id` header
- **Gateway**: Extracts or generates correlation ID, propagates to downstream services
- **Services**: Extract correlation ID from headers, set in `CorrelationContext` (ThreadLocal)
- **Event Publishers**: Extract correlation ID from `CorrelationContext`, inject into `EventMetadata`
- **Event Listeners**: Extract correlation ID from consumed event metadata, set in `CorrelationContext`
- **End-to-End Tracing**: Correlation ID flows from frontend → gateway → services → events → event consumers
- **Logging**: Correlation ID included in all logs (frontend MDC, backend MDC) for log correlation

## Implementation Checklist

### Domain Core Module

- [ ] Entity extends `AggregateRoot<ID>` or `TenantAwareAggregateRoot<ID>`
- [ ] Entity uses public static `Builder builder()` pattern
- [ ] Builder validates all required fields in `build()` method
- [ ] Value objects are immutable with `final` fields
- [ ] Value objects implement `equals()`, `hashCode()`, `toString()`
- [ ] Domain events extend `{Service}Event<T>`
- [ ] Domain events support optional `EventMetadata` for traceability
- [ ] Business logic encapsulated in entity methods
- [ ] No Lombok annotations
- [ ] No Spring/JPA annotations
- [ ] No external dependencies

### Application Service Module

- [ ] Command handlers annotated with `@Component` and `@Transactional`
- [ ] Query handlers annotated with `@Component` and `@Transactional(readOnly = true)`
- [ ] Repository interfaces defined in `port.repository` package
- [ ] Service port interfaces defined in `port.service` package
- [ ] Events published after successful commit
- [ ] Domain events cleared after publishing
- [ ] Correlation ID available via `CorrelationContext` for event publishing
- [ ] Command results return DTOs, not domain entities
- [ ] Query results return optimized DTOs

### Application Layer Module

- [ ] Command controllers separate from query controllers
- [ ] Controllers annotated with `@RestController` and `@Tag` for OpenAPI
- [ ] Endpoints annotated with `@Operation` for documentation
- [ ] Security annotations (`@PreAuthorize`) on endpoints
- [ ] Tenant context extracted from headers (`@RequestHeader("X-Tenant-Id")`)
- [ ] DTOs used for all API communication
- [ ] DTO mappers convert between DTOs and domain objects
- [ ] Global exception handler for consistent error responses

### Data Access Module

- [ ] Repository adapters implement application service repository interfaces
- [ ] JPA entities separate from domain entities
- [ ] Entity mappers convert between JPA and domain entities
- [ ] Multi-tenant schema resolution using `@Table(schema = "tenant_schema")` with `TenantAwarePhysicalNamingStrategy` configured in `application.yml`
- [ ] Cache decorators use decorator pattern
- [ ] Repository methods handle tenant isolation

### Messaging Module

- [ ] Event publisher implements `EventPublisher` interface from common-messaging
- [ ] Event listeners annotated with `@KafkaListener`
- [ ] Event correlation IDs tracked in event metadata
- [ ] Event publishers inject EventMetadata (correlation ID, causation ID, user ID) before publishing
- [ ] Event listeners extract correlation ID from consumed events and set in CorrelationContext
- [ ] Idempotency checks before processing events
- [ ] Error handling with dead letter queue
- [ ] Event versioning support

### Container Module

- [ ] Main application class annotated with `@SpringBootApplication`
- [ ] Configuration classes for database, Kafka, security
- [ ] Health indicators for monitoring
- [ ] Metrics exposed via Actuator
- [ ] Multi-tenant configuration resolver
- [ ] Dependency injection configured properly

### Frontend Module

- [ ] Components follow container/presentational pattern
- [ ] API client separates command and query operations
- [ ] API client injects X-Correlation-Id header on all requests
- [ ] Correlation ID service manages session-level correlation IDs
- [ ] Logger includes correlation ID in all log entries
- [ ] Correlation ID cleared on logout
- [ ] Redux store organized by feature
- [ ] TypeScript types for all DTOs
- [ ] Offline support with IndexedDB
- [ ] Error handling and user feedback
- [ ] Accessibility compliance (WCAG 2.1 Level AA)

## Quick Reference Templates

**Note:** For comprehensive code templates, refer to the dedicated template files:

- **[Domain Core Templates](@01-mandated-domain-core-templates.md)** - Aggregate roots, value objects, domain events
- **[Application Service Templates](@02-mandated-application-service-templates.md)** - Command/query handlers, ports
- **[Application Layer Templates](@03-mandated-application-layer-templates.md)** - REST controllers, DTOs, mappers
- **[Data Access Templates](@04-mandated-data-access-templates.md)** - Repository adapters, JPA entities, mappers
- **[Messaging Templates](@05-mandated-messaging-templates.md)** - Event publishers, listeners, projections
- **[Container Templates](@06-mandated-container-templates.md)** - Application bootstrap, configuration
- **[Frontend Templates](@07-mandated-frontend-templates.md)** - API clients, React components

The template files above are the **single source of truth** for all code templates. This guide provides architectural principles and references to those templates.

## Naming Conventions

### Package Naming

**Standard Package Structure:**

```
com.ccbsa.wms.{service}.{module}.{layer}.{component}
```

#### Domain Core Packages

- **Entities**: `com.ccbsa.wms.{service}.domain.core.entity`
    - Example: `com.ccbsa.wms.stock.domain.core.entity`
    - Contains: Aggregate roots and domain entities

- **Value Objects**: `com.ccbsa.wms.{service}.domain.core.valueobject`
    - Example: `com.ccbsa.wms.stock.domain.core.valueobject`
    - Contains: Immutable value objects (IDs, quantities, dates, etc.)

- **Domain Events**: `com.ccbsa.wms.{service}.domain.core.event`
    - Example: `com.ccbsa.wms.stock.domain.core.event`
    - Contains: Domain events for aggregate state changes

- **Domain Exceptions**: `com.ccbsa.wms.{service}.domain.core.exception`
    - Example: `com.ccbsa.wms.stock.domain.core.exception`
    - Contains: Business rule violation exceptions

#### Application Service Packages

- **Command Handlers**: `com.ccbsa.wms.{service}.application.service.command`
    - Example: `com.ccbsa.wms.stock.application.service.command`
    - Contains: Command use case handlers

- **Query Handlers**: `com.ccbsa.wms.{service}.application.service.query`
    - Example: `com.ccbsa.wms.stock.application.service.query`
    - Contains: Query use case handlers

- **Repository Ports**: `com.ccbsa.wms.{service}.application.service.port.repository`
    - Example: `com.ccbsa.wms.{service}.application.service.port.repository`
    - Contains: Repository interface definitions

- **Service Ports**: `com.ccbsa.wms.{service}.application.service.port.service`
    - Example: `com.ccbsa.wms.stock.application.service.port.service`
    - Contains: External service interface definitions

- **Event Publisher Ports**: `com.ccbsa.wms.{service}.application.service.port.messaging`
    - Example: `com.ccbsa.wms.stock.application.service.port.messaging`
    - Contains: Event publisher interface definitions

#### Application Layer Packages

- **Command Controllers**: `com.ccbsa.wms.{service}.application.command`
    - Example: `com.ccbsa.wms.stock.application.command`
    - Contains: REST endpoints for commands (POST, PUT, DELETE)

- **Query Controllers**: `com.ccbsa.wms.{service}.application.query`
    - Example: `com.ccbsa.wms.stock.application.query`
    - Contains: REST endpoints for queries (GET)

- **Command DTOs**: `com.ccbsa.wms.{service}.application.dto.command`
    - Example: `com.ccbsa.wms.stock.application.dto.command`
    - Contains: Request/response DTOs for commands

- **Query DTOs**: `com.ccbsa.wms.{service}.application.dto.query`
    - Example: `com.ccbsa.wms.stock.application.dto.query`
    - Contains: Response DTOs for queries

- **Mappers**: `com.ccbsa.wms.{service}.application.mapper`
    - Example: `com.ccbsa.wms.stock.application.mapper`
    - Contains: DTO-to-domain mappers

- **Exception Handling**: `com.ccbsa.wms.{service}.application.exception`
    - Example: `com.ccbsa.wms.stock.application.exception`
    - Contains: Global exception handler

#### Data Access Packages

- **JPA Entities**: `com.ccbsa.wms.{service}.dataaccess.entity`
    - Example: `com.ccbsa.wms.stock.dataaccess.entity`
    - Contains: JPA annotated entities (infrastructure layer)

- **Repository Adapters**: `com.ccbsa.wms.{service}.dataaccess.adapter`
    - Example: `com.ccbsa.wms.stock.dataaccess.adapter`
    - Contains: Base repository implementations

- **Cached Adapters**: `com.ccbsa.wms.{service}.dataaccess.cache`
    - Example: `com.ccbsa.wms.stock.dataaccess.cache`
    - Contains: Redis caching decorators

- **JPA Repositories**: `com.ccbsa.wms.{service}.dataaccess.jpa`
    - Example: `com.ccbsa.wms.stock.dataaccess.jpa`
    - Contains: Spring Data JPA repository interfaces

- **Entity Mappers**: `com.ccbsa.wms.{service}.dataaccess.mapper`
    - Example: `com.ccbsa.wms.stock.dataaccess.mapper`
    - Contains: JPA-to-domain entity mappers

#### Messaging Packages

- **Event Publishers**: `com.ccbsa.wms.{service}.messaging.publisher`
    - Example: `com.ccbsa.wms.stock.messaging.publisher`
    - Contains: Kafka event publisher implementations

- **Event Listeners**: `com.ccbsa.wms.{service}.messaging.listener`
    - Example: `com.ccbsa.wms.stock.messaging.listener`
    - Contains: Kafka event consumer implementations

- **Message Mappers**: `com.ccbsa.wms.{service}.messaging.mapper`
    - Example: `com.ccbsa.wms.stock.messaging.mapper`
    - Contains: Event-to-domain mappers

#### Container Packages

- **Configuration**: `com.ccbsa.wms.{service}.container.config`
    - Example: `com.ccbsa.wms.stock.container.config`
    - Contains: Spring configuration classes

- **Health**: `com.ccbsa.wms.{service}.container.health`
    - Example: `com.ccbsa.wms.stock.container.health`
    - Contains: Custom health indicators

### Class Naming

#### Domain Core Classes

| Type                     | Pattern                              | Example                                                    | Notes                                                         |
|--------------------------|--------------------------------------|------------------------------------------------------------|---------------------------------------------------------------|
| **Aggregate Root**       | `{DomainObject}`                     | `StockConsignment`, `Location`, `Product`                  | Extends `AggregateRoot<ID>` or `TenantAwareAggregateRoot<ID>` |
| **Entity**               | `{DomainObject}`                     | `ConsignmentLine`, `LocationMovement`                      | Part of aggregate, not a root                                 |
| **Value Object (ID)**    | `{DomainObject}Id`                   | `ConsignmentId`, `LocationId`, `ProductId`                 | Immutable, wraps UUID or String                               |
| **Value Object (Other)** | `{Attribute}`                        | `Quantity`, `ExpirationDate`, `BatchNumber`                | Immutable, business concept                                   |
| **Domain Event**         | `{DomainObject}{Action}Event`        | `StockConsignmentReceivedEvent`, `LocationAssignedEvent`   | Extends `{Service}Event<T>`                                   |
| **Domain Exception**     | `{DomainObject}{Violation}Exception` | `ConsignmentNotFoundException`, `InvalidQuantityException` | Extends `DomainException`                                     |
| **Service Event Base**   | `{Service}Event`                     | `StockManagementEvent`, `LocationManagementEvent`          | Extends `DomainEvent<T>`                                      |

**Domain Core Naming Rules:**

- Use business language (ubiquitous language)
- Avoid technical terms (repository, service, manager, handler)
- Single Responsibility Principle - clear, focused names
- Value objects named after concept they represent
- Events named: `{What}{Happened}Event` (past tense)

#### Application Service Classes

| Type                     | Pattern                                                | Example                                              | Notes                                                          |
|--------------------------|--------------------------------------------------------|------------------------------------------------------|----------------------------------------------------------------|
| **Command**              | `{Action}{DomainObject}Command`                        | `ConfirmConsignmentCommand`, `CreateLocationCommand` | Implements `Command` marker interface                          |
| **Command Result**       | `{Action}{DomainObject}Result`                         | `ConfirmConsignmentResult`, `CreateLocationResult`   | Return type from command handlers                              |
| **Command Handler**      | `{Action}{DomainObject}CommandHandler`                 | `ConfirmConsignmentCommandHandler`                   | Annotated with `@Component`, `@Transactional`                  |
| **Query**                | `Get{DomainObject}Query` or `List{DomainObject}sQuery` | `GetConsignmentQuery`, `ListExpiringStockQuery`      | Implements `Query<T>` marker interface                         |
| **Query Result**         | `{DomainObject}QueryResult`                            | `ConsignmentQueryResult`, `StockLevelQueryResult`    | Return type from query handlers                                |
| **Query Handler**        | `Get{DomainObject}QueryHandler`                        | `GetConsignmentQueryHandler`                         | Annotated with `@Component`, `@Transactional(readOnly = true)` |
| **Repository Port**      | `{DomainObject}Repository`                             | `StockConsignmentRepository`, `LocationRepository`   | Interface in application-service layer                         |
| **Service Port**         | `{ExternalSystem}Service`                              | `D365IntegrationService`, `NotificationService`      | Interface for external integrations                            |
| **Event Publisher Port** | `{DomainObject}EventPublisher`                         | `StockEventPublisher`, `LocationEventPublisher`      | Interface extending `EventPublisher`                           |

**Application Service Naming Rules:**

- Commands: Verb (action) + Noun (object) + "Command"
- Queries: "Get" or "List" or "Find" + object + "Query"
- Handlers: Match command/query name + "Handler"
- Results: Match command/query name + "Result"

#### Application Layer Classes

| Type                   | Pattern                            | Example                                     | Notes                                                |
|------------------------|------------------------------------|---------------------------------------------|------------------------------------------------------|
| **Command Controller** | `{DomainObject}CommandController`  | `StockConsignmentCommandController`         | Annotated with `@RestController`, `@Tag`             |
| **Query Controller**   | `{DomainObject}QueryController`    | `StockConsignmentQueryController`           | Annotated with `@RestController`, `@Tag`             |
| **Command DTO**        | `{Action}{DomainObject}CommandDTO` | `ConfirmConsignmentCommandDTO`              | Request DTO for command endpoints                    |
| **Command Result DTO** | `{Action}{DomainObject}ResultDTO`  | `ConfirmConsignmentResultDTO`               | Response DTO for command endpoints                   |
| **Query DTO**          | `{DomainObject}QueryDTO`           | `ConsignmentQueryDTO`, `StockLevelQueryDTO` | Response DTO for query endpoints                     |
| **DTO Mapper**         | `{DomainObject}DTOMapper`          | `StockConsignmentDTOMapper`                 | Annotated with `@Component` or `@Mapper` (MapStruct) |
| **Exception Handler**  | `GlobalExceptionHandler`           | `GlobalExceptionHandler`                    | Annotated with `@RestControllerAdvice`               |
| **Error Response**     | `ErrorResponse`                    | `ErrorResponse`, `ValidationErrorResponse`  | Standard error structure                             |

**Application Layer Naming Rules:**

- Controllers suffixed with "CommandController" or "QueryController"
- DTOs suffixed with "DTO"
- Mappers suffixed with "Mapper" or "DTOMapper"
- One controller per aggregate root
- Request mapping: `/api/v1/{service}/{domain-objects}`

#### Data Access Classes

| Type                          | Pattern                                  | Example                                    | Notes                                |
|-------------------------------|------------------------------------------|--------------------------------------------|--------------------------------------|
| **JPA Entity**                | `{DomainObject}Entity`                   | `StockConsignmentEntity`, `LocationEntity` | Annotated with `@Entity`, `@Table`   |
| **Base Repository Adapter**   | `Base{DomainObject}PersistenceAdapter`   | `BaseStockConsignmentPersistenceAdapter`   | Implements repository port interface |
| **Cached Repository Adapter** | `Cached{DomainObject}PersistenceAdapter` | `CachedStockConsignmentPersistenceAdapter` | Decorates base adapter, adds caching |
| **JPA Repository**            | `{DomainObject}JpaRepository`            | `StockConsignmentJpaRepository`            | Extends `JpaRepository<Entity, ID>`  |
| **Entity Mapper**             | `{DomainObject}EntityMapper`             | `StockConsignmentEntityMapper`             | Converts JPA ↔ Domain entities       |
| **Custom Query Class**        | `{DomainObject}CustomQueryImpl`          | `StockConsignmentCustomQueryImpl`          | Custom query implementations         |

**Data Access Naming Rules:**

- JPA entities suffixed with "Entity"
- Base adapters prefixed with "Base", suffixed with "PersistenceAdapter"
- Cached adapters prefixed with "Cached", suffixed with "PersistenceAdapter"
- JPA repositories suffixed with "JpaRepository"
- Entity mappers suffixed with "EntityMapper"

#### Messaging Classes

| Type                     | Pattern                         | Example                                             | Notes                            |
|--------------------------|---------------------------------|-----------------------------------------------------|----------------------------------|
| **Event Publisher**      | `{Service}EventPublisherImpl`   | `StockManagementEventPublisherImpl`                 | Implements event publisher port  |
| **Event Listener**       | `{DomainObject}EventListener`   | `ConsignmentEventListener`, `LocationEventListener` | Annotated with `@Component`      |
| **Event Handler Method** | `on{DomainObject}{Action}Event` | `onStockConsignmentReceivedEvent`                   | Annotated with `@KafkaListener`  |
| **Message Mapper**       | `{DomainObject}EventMapper`     | `ConsignmentEventMapper`                            | Converts events ↔ domain objects |

**Messaging Naming Rules:**

- Publishers suffixed with "EventPublisherImpl"
- Listeners suffixed with "EventListener"
- Handler methods prefixed with "on", past tense event name
- Kafka topics: `{service}.{aggregate}.{event}` (e.g., `stock.consignment.received`)

#### Container Classes

| Type                 | Pattern                      | Example                                           | Notes                                   |
|----------------------|------------------------------|---------------------------------------------------|-----------------------------------------|
| **Main Application** | `{Service}Application`       | `StockManagementApplication`                      | Annotated with `@SpringBootApplication` |
| **Configuration**    | `{Component}Config`          | `DatabaseConfig`, `KafkaConfig`, `SecurityConfig` | Annotated with `@Configuration`         |
| **Bean Factory**     | `{Component}BeanFactory`     | `RepositoryBeanFactory`, `MessagingBeanFactory`   | Annotated with `@Configuration`         |
| **Health Indicator** | `{Component}HealthIndicator` | `KafkaHealthIndicator`, `RedisHealthIndicator`    | Implements `HealthIndicator`            |

**Container Naming Rules:**

- Main class: Service name + "Application"
- Configuration classes suffixed with "Config"
- Bean factory classes suffixed with "BeanFactory"
- Health indicators suffixed with "HealthIndicator"

### Method Naming

#### Domain Entity Methods

| Type                       | Pattern                          | Example                                              | Notes                              |
|----------------------------|----------------------------------|------------------------------------------------------|------------------------------------|
| **Business Logic**         | `{businessAction}()`             | `confirm()`, `expire()`, `assign()`, `allocate()`    | Business language, imperative mood |
| **State Validation**       | `is{State}()`, `can{Action}()`   | `isExpired()`, `canBeConfirmed()`, `hasCapacity()`   | Returns boolean                    |
| **Business Calculation**   | `calculate{Result}()`            | `calculateTotalQuantity()`, `calculateOccupancy()`   | Returns value object or primitive  |
| **Domain Event Publisher** | `{action}()` (internal)          | Internal method calls `addDomainEvent()`             | Not exposed publicly               |
| **Factory Method**         | `create{Object}()`, `generate()` | `createLine()`, `generateId()`                       | Creates related objects            |
| **Getters**                | `get{Attribute}()`               | `getConsignmentId()`, `getQuantity()`, `getStatus()` | Read-only access to state          |

**Domain Method Naming Rules:**

- Use ubiquitous language from business domain
- Avoid technical terms (persist, serialize, map, etc.)
- Business logic methods should reflect real-world actions
- Avoid "set" methods - use business actions instead
- Boolean queries: `is` or `can` prefix
- NO `update()`, `modify()`, `change()` - use specific business actions

#### Application Service Handler Methods

| Type                  | Pattern                                      | Example                                                 | Notes                             |
|-----------------------|----------------------------------------------|---------------------------------------------------------|-----------------------------------|
| **Command Execution** | `handle({Command}Command command)`           | `handle(ConfirmConsignmentCommand command)`             | Returns command result            |
| **Query Execution**   | `handle({Query}Query query)`                 | `handle(GetConsignmentQuery query)`                     | Returns query result              |
| **Event Publishing**  | `publishEvents(List<DomainEvent<?>> events)` | Internal helper method                                  | Delegated to event publisher port |
| **Validation**        | `validate{Aspect}()`                         | `validateConsignmentExists()`, `validateTenantAccess()` | Throws exception on failure       |

**Application Service Method Naming Rules:**

- All handlers use `handle()` method name
- Command parameter type distinguishes handlers
- Validation methods prefixed with `validate`
- Internal helper methods use camelCase

#### Repository Methods

| Type                 | Pattern                         | Example                                  | Notes                              |
|----------------------|---------------------------------|------------------------------------------|------------------------------------|
| **Save/Update**      | `save({DomainObject} entity)`   | `save(StockConsignment consignment)`     | Upsert operation                   |
| **Find by ID**       | `findById({DomainObject}Id id)` | `findById(ConsignmentId id)`             | Returns `Optional<T>`              |
| **Find by Criteria** | `findBy{Criteria}()`            | `findByStatus()`, `findExpiringSoon()`   | Returns `List<T>` or `Optional<T>` |
| **Exists Check**     | `existsBy{Criteria}()`          | `existsById()`, `existsByBatchNumber()`  | Returns `boolean`                  |
| **Delete**           | `deleteBy{Criteria}()`          | `deleteById()`, `deleteByStatus()`       | Void return or deletion count      |
| **Count**            | `countBy{Criteria}()`           | `countByStatus()`, `countExpiredStock()` | Returns `long`                     |

**Repository Method Naming Rules:**

- Use Spring Data JPA query method naming conventions
- Return `Optional<T>` for single results that may not exist
- Return `List<T>` for multiple results (never null)
- Boolean methods: `exists` prefix
- Counting methods: `count` prefix

#### REST Controller Methods

| Type                 | Pattern                                              | Example                                    | Notes                 |
|----------------------|------------------------------------------------------|--------------------------------------------|-----------------------|
| **Command Endpoint** | `{action}{DomainObject}()`                           | `confirmConsignment()`, `createLocation()` | POST/PUT/DELETE verbs |
| **Query Endpoint**   | `get{DomainObject}()`, `list{DomainObject}s()`       | `getConsignment()`, `listExpiringStock()`  | GET verb              |
| **Endpoint URL**     | `/api/v1/{service}/{domain-objects}/{id?}/{action?}` | `/api/v1/stock/consignments/{id}/confirm`  | Kebab-case            |

**REST Endpoint Naming Rules:**

- Method names: camelCase
- URLs: kebab-case
- Commands: POST to `/resources/{id}/{action}` or PUT to `/resources/{id}`
- Queries: GET to `/resources` or `/resources/{id}`
- Use plural resource names (`/consignments`, not `/consignment`)

#### Event Handler Methods

| Type              | Pattern                           | Example                                                     | Notes                           |
|-------------------|-----------------------------------|-------------------------------------------------------------|---------------------------------|
| **Event Handler** | `on{DomainObject}{Action}Event()` | `onConsignmentReceivedEvent()`, `onLocationAssignedEvent()` | Annotated with `@KafkaListener` |

**Event Handler Naming Rules:**

- Prefix with `on`
- Use past tense (event already happened)
- Match event class name

### Constants and Configuration

| Type                         | Pattern                         | Example                                                 | Notes                    |
|------------------------------|---------------------------------|---------------------------------------------------------|--------------------------|
| **Constants**                | `UPPER_SNAKE_CASE`              | `MAX_CONSIGNMENT_LINES`, `DEFAULT_EXPIRY_DAYS`          | Static final fields      |
| **Configuration Properties** | `kebab-case`                    | `stock-management.cache.ttl`, `kafka.consumer.group-id` | application.yml          |
| **Environment Variables**    | `UPPER_SNAKE_CASE`              | `DATABASE_URL`, `KAFKA_BOOTSTRAP_SERVERS`               | System environment       |
| **Kafka Topics**             | `{service}.{aggregate}.{event}` | `stock.consignment.received`, `location.assigned`       | Lowercase, dot-separated |
| **Redis Keys**               | `{service}:{aggregate}:{id}`    | `stock:consignment:123`, `location:warehouse:456`       | Colon-separated          |

### File and Module Naming

| Type                    | Pattern                           | Example                                                        | Notes                        |
|-------------------------|-----------------------------------|----------------------------------------------------------------|------------------------------|
| **Java Files**          | `{ClassName}.java`                | `StockConsignment.java`, `ConsignmentId.java`                  | Match class name             |
| **Test Files**          | `{ClassName}Test.java`            | `StockConsignmentTest.java`                                    | Same package as tested class |
| **Integration Tests**   | `{ClassName}IntegrationTest.java` | `StockRepositoryIntegrationTest.java`                          | Separate test source root    |
| **Maven Modules**       | `{service}-{layer}`               | `stock-management-domain-core`, `stock-management-application` | Kebab-case                   |
| **Configuration Files** | `application-{profile}.yml`       | `application-dev.yml`, `application-prod.yml`                  | Spring Boot convention       |

### Common Abbreviations (Avoid Unless Standard)

| Avoid         | Use Instead  | Exception Cases                     |
|---------------|--------------|-------------------------------------|
| `mgmt`, `mgt` | `management` | -                                   |
| `svc`         | `service`    | -                                   |
| `repo`        | `repository` | -                                   |
| `dto`         | -            | Acceptable as suffix only           |
| `id`          | -            | Acceptable for identifiers          |
| `jpa`         | -            | Acceptable for infrastructure layer |
| `api`         | -            | Acceptable for REST API layer       |
| `db`          | `database`   | Acceptable for properties           |
| `msg`         | `message`    | -                                   |
| `qty`         | `quantity`   | -                                   |

### Summary of Naming Principles

1. **Business Language First**: Use domain terminology, not technical jargon
2. **Consistency**: Same pattern across all services
3. **Clarity over Brevity**: Full words preferred over abbreviations
4. **Layer Suffixes**: Clear indication of architectural layer (`Entity`, `DTO`, `Mapper`, etc.)
5. **CQRS Separation**: Clear distinction between commands and queries
6. **Intent-Revealing**: Name should explain purpose without reading implementation
7. **No Redundancy**: Don't include package name in class name (`stock.ConsignmentRepository`, not `stock.StockConsignmentRepository`)
8. **Future-Proof**: Names should remain valid as business evolves

---

## Common Anti-Patterns to Avoid

This section details common violations of architectural principles with concrete examples of what NOT to do and the correct patterns to follow.

### Architectural Violations

#### 1. Repository Interface Misplacement

**Anti-Pattern:**

```java
// ❌ WRONG: Repository interface in domain core
package com.ccbsa.wms.stock.domain.core.repository;

public interface StockConsignmentRepository {
    void save(StockConsignment consignment);
    Optional<StockConsignment> findById(ConsignmentId id);
}
```

**Correct Pattern:**

```java
// ✅ CORRECT: Repository interface in application service layer
package com.ccbsa.wms.stock.application.service.port.repository;

public interface StockConsignmentRepository {
    void save(StockConsignment consignment);
    Optional<StockConsignment> findById(ConsignmentId id);
}
```

**Why This Matters:**

- Domain core must remain pure Java with no infrastructure dependencies
- Repository is a port (interface) that infrastructure adapts to
- Application service layer defines the contracts that infrastructure implements
- Dependency direction: Domain Core ← Application Service ← Data Access

---

#### 2. Infrastructure Concerns in Domain

**Anti-Pattern:**

```java
// ❌ WRONG: JPA annotations in domain entity
package com.ccbsa.wms.stock.domain.core.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stock_consignments")
public class StockConsignment extends AggregateRoot<ConsignmentId> {
    @Id
    private String id;

    @Column(name = "batch_number")
    private String batchNumber;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;
}
```

**Correct Pattern:**

```java
// ✅ CORRECT: Pure domain entity (domain core)
package com.ccbsa.wms.stock.domain.core.entity;

public class StockConsignment extends AggregateRoot<ConsignmentId> {
    private ConsignmentId id;
    private BatchNumber batchNumber;
    private LocationId locationId;

    // Business logic methods
    public void assignToLocation(LocationId locationId) {
        this.locationId = locationId;
        addDomainEvent(new ConsignmentLocationAssignedEvent(this.id, locationId));
    }
}

// ✅ CORRECT: Separate JPA entity (data access layer)
package com.ccbsa.wms.stock.dataaccess.entity;

@Entity
@Table(name = "stock_consignments")
public class StockConsignmentEntity {
    @Id
    private String id;

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "location_id")
    private String locationId;
}
```

**Why This Matters:**

- Domain entities represent business concepts, not database tables
- JPA annotations couple domain to infrastructure framework
- Domain should be testable without database
- Separate JPA entities allow different persistence strategies

---

#### 3. Missing Anti-Corruption Layer

**Anti-Pattern:**

```java
// ❌ WRONG: Exposing domain entities directly in REST API
@RestController
@RequestMapping("/api/v1/stock/consignments")
public class StockConsignmentController {

    @GetMapping("/{id}")
    public ResponseEntity<StockConsignment> getConsignment(@PathVariable String id) {
        StockConsignment consignment = repository.findById(ConsignmentId.of(id))
            .orElseThrow(() -> new NotFoundException());
        return ResponseEntity.ok(consignment); // ❌ Exposing domain entity
    }
}
```

**Correct Pattern:**

```java
// ✅ CORRECT: DTO layer as anti-corruption boundary
@RestController
@RequestMapping("/api/v1/stock/consignments")
public class StockConsignmentQueryController {

    private final GetConsignmentQueryHandler queryHandler;
    private final StockConsignmentDTOMapper mapper;

    @GetMapping("/{id}")
    public ResponseEntity<ConsignmentQueryDTO> getConsignment(@PathVariable String id) {
        GetConsignmentQuery query = new GetConsignmentQuery(id);
        ConsignmentQueryResult result = queryHandler.handle(query);
        ConsignmentQueryDTO dto = mapper.toDTO(result);
        return ResponseEntity.ok(dto); // ✅ Returns DTO
    }
}
```

**Why This Matters:**

- External API contracts should not change when domain evolves
- DTOs protect domain from external influences
- API versioning without domain changes
- Different representations for different clients

---

#### 4. CQRS Violations

**Anti-Pattern:**

```java
// ❌ WRONG: Returning domain entity from command
@Component
public class ConfirmConsignmentCommandHandler {

    @Transactional
    public StockConsignment handle(ConfirmConsignmentCommand command) {
        StockConsignment consignment = repository.findById(command.getConsignmentId())
            .orElseThrow(() -> new NotFoundException());

        consignment.confirm();
        repository.save(consignment);

        return consignment; // ❌ Returning full domain entity
    }
}
```

**Correct Pattern:**

```java
// ✅ CORRECT: Command returns focused result DTO
@Component
public class ConfirmConsignmentCommandHandler {

    @Transactional
    public ConfirmConsignmentResult handle(ConfirmConsignmentCommand command) {
        StockConsignment consignment = repository.findById(command.getConsignmentId())
            .orElseThrow(() -> new NotFoundException());

        consignment.confirm();
        repository.save(consignment);

        // Publish events
        eventPublisher.publish(consignment.getDomainEvents());
        consignment.clearDomainEvents();

        // ✅ Return command-specific result
        return ConfirmConsignmentResult.builder()
            .consignmentId(consignment.getId())
            .status(consignment.getStatus())
            .confirmedAt(consignment.getConfirmedAt())
            .build();
    }
}
```

**Why This Matters:**

- Commands should return minimal acknowledgment
- Queries optimized for reads, commands optimized for writes
- Event-driven updates for query models
- Clear separation of concerns

---

#### 5. Event Storage Anti-Pattern

**Anti-Pattern:**

```java
// ❌ WRONG: Storing events for replay (Event Sourcing when not needed)
@Entity
@Table(name = "domain_events")
public class DomainEventEntity {
    @Id
    private UUID eventId;

    @Column(columnDefinition = "jsonb")
    private String eventPayload;

    private Instant occurredOn;
}

@Repository
public interface DomainEventRepository extends JpaRepository<DomainEventEntity, UUID> {
    List<DomainEventEntity> findByAggregateId(UUID aggregateId);
}
```

**Correct Pattern:**

```java
// ✅ CORRECT: Events are transient - published and discarded
@Component
public class ConfirmConsignmentCommandHandler {

    @Transactional
    public ConfirmConsignmentResult handle(ConfirmConsignmentCommand command) {
        // Load aggregate from database (current state)
        StockConsignment consignment = repository.findById(command.getConsignmentId())
            .orElseThrow(() -> new NotFoundException());

        // Execute business logic (generates events)
        consignment.confirm();

        // Persist aggregate (current state only)
        repository.save(consignment);

        // Publish events to Kafka (transient - no storage)
        eventPublisher.publish(consignment.getDomainEvents());
        consignment.clearDomainEvents();

        return result;
    }
}
```

**Why This Matters:**

- Cloud deployment optimization - avoid unnecessary storage
- Events are for choreography, not audit trail
- Use database audit logs for compliance requirements
- Simplifies deployment and reduces cost

---

## Template Files Reference

All code templates are maintained in separate template files for clarity and maintainability:

1. **[@01-mandated-domain-core-templates.md](@01-mandated-domain-core-templates.md)** - Complete domain core templates
2. **[@02-mandated-application-service-templates.md](@02-mandated-application-service-templates.md)** - Application service templates
3. **[@03-mandated-application-layer-templates.md](@03-mandated-application-layer-templates.md)** - Application layer templates
4. **[@04-mandated-data-access-templates.md](@04-mandated-data-access-templates.md)** - Data access templates
5. **[@05-mandated-messaging-templates.md](@05-mandated-messaging-templates.md)** - Messaging templates
6. **[@06-mandated-container-templates.md](@06-mandated-container-templates.md)** - Container templates
7. **[@07-mandated-frontend-templates.md](@07-mandated-frontend-templates.md)** - Frontend templates

**These template files are the single source of truth for all code templates.** When implementing new features, always refer to these template files for the correct patterns and
structure.

## Next Steps

1. Review this guide and template files with the development team
2. Set up project structure following these templates
3. Begin implementation following the templates
4. Conduct code reviews to ensure template compliance
5. Update templates when patterns evolve (with team review)

---

**Document Control**

- **Version History:**
    - v2.0 (2025-01): Updated to reference separate template files, removed inline templates
    - v1.0 (2025-11): Initial draft
- **Review Cycle:** This document will be reviewed monthly or when patterns change
- **Distribution:** This document will be distributed to all development team members

**Related Documents:**

- [Service Architecture Document](../01-project-planning/architecture/Service_Architecture_Document.md) - Architectural principles

