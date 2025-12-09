# Mandated Data Access Templates

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Approved

---

## Overview

Templates for the **Data Access** module (`{service}-dataaccess`). Implements repository adapters and data adapters with JPA.

**Key Distinction:**

- **Repository Adapters** - Implement repository ports for aggregate persistence (write model)
- **Data Adapters** - Implement data ports for read model queries (projections/views)

---

## Package Structure

The Data Access module (`{service}-dataaccess`) follows a strict package structure to enforce adapter pattern and CQRS separation:

```
com.ccbsa.wms.{service}.dataaccess/
├── adapter/                           # Repository and data adapters
│   ├── {DomainObject}RepositoryAdapter.java      # Implements repository port (write model)
│   └── {DomainObject}ViewRepositoryAdapter.java  # Implements data port (read model)
├── entity/                            # JPA entities (infrastructure layer)
│   ├── {DomainObject}Entity.java                 # Aggregate entity (write model)
│   └── {DomainObject}ViewEntity.java              # View entity (read model)
├── mapper/                            # Entity mappers
│   ├── {DomainObject}EntityMapper.java            # Domain ↔ JPA entity mapper
│   └── {DomainObject}ViewEntityMapper.java       # View ↔ JPA view entity mapper
├── jpa/                               # JPA repositories
│   ├── {DomainObject}JpaRepository.java          # Aggregate JPA repository
│   └── {DomainObject}ViewJpaRepository.java       # View JPA repository
└── cache/                             # Cache decorators (optional)
    └── Cached{DomainObject}RepositoryAdapter.java
```

**Package Naming Convention:**

- Base package: `com.ccbsa.wms.{service}.dataaccess`
- Replace `{service}` with actual service name (e.g., `stock`, `location`, `product`)
- Replace `{DomainObject}` with actual domain object name (e.g., `StockConsignment`, `Location`, `Product`)

**Package Responsibilities:**

| Package   | Responsibility      | Contains                                                                                            |
|-----------|---------------------|-----------------------------------------------------------------------------------------------------|
| `adapter` | Repository adapters | Implements repository ports (write model) and data ports (read model), annotated with `@Repository` |
| `entity`  | JPA entities        | JPA annotated entities for persistence, separate from domain entities                               |
| `mapper`  | Entity mappers      | Converts between JPA entities and domain entities, annotated with `@Component`                      |
| `jpa`     | JPA repositories    | Spring Data JPA repository interfaces, extends `JpaRepository<Entity, ID>`                          |
| `cache`   | Cache decorators    | Decorator pattern implementations for caching, wraps repository adapters                            |

**Important Package Rules:**

- **Adapter pattern**: Adapters implement ports defined in application service layer
- **CQRS separation**: Repository adapters for write model, data adapters for read model
- **Entity separation**: JPA entities separate from domain entities
- **Mapper responsibility**: Mappers handle conversion between infrastructure and domain
- **Multi-tenant**: Entities use tenant-aware schema resolution

---

## Important Domain Driven Design, Clean Hexagonal Architecture principles, CQRS and Event Driven Design Notes

**Domain-Driven Design (DDD) Principles:**

- Data access layer is infrastructure (outer layer)
- Adapters adapt infrastructure to domain contracts (ports)
- Domain entities remain pure, no JPA annotations
- Persistence is an implementation detail

**Clean Hexagonal Architecture Principles:**

- Adapters implement ports defined in application service layer
- Dependency direction: Application Service → Data Access
- Domain core has no knowledge of persistence
- Infrastructure adapts to domain, not vice versa

**CQRS Principles:**

- **Repository adapters**: Persist aggregates (write model) via repository ports
- **Data adapters**: Query read models (projections) via data ports
- **Separate entities**: Write model entities vs read model view entities
- **Optimization**: Read model entities denormalized for query performance

**Event-Driven Design Principles:**

- Read models updated via event projections (asynchronously)
- Event handlers update view entities from domain events
- Eventual consistency between write and read models
- Projection listeners process events to update views

---

## Repository Adapter Template

