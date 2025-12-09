# Multi-Tenancy Enforcement Guide

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Draft  
**Related Documents:**

- [Security Architecture Document](../architecture/Security_Architecture_Document.md)
- [Service Architecture Document](../architecture/Service_Architecture_Document.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Tenant Model](#tenant-model)
3. [Enforcement Layers](#enforcement-layers)
4. [Gateway-Level Enforcement](#gateway-level-enforcement)
5. [Service-Level Enforcement](#service-level-enforcement)
6. [Repository-Level Enforcement](#repository-level-enforcement)
7. [Domain-Level Enforcement](#domain-level-enforcement)
8. [Testing Tenant Isolation](#testing-tenant-isolation)
9. [Best Practices](#best-practices)

---

## Overview

### Purpose

This guide describes how tenant boundaries are enforced across all layers of the system to ensure complete data isolation between tenants (LDPs).

### Tenant Isolation Strategy

**Schema per Tenant (MVP):**

- Each tenant has isolated PostgreSQL schema
- Tenant ID enforced at multiple layers
- Database-level schema permissions

### Enforcement Principles

1. **Fail Secure** - Default to denying access if tenant context is missing
2. **Defense in Depth** - Multiple layers of enforcement
3. **Explicit Validation** - Never assume tenant context
4. **Audit Trail** - Log all tenant-related operations

---

## Tenant Model

### Tenant Entity

**Tenant Service:** The `tenant-service` manages the Tenant entity and lifecycle.

**Tenant Aggregate:**

- `Tenant` - Aggregate root managed by Tenant Service
- Contains tenant metadata (name, contact information)
- Manages tenant status (PENDING, ACTIVE, INACTIVE, SUSPENDED)
- Stores tenant configuration (settings, preferences, limits)

**Tenant Lifecycle:**

- **PENDING** - Tenant created but not yet activated
- **ACTIVE** - Tenant is active and operational
- **INACTIVE** - Tenant is deactivated (cannot access system)
- **SUSPENDED** - Tenant is temporarily suspended

### Tenant Identification

- **Tenant ID:** Unique identifier for each LDP (Local Distribution Partner)
- **Format:** String, max 50 characters, wrapped in `TenantId` ValueObject
- **Source:** JWT token `tenant_id` claim (validated against Tenant Service)
- **Propagation:** Via `X-Tenant-Id` HTTP header

### Tenant Validation

**Tenant Existence Check:**

- Services should validate tenant exists before operations
- Query Tenant Service: `GET /api/v1/tenants/{id}`
- Use circuit breaker for resilience
- Cache tenant list for performance (with TTL)

**Tenant Status Check:**

- Only ACTIVE tenants can access the system
- PENDING, INACTIVE, and SUSPENDED tenants are rejected
- Gateway validates tenant status during authentication

### Tenant Context

Tenant context is stored in ThreadLocal and contains:

- `TenantId` - Current tenant identifier (ValueObject)
- `UserId` - Current user identifier

### Schema Creation

**Event-Driven Schema Creation:**

All backend microservices (except `tenant-service`) implement schema-per-tenant pattern:

- Each service listens to `TenantSchemaCreatedEvent` from `tenant-service`
- Service creates tenant schema in its database
- Service runs Flyway migrations programmatically in tenant schema
- All tables and indexes created in tenant-specific schemas

**Schema Naming:**

- Format: `tenant_{sanitized_tenant_id}_schema`
- Example: `tenant_qui_ea_eum_schema`

**Implementation:**

- See [Schema-Per-Tenant Implementation Pattern](../01-architecture/Schema_Per_Tenant_Implementation_Pattern.md) for detailed guide
- All services must implement `TenantSchemaCreatedEventListener`

---

## Enforcement Layers

### Layer 1: Gateway Level

**Responsibility:** Initial tenant validation and context extraction

**Enforcement:**

- JWT token must contain `tenant_id` claim
- Tenant ID extracted from JWT
- Tenant ID injected as `X-Tenant-Id` header
- Cross-tenant access attempts rejected

### Layer 2: Service Level

**Responsibility:** Tenant context validation and propagation

**Enforcement:**

- `X-Tenant-Id` header must be present
- Tenant context set in ThreadLocal
- Request rejected if tenant ID missing

### Layer 3: Repository Level

**Responsibility:** Tenant-aware data access

**Enforcement:**

- All queries filtered by tenant ID
- Schema resolution based on tenant
- Cross-tenant queries prevented

### Layer 4: Domain Level

**Responsibility:** Business rule enforcement

**Enforcement:**

- Aggregate tenant must match context tenant
- Tenant ID immutable after creation
- Cross-tenant operations rejected

---

## Gateway-Level Enforcement

### TenantValidationFilter

**Location:** `gateway-service/gateway-container`

**Function:**

1. Extracts `tenant_id` from JWT token
2. Validates tenant ID is present
3. Validates requested tenant matches JWT tenant (if specified)
4. Rejects cross-tenant access attempts

**Implementation:**

```java
@Component
public class TenantValidationFilter extends AbstractGatewayFilterFactory<Config> {
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Extract tenant from JWT
            String jwtTenantId = extractTenantId(jwt);
            
            // Validate tenant is present
            if (jwtTenantId == null) {
                return handleError(exchange, HttpStatus.FORBIDDEN, 
                    "Tenant ID not found in token");
            }
            
            // Validate tenant matches request (if specified)
            String requestedTenantId = request.getHeaders().getFirst("X-Tenant-Id");
            if (requestedTenantId != null && !jwtTenantId.equals(requestedTenantId)) {
                return handleError(exchange, HttpStatus.FORBIDDEN,
                    "Tenant ID mismatch");
            }
            
            // Inject tenant ID header
            requestBuilder.header("X-Tenant-Id", jwtTenantId);
            
            return chain.filter(exchange);
        };
    }
}
```

### TenantContextFilter

**Function:**

1. Extracts tenant ID, user ID, and roles from JWT
2. Injects headers: `X-Tenant-Id`, `X-User-Id`, `X-Role`
3. Ensures tenant context propagates to backend services

---

## Service-Level Enforcement

### TenantContextInterceptor

**Location:** `common/common-security`

**Function:**

1. Extracts `X-Tenant-Id` header from request
2. Validates tenant ID is present
3. Sets tenant context in ThreadLocal
4. Clears context after request completion

**Implementation:**

```java
@Component
public class TenantContextInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        String tenantIdValue = request.getHeader("X-Tenant-Id");
        
        if (tenantIdValue == null || tenantIdValue.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        
        TenantContext.setTenantId(TenantId.of(tenantIdValue));
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request,
                               HttpServletResponse response,
                               Object handler,
                               Exception ex) {
        TenantContext.clear();
    }
}
```

### Security Configuration

**Location:** `common/common-security`

**Function:**

- Registers `TenantContextInterceptor` for all requests
- Excludes actuator and error endpoints

**Usage in Services:**

```java
@Configuration
@Import(SecurityConfig.class)
public class ServiceConfiguration {
    // Tenant context interceptor automatically registered
}
```

---

## Repository-Level Enforcement

### Tenant-Aware Queries

**Pattern:** All repository queries must include tenant filter

**Implementation:**

```java
@Repository
public class StockConsignmentRepositoryAdapter implements StockConsignmentRepository {
    
    @Override
    public Optional<StockConsignment> findById(StockConsignmentId id) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set");
        }
        
        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), id.getValue());
    }
    
    @Override
    public List<StockConsignment> findByStatus(ConsignmentStatus status) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set");
        }
        
        return jpaRepository.findByTenantIdAndStatus(tenantId.getValue(), status);
    }
}
```

### Schema Resolution

**Pattern:** Dynamic schema switching based on tenant using Hibernate PhysicalNamingStrategy

**Implementation:**

The system uses `TenantAwarePhysicalNamingStrategy` to dynamically resolve tenant schemas at runtime. This approach is necessary because Hibernate does not evaluate SpEL
expressions in `@Table` annotations.

**Configuration:**

1. **Import MultiTenantDataAccessConfig** in your service configuration:

```java
@Configuration
@Import({ServiceSecurityConfig.class, MultiTenantDataAccessConfig.class})
public class ServiceConfiguration {
    // TenantSchemaResolver and TenantAwarePhysicalNamingStrategy are now available
}
```

2. **Configure Hibernate naming strategy** in `application.yml`:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        physical_naming_strategy: com.ccbsa.wms.common.dataaccess.naming.TenantAwarePhysicalNamingStrategy
```

**JPA Entity:**

Use the placeholder schema `"tenant_schema"` in your entity annotations. The naming strategy will dynamically replace it with the actual tenant schema at runtime:

```java
@Entity
@Table(name = "stock_consignments", schema = "tenant_schema")
public class StockConsignmentEntity {
    // The schema "tenant_schema" is a placeholder that will be dynamically 
    // replaced with the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy
    // ...
}
```

**Important:** Do not use SpEL expressions (e.g., `#{@tenantSchemaResolver.resolveSchema()}`) in `@Table` annotations, as Hibernate does not evaluate them.

**How It Works:**

1. **Startup Validation:** When tenant context is not set (during schema validation), the naming strategy returns `null`, causing Hibernate to use the default schema (public) where
   Flyway creates tables
2. **Runtime Operations:** When tenant context is available, the naming strategy resolves the actual tenant schema (e.g., `tenant_abc123_schema`) and Hibernate uses it for queries

**TenantSchemaResolver:**

The `TenantSchemaResolver` component provides the schema resolution logic:

```java
@Component
public class TenantSchemaResolver {
    
    public String resolveSchema() {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set");
        }
        
        String sanitizedTenantId = sanitizeTenantId(tenantId.getValue());
        return String.format("tenant_%s_schema", sanitizedTenantId);
    }
}
```

---

## Domain-Level Enforcement

### Tenant-Aware Aggregates

**Base Class:** `TenantAwareAggregateRoot`

**Function:**

- Ensures all aggregates have tenant ID
- Validates tenant matches context
- Prevents cross-tenant operations
- Provides standard initialization pattern

**Implementation:**

```java
import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.TenantId;

public abstract class TenantAwareAggregateRoot<ID> extends AggregateRoot<ID> {
    
    protected TenantId tenantId;
    
    /**
     * Protected no-arg constructor for builder pattern and reflection-based construction.
     * Subclasses should use the constructor that requires TenantId when possible.
     * The tenantId must be set via setTenantId() before the aggregate is used.
     */
    protected TenantAwareAggregateRoot() {
        super();
    }
    
    /**
     * Protected constructor that ensures TenantId is always set.
     * This is the preferred constructor for subclasses.
     */
    protected TenantAwareAggregateRoot(TenantId tenantId) {
        super();
        setTenantId(tenantId);
    }
    
    /**
     * Sets the tenant ID with validation.
     */
    protected void setTenantId(TenantId tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null for tenant-aware aggregates");
        }
        this.tenantId = tenantId;
    }
    
    /**
     * Returns the tenant ID.
     * Validates that tenantId is set before returning.
     */
    public TenantId getTenantId() {
        if (tenantId == null) {
            throw new IllegalStateException("TenantId has not been initialized for this tenant-aware aggregate");
        }
        return tenantId;
    }
    
    /**
     * Validates that the aggregate's tenant matches the context tenant.
     */
    public void validateTenant(TenantId contextTenantId) {
        if (contextTenantId == null) {
            throw new IllegalArgumentException("Context tenant ID cannot be null");
        }
        if (!getTenantId().equals(contextTenantId)) {
            throw new TenantMismatchException(
                "Aggregate tenant does not match context tenant");
        }
    }
}
```

### Domain Service Validation

**Pattern:** Validate tenant in application services

**Implementation:**

```java
import com.ccbsa.common.domain.valueobject.TenantId;

@Component
public class CreateStockConsignmentHandler {
    
    public StockConsignmentId handle(CreateStockConsignmentCommand command) {
        TenantId contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            throw new IllegalStateException("Tenant context not set");
        }
        
        // Validate command tenant matches context
        if (!command.getTenantId().equals(contextTenantId)) {
            throw new TenantMismatchException("Command tenant mismatch");
        }
        
        // Create aggregate with tenant ID using builder pattern
        StockConsignment consignment = StockConsignment.builder()
            .consignmentId(ConsignmentId.generate())
            .tenantId(contextTenantId)
            .build();
        
        return consignment.getId();
    }
}
```

**Alternative: Using Constructor Pattern**

```java
// If aggregate provides constructor that requires TenantId
public class StockConsignment extends TenantAwareAggregateRoot<ConsignmentId> {
    
    private StockConsignment(TenantId tenantId) {
        super(tenantId); // Ensures tenantId is set
        // ... other initialization
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private TenantId tenantId;
        
        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }
        
        public StockConsignment build() {
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            StockConsignment consignment = new StockConsignment(tenantId);
            // ... set other fields
            return consignment;
        }
    }
}
```

---

## Testing Tenant Isolation

### Unit Tests

**Test Tenant Context:**

```java
import com.ccbsa.common.domain.valueobject.TenantId;

@Test
void testTenantIsolation() {
    // Set tenant context
    TenantId tenantId = TenantId.of("tenant-1");
    TenantContext.setTenantId(tenantId);
    
    // Create aggregate with TenantId ValueObject
    StockConsignment consignment = StockConsignment.builder()
        .consignmentId(ConsignmentId.generate())
        .tenantId(tenantId)
        .build();
    
    // Verify tenant matches (comparing TenantId ValueObjects)
    assertEquals(tenantId, consignment.getTenantId());
    
    // Clear context
    TenantContext.clear();
}
```

### Integration Tests

**Test Cross-Tenant Access Prevention:**

```java
@Test
void testCrossTenantAccessPrevention() {
    // Set tenant-1 context
    TenantId tenant1 = TenantId.of("tenant-1");
    TenantContext.setTenantId(tenant1);
    
    // Try to access tenant-2 data
    TenantId tenant2 = TenantId.of("tenant-2");
    assertThrows(TenantMismatchException.class, () -> {
        repository.findByTenantIdAndId(tenant2, aggregateId);
    });
    
    TenantContext.clear();
}
```

### End-to-End Tests

**Test Gateway Tenant Validation:**

```java
@Test
void testGatewayTenantValidation() {
    // Request with tenant-1 token
    String token = getTokenForTenant("tenant-1");
    
    // Try to access tenant-2 data
    ResponseEntity<?> response = restTemplate.exchange(
        "/api/v1/stock-management/stock-counts/{id}",
        HttpMethod.GET,
        new HttpEntity<>(headersWithToken(token)),
        Object.class,
        tenant2StockCountId
    );
    
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
}
```

---

## Best Practices

### Development

1. **Always Validate Tenant Context:**
    - Check `TenantContext.getTenantId()` is not null
    - Validate tenant matches before operations
    - Fail fast if tenant context missing

2. **Never Trust Client Input:**
    - Always use tenant from context, not request
    - Validate tenant ID format using `TenantId.of()` factory method
    - Reject malformed tenant IDs

3. **Use Tenant-Aware Base Classes:**
    - Extend `TenantAwareAggregateRoot`
    - Use `TenantId` ValueObject, not String
    - Use tenant-aware repositories
    - Implement tenant validation methods

4. **Initialization Pattern:**
    - Prefer constructor with `TenantId` parameter for guaranteed initialization
    - Use builder pattern with `tenantId()` method that accepts `TenantId` ValueObject
    - Always validate `TenantId` is not null before setting

5. **Log Tenant Operations:**
    - Log tenant ID in all operations (use `tenantId.getValue()` for logging)
    - Audit cross-tenant access attempts
    - Monitor tenant-related errors

### Code Review Checklist

- [ ] Tenant context validated in all entry points
- [ ] Repository queries filtered by tenant
- [ ] Domain aggregates use `TenantId` ValueObject, not String
- [ ] Domain aggregates validate tenant
- [ ] Cross-tenant access prevented
- [ ] Tenant context cleared after request
- [ ] Error messages don't leak tenant information
- [ ] Builder pattern uses `TenantId` ValueObject

### Common Pitfalls

1. **Missing Tenant Validation:**
    - Always validate tenant context is set
    - Never assume tenant context exists

2. **Using String Instead of TenantId ValueObject:**
    - Always use `TenantId` ValueObject in domain layer
    - Use `TenantId.of(string)` to create from String
    - Use `tenantId.getValue()` only when converting to String (e.g., for JPA entities)

3. **Cross-Tenant Queries:**
    - Always include tenant filter in queries
    - Never query without tenant context

4. **Tenant Context Leakage:**
    - Clear tenant context after request
    - Don't share tenant context between threads

5. **Error Message Leakage:**
    - Don't include tenant IDs in error messages
    - Use generic error messages for security

---

## Troubleshooting

### Tenant Context Not Set

**Symptoms:**

- `IllegalStateException: Tenant context not set`
- `IllegalStateException: TenantId has not been initialized for this tenant-aware aggregate`
- Null pointer exceptions in repository queries

**Solutions:**

1. Ensure `TenantContextInterceptor` is registered
2. Verify `X-Tenant-Id` header is present
3. Check gateway is injecting tenant header
4. Verify aggregate is initialized with `TenantId` (via constructor or builder)

### Cross-Tenant Access

**Symptoms:**

- `TenantMismatchException`
- 403 Forbidden responses

**Solutions:**

1. Verify JWT token contains correct tenant_id
2. Check gateway tenant validation filter
3. Ensure repository queries filter by tenant
4. Verify aggregate tenant matches context tenant

### Schema Resolution Issues

**Symptoms:**

- Database errors about missing schema
- Queries failing with schema not found

**Solutions:**

1. Verify tenant schema exists in database
2. Check schema resolver implementation
3. Ensure tenant ID format matches schema name

### TenantId Initialization Issues

**Symptoms:**

- `IllegalArgumentException: TenantId cannot be null for tenant-aware aggregates`
- `IllegalStateException: TenantId has not been initialized for this tenant-aware aggregate`

**Solutions:**

1. Ensure aggregate is created with `TenantId` ValueObject (not String)
2. Use constructor with `TenantId` parameter or builder pattern
3. Verify `setTenantId()` is called before aggregate is used
4. Check that `TenantId.of()` is used to create ValueObject from String

### Tenant Not Found or Inactive

**Symptoms:**

- `404 Not Found` when querying tenant service
- `403 Forbidden` for tenant status validation
- `TenantNotFoundException`

**Solutions:**

1. Verify tenant exists in Tenant Service
2. Check tenant status is ACTIVE
3. Ensure tenant was properly onboarded
4. Verify JWT token contains correct tenant_id
5. Check Tenant Service is accessible and healthy

---

## Tenant Lifecycle Management

### Tenant Onboarding

**Process:**

1. **Create Tenant** - Admin creates tenant via Tenant Service API
    - `POST /api/v1/tenants`
    - Tenant created with status PENDING
    - Tenant metadata stored (name, contact information)

2. **Create Tenant Schema** - Database schema created for tenant
    - Triggered by `TenantCreatedEvent`
    - PostgreSQL schema created: `{tenant_id}_schema`
    - Tables created in tenant schema

3. **Activate Tenant** - Admin activates tenant
    - `PUT /api/v1/tenants/{id}/activate`
    - Tenant status changed to ACTIVE
    - `TenantActivatedEvent` published
    - Tenant can now access system

4. **Keycloak Integration** - Tenant realm/group created in Keycloak
    - Triggered by `TenantActivatedEvent`
    - Keycloak realm or group created for tenant
    - Users can be assigned to tenant

### Tenant Status Management

**Status Transitions:**

- **PENDING → ACTIVE** - Tenant onboarding complete
- **ACTIVE → INACTIVE** - Tenant deactivated (cannot access system)
- **ACTIVE → SUSPENDED** - Tenant temporarily suspended
- **SUSPENDED → ACTIVE** - Tenant suspension lifted
- **SUSPENDED → INACTIVE** - Tenant permanently deactivated
- **INACTIVE → ACTIVE** - Tenant reactivated

**Status Validation:**

- Gateway validates tenant status during authentication
- Only ACTIVE tenants can access the system
- Services should validate tenant status before operations

### Tenant Deactivation

**Process:**

1. **Deactivate Tenant** - Admin deactivates tenant
    - `PUT /api/v1/tenants/{id}/deactivate`
    - Tenant status changed to INACTIVE
    - `TenantDeactivatedEvent` published
    - Existing user sessions invalidated

2. **Data Retention** - Tenant data retained
    - Tenant data remains in database
    - Tenant schema preserved
    - Can be reactivated later

### Tenant Suspension

**Process:**

1. **Suspend Tenant** - Admin suspends tenant
    - `PUT /api/v1/tenants/{id}/suspend`
    - Tenant status changed to SUSPENDED
    - `TenantSuspendedEvent` published
    - Existing user sessions invalidated

2. **Temporary Restriction** - Tenant cannot access system
    - All API requests rejected
    - Can be reactivated later

---

## References

- [Service Architecture Document](../architecture/Service_Architecture_Document.md)
- [Tenant Service Implementation Plan](../architecture/Tenant_Service_Implementation_Plan.md)

- [Security Architecture Document](../architecture/Security_Architecture_Document.md)
- [Service Architecture Document](../architecture/Service_Architecture_Document.md)
- [TenantContext JavaDoc](../common/common-security/src/main/java/com/ccbsa/wms/common/security/TenantContext.java)

---

**Document Status:** Draft  
**Last Updated:** 2025-01  
**Next Review:** 2025-04
