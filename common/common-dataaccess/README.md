# Common Data Access Module

## Overview

The `common-dataaccess` module provides shared data access utilities for multi-tenant schema resolution across all
services (except `tenant-service`).

## Purpose

This module implements the **schema-per-tenant** strategy for multi-tenant isolation, ensuring that each tenant (LDP)
has its own isolated PostgreSQL schema.

## Key Components

### TenantSchemaResolver

A Spring component that resolves the database schema name based on the current tenant context.

**Schema Naming Convention:** `tenant_{sanitized_tenant_id}_schema`

**Features:**

- Automatically sanitizes tenant IDs to ensure valid PostgreSQL identifiers
- Converts to lowercase
- Replaces hyphens and special characters with underscores
- Handles edge cases (IDs starting with digits, very long IDs)

### TenantAwarePhysicalNamingStrategy

A Hibernate `PhysicalNamingStrategy` that dynamically resolves tenant schemas at runtime.

**How it works:**

- Intercepts schema resolution during Hibernate operations
- Replaces the placeholder schema `"tenant_schema"` with the actual tenant schema
- Uses `TenantSchemaResolver` to resolve the schema name based on tenant context
- During startup validation (when tenant context is not set): returns `null` to use default schema (public)
- At runtime (when tenant context is available): resolves and returns the actual tenant schema

**Features:**

- Implements `ApplicationContextAware` to access Spring beans
- Handles startup validation gracefully when tenant context is not available
- Automatically resolves tenant schema at runtime based on `TenantContext`

### MultiTenantDataAccessConfig

Spring configuration class that auto-configures:

- `TenantSchemaResolver` bean
- `TenantAwarePhysicalNamingStrategy` bean

## Usage

### 1. Add Dependency

Add the `common-dataaccess` dependency to your service's container module:

```xml
<dependency>
    <groupId>com.ccbsa.wms</groupId>
    <artifactId>common-dataaccess</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. Import Configuration

Import `MultiTenantDataAccessConfig` in your service configuration:

```java
@Configuration
@Import({ServiceSecurityConfig.class, MultiTenantDataAccessConfig.class})
public class ServiceConfiguration {
    // TenantSchemaResolver is now available
}
```

### 3. Configure Hibernate Naming Strategy

Configure the tenant-aware physical naming strategy in your `application.yml`:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        physical_naming_strategy: com.ccbsa.wms.common.dataaccess.naming.TenantAwarePhysicalNamingStrategy
```

### 4. Use in JPA Entities

Use the placeholder schema `"tenant_schema"` in your JPA entity annotations. The `TenantAwarePhysicalNamingStrategy`
will dynamically replace it with the actual tenant schema at
runtime:

```java
@Entity
@Table(name = "users", schema = "tenant_schema")
public class UserEntity {
    // The schema "tenant_schema" is a placeholder that will be dynamically 
    // replaced with the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy
    // ...
}
```

**Important:** Do not use SpEL expressions (e.g., `#{@tenantSchemaResolver.resolveSchema()}`) in `@Table` annotations,
as Hibernate does not evaluate them. Use the placeholder
`"tenant_schema"` instead.

## Schema Resolution Strategy

The module implements **schema-per-tenant** isolation:

- Each tenant has its own isolated PostgreSQL schema
- Schema names are derived from tenant IDs: `tenant_{sanitized_tenant_id}_schema`
- Tenant IDs are automatically sanitized to ensure valid PostgreSQL identifiers
- Dynamic schema resolution at runtime via `TenantAwarePhysicalNamingStrategy`
- Startup validation uses default schema (public) when tenant context is not available
- Runtime operations use tenant-specific schemas when tenant context is set

### How Schema Resolution Works

1. **Entity Definition:** Entities use placeholder schema `"tenant_schema"` in `@Table` annotation
2. **Hibernate Configuration:** `TenantAwarePhysicalNamingStrategy` is configured in `application.yml`
3. **Startup Validation:** When tenant context is not set (during schema validation), the naming strategy returns
   `null`, causing Hibernate to use the default schema (public) where
   Flyway creates tables
4. **Runtime Operations:** When tenant context is available, the naming strategy resolves the actual tenant schema (
   e.g., `tenant_abc123_schema`) and Hibernate uses it for queries

## Example

For a tenant with ID `"my-tenant-123"`:

- Sanitized: `my_tenant_123`
- Schema name: `tenant_my_tenant_123_schema`

## Excluded Services

The `tenant-service` does **not** use this module as it manages tenant data itself and does not require multi-tenant
isolation.

## Dependencies

- `common-domain` - For `TenantId` value object
- `common-security` - For `TenantContext` access

## Testing

The module includes comprehensive unit tests for:

- Schema name resolution
- Tenant ID sanitization
- Edge cases (special characters, long IDs, etc.)
- Error handling (missing tenant context)