```java
package com.ccbsa.wms.{service}.dataaccess.adapter;

import com.ccbsa.wms.{service}.application.service.port.repository.{DomainObject}Repository;
import com.ccbsa.wms.{service}.domain.core.entity.{DomainObject};
import com.ccbsa.wms.{service}.domain.core.valueobject.{DomainObject}Id;
import com.ccbsa.wms.{service}.dataaccess.entity.{DomainObject}Entity;
import com.ccbsa.wms.{service}.dataaccess.jpa.{DomainObject}JpaRepository;
import com.ccbsa.wms.{service}.dataaccess.mapper.{DomainObject}EntityMapper;
import com.ccbsa.common.domain.valueobject.TenantId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class {DomainObject}RepositoryAdapter implements {DomainObject}Repository {
    
    private final {DomainObject}JpaRepository jpaRepository;
    private final {DomainObject}EntityMapper mapper;
    
    public {DomainObject}RepositoryAdapter(
            {DomainObject}JpaRepository jpaRepository,
            {DomainObject}EntityMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public void save({DomainObject} {domainObject}) {
        // Check if entity already exists to handle version correctly for optimistic locking
        Optional<{DomainObject}Entity> existingEntity = 
            jpaRepository.findByTenantIdAndId(
                {domainObject}.getTenantId().getValue(),
                {domainObject}.getId().getValue()
            );

        {DomainObject}Entity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            updateEntityFromDomain(entity, {domainObject});
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity({domainObject});
        }

        jpaRepository.save(entity);
    }
    
    /**
     * Updates an existing entity with values from the domain model.
     * Preserves JPA managed state and version for optimistic locking.
     *
     * @param entity     Existing JPA entity
     * @param {domainObject} Domain aggregate
     */
    private void updateEntityFromDomain({DomainObject}Entity entity, {DomainObject} {domainObject}) {
        // Update all mutable fields from domain model
        entity.set{Attribute}({domainObject}.get{Attribute}().getValue());
        entity.setStatus({domainObject}.getStatus());
        entity.setLastModifiedAt({domainObject}.getLastModifiedAt());
        // ... update other mutable fields
        
        // Version is managed by JPA - don't update it manually
        // Hibernate will automatically increment version on update
    }
    
    @Override
    public Optional<{DomainObject}> findById({DomainObject}Id id) {
        return jpaRepository.findById(id.getValue())
            .map(mapper::toDomain);
    }
    
    @Override
    public Optional<{DomainObject}> findByTenantIdAndId(TenantId tenantId, {DomainObject}Id id) {
        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), id.getValue())
            .map(mapper::toDomain);
    }
    
    @Override
    public List<{DomainObject}> findByTenantId(TenantId tenantId) {
        return jpaRepository.findByTenantId(tenantId.getValue()).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById({DomainObject}Id id) {
        jpaRepository.deleteById(id.getValue());
    }
    
    @Override
    public boolean existsById({DomainObject}Id id) {
        return jpaRepository.existsById(id.getValue());
    }
}
```

## JPA Entity Template

```java
package com.ccbsa.wms.{service}.dataaccess.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity: {DomainObject}Entity
 * <p>
 * JPA representation of {DomainObject} aggregate.
 * <p>
 * Note: {DomainObject} is tenant-aware. Uses schema-per-tenant strategy via TenantAwarePhysicalNamingStrategy
 * for multi-tenant isolation. Each tenant has its own isolated PostgreSQL schema.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with
 * the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "{domain_objects}", schema = "tenant_schema")
public class {DomainObject}Entity {
    
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "{attribute}")
    private String {attribute};
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private {DomainObject}Status status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    @OneToMany(mappedBy = "{domainObject}", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<{ChildEntity}Entity> {childEntities} = new ArrayList<>();
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    // ... other getters/setters
}
```

## Entity Mapper Template

```java
package com.ccbsa.wms.{service}.dataaccess.mapper;

import com.ccbsa.wms.{service}.domain.core.entity.{DomainObject};
import com.ccbsa.wms.{service}.dataaccess.entity.{DomainObject}Entity;
import org.springframework.stereotype.Component;

@Component
public class {DomainObject}EntityMapper {
    
    /**
     * Converts Notification domain entity to NotificationEntity JPA entity.
     * <p>
     * For new entities (version == 0), version is not set to let Hibernate manage it.
     * For existing entities (version > 0), version is set to enable optimistic locking.
     *
     * @param {domainObject} Domain aggregate
     * @return JPA entity
     */
    public {DomainObject}Entity toEntity({DomainObject} {domainObject}) {
        {DomainObject}Entity entity = new {DomainObject}Entity();
        entity.setId({domainObject}.getId().getValue());
        entity.setTenantId({domainObject}.getTenantId().getValue());
        entity.set{Attribute}({domainObject}.get{Attribute}().getValue());
        entity.setStatus({domainObject}.getStatus());
        entity.setCreatedAt({domainObject}.getCreatedAt());
        entity.setLastModifiedAt({domainObject}.getLastModifiedAt());
        
        // For new entities, version will be set by Hibernate when persisting
        // For existing entities loaded from DB, version is already set
        // We only set version when mapping from domain if it's > 0 (existing entity)
        int domainVersion = {domainObject}.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }
        // For new entities (version == 0), don't set version - let Hibernate manage it
        
        return entity;
    }
    
    public {DomainObject} toDomain({DomainObject}Entity entity) {
        return {DomainObject}.builder()
            .{domainObject}Id({DomainObject}Id.of(entity.getId()))
            .tenantId(TenantId.of(entity.getTenantId()))
            .{attribute}({Attribute}.of(entity.get{Attribute}()))
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .lastModifiedAt(entity.getLastModifiedAt())
            .version(entity.getVersion() != null ? entity.getVersion().intValue() : 0)
            .buildWithoutEvents();
    }
}
```

