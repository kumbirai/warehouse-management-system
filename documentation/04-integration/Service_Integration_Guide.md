# Service Integration Guide - Tenant Context and Security

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Draft  
**Related Documents:**

- [Multi-Tenancy Enforcement Guide](../security/Multi_Tenancy_Enforcement_Guide.md)
- [Security Architecture Document](../architecture/Security_Architecture_Document.md)

---

## Overview

This guide describes how to integrate tenant context and security features into backend services.

---

## Quick Start

### 1. Add Common Security Dependency

**pom.xml:**

```xml
<dependency>
    <groupId>com.ccbsa.wms</groupId>
    <artifactId>common-security</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. Import Security Configuration

**Container Module Configuration:**

```java
@Configuration
@Import(com.ccbsa.wms.common.security.ServiceSecurityConfig.class)
public class ServiceConfiguration {
    // ServiceSecurityConfig automatically:
    // - Imports SecurityConfig (tenant context interceptor)
    // - Configures OAuth2 Resource Server with JWT validation
    // - Enables method security (@PreAuthorize)
}
```

**Or use component scanning:**

```java
@SpringBootApplication
@Import(com.ccbsa.wms.common.security.ServiceSecurityConfig.class)
public class StockManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockManagementApplication.class, args);
    }
}
```

### 3. Use Tenant Context

**In Application Services:**

```java
@Component
public class CreateStockConsignmentHandler {
    
    public StockConsignmentId handle(CreateStockConsignmentCommand command) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set");
        }
        
        // Use tenant ID in business logic
        StockConsignment consignment = StockConsignment.builder()
            .tenantId(tenantId.getValue())
            .build();
        
        return consignment.getId();
    }
}
```

---

## Tenant Context Usage

### Getting Tenant Context

```java
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;

// Get tenant ID
TenantId tenantId = TenantContext.getTenantId();

// Get user ID
UserId userId = TenantContext.getUserId();

// Validate tenant context is set
if (tenantId == null) {
    throw new IllegalStateException("Tenant context not set");
}
```

### Repository Queries

**Always filter by tenant:**

```java
@Repository
public class StockConsignmentRepositoryAdapter {
    
    public Optional<StockConsignment> findById(StockConsignmentId id) {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set");
        }
        
        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), id.getValue());
    }
}
```

---

## Tenant Service Integration

### Querying Tenant Service

Services can query the Tenant Service to validate tenant existence and status before performing operations.

**Example: Validate Tenant Before Operation**

```java
@Component
public class CreateStockConsignmentHandler {
    
    private final TenantServicePort tenantServicePort;
    
    @Transactional
    public StockConsignmentId handle(CreateStockConsignmentCommand command) {
        TenantId tenantId = TenantContext.getTenantId();
        
        // Validate tenant exists and is active
        if (!tenantServicePort.isTenantActive(tenantId)) {
            throw new IllegalStateException("Tenant is not active: " + tenantId);
        }
        
        // Proceed with business logic
        // ...
    }
}
```

**TenantServicePort Interface:**

```java
public interface TenantServicePort {
    boolean isTenantActive(TenantId tenantId);
    Optional<String> getTenantRealmName(TenantId tenantId);
    Optional<TenantView> getTenant(TenantId tenantId);
}
```

**Implementation:**

```java
@Component
public class TenantServiceAdapter implements TenantServicePort {
    
    private final RestTemplate restTemplate;
    private final String tenantServiceUrl;
    
    @Override
    public boolean isTenantActive(TenantId tenantId) {
        String status = restTemplate.getForObject(
            tenantServiceUrl + "/api/v1/tenants/" + tenantId.getValue() + "/status",
            String.class
        );
        return "ACTIVE".equals(status);
    }
    
    @Override
    public Optional<String> getTenantRealmName(TenantId tenantId) {
        TenantRealmResponse response = restTemplate.getForObject(
            tenantServiceUrl + "/api/v1/tenants/" + tenantId.getValue() + "/realm",
            TenantRealmResponse.class
        );
        return Optional.ofNullable(response != null ? response.getRealmName() : null);
    }
}
```

**Note:** User Service uses `TenantServicePort` to determine which Keycloak realm to use when creating users.

---

## Security Configuration

### Enable Method Security

**ServiceSecurityConfig is already configured in common-security.**

Just import it:

```java
@Configuration
@Import(com.ccbsa.wms.common.security.ServiceSecurityConfig.class)
public class ServiceConfiguration {
    // Method security is already enabled
    // JWT validation is already configured
    // Tenant context interceptor is already registered
}
```

**Configuration is done via application.yml:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:7080/realms/wms-realm
          jwk-set-uri: http://localhost:7080/realms/wms-realm/protocol/openid-connect/certs
```

### Use @PreAuthorize

**Controller:**

```java
@RestController
@RequestMapping("/api/v1/stock-management")
public class StockManagementController {
    
    @GetMapping("/stock-counts/{id}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER')")
    public ResponseEntity<StockCountDto> getStockCount(@PathVariable String id) {
        // Tenant context automatically validated
        return ResponseEntity.ok(service.getStockCount(id));
    }
    
    @PostMapping("/stock-counts")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<StockCountDto> createStockCount(@RequestBody CreateStockCountDto dto) {
        return ResponseEntity.ok(service.createStockCount(dto));
    }
}
```

---

## Best Practices

1. **Always Validate Tenant Context:**
    - Check `TenantContext.getTenantId()` is not null
    - Fail fast if tenant context missing

2. **Filter Queries by Tenant:**
    - All repository queries must include tenant filter
    - Never query without tenant context

3. **Use @PreAuthorize:**
    - Protect endpoints with role checks
    - Use permission-based checks where applicable

4. **Clear Context After Request:**
    - TenantContextInterceptor handles this automatically
    - Don't manually clear in application code

---

## Troubleshooting

### Tenant Context Not Set

**Error:** `IllegalStateException: Tenant context not set`

**Solution:**

1. Verify `SecurityConfig` is imported
2. Check `X-Tenant-Id` header is present
3. Verify gateway is injecting tenant header

### Authorization Failures

**Error:** `403 Forbidden`

**Solution:**

1. Check user has required role
2. Verify `@PreAuthorize` annotation is correct
3. Check JWT token contains roles

---

**Document Status:** Draft  
**Last Updated:** 2025-01