### Critical: Version Field Handling with Optimistic Locking

**Problem:**
When using JPA `@Version` annotation for optimistic locking, setting `version = 0` for new entities causes Hibernate to treat them as existing entities, leading to optimistic
locking errors:

```
Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect)
```

**Solution Pattern:**

1. **Repository Adapter**: Check if entity exists before saving
    - If exists: Update existing managed entity (preserves version)
    - If new: Create new entity from mapper

2. **Entity Mapper**: Handle version field correctly
    - For new entities (version == 0): Don't set version field (let Hibernate manage it)
    - For existing entities (version > 0): Set version for optimistic locking

**Key Principles:**

- Never set `version = 0` for new entities in the mapper
- Always check entity existence in repository adapter before saving
- Preserve JPA managed state when updating existing entities
- Let Hibernate manage version field for new entities automatically

**Example Implementation:**
See `NotificationRepositoryAdapter` and `NotificationEntityMapper` in notification-service for reference implementation.

## Data Adapter Template (For Read Models)

```java
package com.ccbsa.wms.{service}.dataaccess.adapter;

import com.ccbsa.wms.{service}.application.service.port.data.{DomainObject}ViewRepository;
import com.ccbsa.wms.{service}.application.service.query.dto.{DomainObject}View;
import com.ccbsa.wms.{service}.dataaccess.entity.{DomainObject}ViewEntity;
import com.ccbsa.wms.{service}.dataaccess.jpa.{DomainObject}ViewJpaRepository;
import com.ccbsa.wms.{service}.dataaccess.mapper.{DomainObject}ViewEntityMapper;
import com.ccbsa.common.domain.valueobject.TenantId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Data Adapter: {DomainObject}ViewRepositoryAdapter
 * 
 * Implements data port for read model queries.
 * Used by query handlers for optimized read operations.
 */
@Repository
public class {DomainObject}ViewRepositoryAdapter implements {DomainObject}ViewRepository {
    
    private final {DomainObject}ViewJpaRepository jpaRepository;
    private final {DomainObject}ViewEntityMapper mapper;
    
    @Override
    public Optional<{DomainObject}View> findById({DomainObject}Id id) {
        return jpaRepository.findById(id.getValue())
            .map(mapper::toView);
    }
    
    @Override
    public Optional<{DomainObject}View> findByTenantIdAndId(TenantId tenantId, {DomainObject}Id id) {
        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), id.getValue())
            .map(mapper::toView);
    }
    
    @Override
    public List<{DomainObject}View> findByTenantId(TenantId tenantId) {
        return jpaRepository.findByTenantId(tenantId.getValue()).stream()
            .map(mapper::toView)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<{DomainObject}View> findByTenantIdAndStatus(TenantId tenantId, {DomainObject}Status status) {
        return jpaRepository.findByTenantIdAndStatus(tenantId.getValue(), status).stream()
            .map(mapper::toView)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<{DomainObject}View> findByTenantIdWithPagination(TenantId tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jpaRepository.findByTenantId(tenantId.getValue(), pageable).stream()
            .map(mapper::toView)
            .collect(Collectors.toList());
    }
}
```

## View Entity Template (Read Model)

```java
package com.ccbsa.wms.{service}.dataaccess.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * View Entity: {DomainObject}ViewEntity
 * <p>
 * JPA entity for read model (projection/view).
 * Denormalized for optimized queries.
 * Updated via event projections.
 * <p>
 * Note: View entities are tenant-aware. Uses schema-per-tenant strategy via TenantAwarePhysicalNamingStrategy
 * for multi-tenant isolation. Each tenant has its own isolated PostgreSQL schema.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with
 * the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "{domain_object}_views", schema = "tenant_schema")
public class {DomainObject}ViewEntity {
    
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    // Denormalized fields for fast queries
    @Column(name = "{attribute}")
    private String {attribute};
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private {DomainObject}Status status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;
    
    // Indexed fields for common queries
    @Column(name = "product_id")
    private UUID productId;
    
    @Column(name = "location_id")
    private UUID locationId;
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    // ... other getters/setters
}
```

## View Entity Mapper Template

```java
package com.ccbsa.wms.{service}.dataaccess.mapper;

import com.ccbsa.wms.{service}.application.service.query.dto.{DomainObject}View;
import com.ccbsa.wms.{service}.dataaccess.entity.{DomainObject}ViewEntity;
import org.springframework.stereotype.Component;

@Component
public class {DomainObject}ViewEntityMapper {
    
    public {DomainObject}View toView({DomainObject}ViewEntity entity) {
        return {DomainObject}View.builder()
            .id({DomainObject}Id.of(entity.getId()))
            .tenantId(TenantId.of(entity.getTenantId()))
            .{attribute}({Attribute}.of(entity.get{Attribute}()))
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .lastModifiedAt(entity.getLastModifiedAt())
            .build();
    }
}
```

## JPA Repository Template (For Aggregates)

```java
package com.ccbsa.wms.{service}.dataaccess.jpa;

import com.ccbsa.wms.{service}.dataaccess.entity.{DomainObject}Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface {DomainObject}JpaRepository extends JpaRepository<{DomainObject}Entity, UUID> {
    
    Optional<{DomainObject}Entity> findByTenantIdAndId(String tenantId, UUID id);
    
    List<{DomainObject}Entity> findByTenantId(String tenantId);
    
    List<{DomainObject}Entity> findByTenantIdAndStatus(String tenantId, {DomainObject}Status status);
    
    boolean existsByTenantIdAndId(String tenantId, UUID id);
}
```

## JPA View Repository Template (For Read Models)

```java
package com.ccbsa.wms.{service}.dataaccess.jpa;

import com.ccbsa.wms.{service}.dataaccess.entity.{DomainObject}ViewEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface {DomainObject}ViewJpaRepository extends JpaRepository<{DomainObject}ViewEntity, UUID> {
    
    Optional<{DomainObject}ViewEntity> findByTenantIdAndId(String tenantId, UUID id);
    
    List<{DomainObject}ViewEntity> findByTenantId(String tenantId);
    
    List<{DomainObject}ViewEntity> findByTenantId(String tenantId, Pageable pageable);
    
    List<{DomainObject}ViewEntity> findByTenantIdAndStatus(String tenantId, {DomainObject}Status status);
    
    // Optimized queries for read model
    List<{DomainObject}ViewEntity> findByTenantIdAndProductId(String tenantId, UUID productId);
    
    List<{DomainObject}ViewEntity> findByTenantIdAndLocationId(String tenantId, UUID locationId);
}
```

---

## Best Practices and Common Pitfalls

### Optimistic Locking with Version Fields

**Problem:**
When using JPA `@Version` annotation for optimistic locking, setting `version = 0` for new entities causes Hibernate to treat them as existing entities, leading to optimistic
locking errors:

```
Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect)
```

**Solution Pattern:**

1. **Repository Adapter**: Always check if entity exists before saving
    - If entity exists: Update existing managed entity (preserves JPA managed state and version)
    - If entity is new: Create new entity from mapper

2. **Entity Mapper**: Handle version field correctly
    - For new entities (version == 0): Don't set version field (let Hibernate manage it automatically)
    - For existing entities (version > 0): Set version for optimistic locking

**Key Principles:**

- ✅ Always check entity existence in repository adapter before saving
- ✅ For new entities (version == 0): Don't set version field in mapper
- ✅ For existing entities (version > 0): Set version for optimistic locking
- ✅ When updating existing entities: Preserve JPA managed state, don't manually update version
- ❌ Never set `version = 0` for new entities (causes optimistic locking errors)

**Reference Implementation:**

- `NotificationRepositoryAdapter.save()` - Correct pattern for checking entity existence
- `NotificationEntityMapper.toEntity()` - Correct version field handling
- `TenantRepositoryAdapter.save()` - Alternative reference implementation

**Example Error Scenario:**

```java
// ❌ WRONG - Causes optimistic locking error
public NotificationEntity toEntity(Notification notification) {
    NotificationEntity entity = new NotificationEntity();
    // ... set other fields ...
    entity.setVersion(Long.valueOf(notification.getVersion())); // Sets 0 for new entities
    return entity;
}
```

**Correct Implementation:**

```java
// ✅ CORRECT - Handles version correctly
public NotificationEntity toEntity(Notification notification) {
    NotificationEntity entity = new NotificationEntity();
    // ... set other fields ...
    
    // Only set version if entity already exists (version > 0)
    int domainVersion = notification.getVersion();
    if (domainVersion > 0) {
        entity.setVersion(Long.valueOf(domainVersion));
    }
    // For new entities (version == 0), don't set version - let Hibernate manage it
    return entity;
}
```

---

**Document Control**

- **Version History:**
    - v1.0 (2025-01) - Initial template creation
    - v1.1 (2025-12) - Added optimistic locking version field handling pattern
- **Review Cycle:** Review when data access patterns change

