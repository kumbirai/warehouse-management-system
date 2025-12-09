# Security Architecture Document

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Draft  
**Related Documents:**

- [Service Architecture Document](Service_Architecture_Document.md)
- [API Specifications](../api/API_Specifications.md)
- [Multi-Tenancy Enforcement Guide](../security/Multi_Tenancy_Enforcement_Guide.md)
- [IAM Integration Guide](../security/IAM_Integration_Guide.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Security Architecture](#security-architecture)
3. [Authentication](#authentication)
4. [Authorization](#authorization)
5. [Multi-Tenancy Security](#multi-tenancy-security)
6. [API Gateway Security](#api-gateway-security)
7. [Service-Level Security](#service-level-security)
8. [Data Security](#data-security)
9. [Network Security](#network-security)
10. [Security Monitoring](#security-monitoring)

---

## Overview

### Purpose

This document defines the security architecture for the Warehouse Management System Integration, including authentication, authorization, multi-tenancy enforcement, and data
protection mechanisms.

### Security Principles

1. **Defense in Depth** - Multiple layers of security controls
2. **Least Privilege** - Users and services have minimum required permissions
3. **Zero Trust** - Never trust, always verify
4. **Tenant Isolation** - Strict enforcement of tenant boundaries
5. **Security by Design** - Security built into every layer

---

## Security Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    External Clients                      │
│              (Web Browser, Mobile App)                   │
└──────────────────┬──────────────────────────────────────┘
                   │ HTTPS/TLS 1.2+
┌──────────────────▼──────────────────────────────────────┐
│              API Gateway Service                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Security Layer:                                   │  │
│  │  - JWT Token Validation                           │  │
│  │  - Tenant Context Extraction                      │  │
│  │  - Rate Limiting                                  │  │
│  │  - CORS Handling                                  │  │
│  └──────────────────────────────────────────────────┘  │
└──────────────────┬──────────────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
┌───────▼────────┐   ┌────────▼──────────┐
│  Keycloak IAM  │   │  Backend Services │
│  - User Auth   │   │  ┌──────────────┐ │
│  - Token Issuance│   │  │ Security     │ │
│  - User Mgmt    │   │  │ - Tenant     │ │
└────────────────┘   │  │   Validation │ │
                     │  │ - Role Check  │ │
                     │  │ - Permission  │ │
                     │  └──────────────┘ │
                     └──────────────────┘
```

### Security Layers

1. **Network Layer** - TLS encryption, network segmentation
2. **Gateway Layer** - Authentication, rate limiting, tenant validation
3. **Service Layer** - Authorization, tenant enforcement, permission checks
4. **Data Layer** - Encryption at rest, tenant isolation, access controls

---

## Authentication

### Authentication Flow

1. **User Login:**
    - User authenticates with Keycloak (username/password, SSO, MFA)
    - Keycloak issues JWT access token and refresh token
    - Access token contains: user_id, tenant_id, roles, permissions

2. **API Request:**
    - Client includes JWT token in `Authorization: Bearer <token>` header
    - Gateway validates token signature and expiration
    - Gateway extracts tenant context and user information
    - Gateway injects headers: `X-Tenant-Id`, `X-User-Id`, `X-Role`

3. **Token Refresh:**
    - Client uses refresh token to obtain new access token
    - Refresh token validated by Keycloak
    - New access token issued

### JWT Token Structure

```json
{
  "sub": "user-uuid",
  "tenant_id": "ldp-123",
  "realm_access": {
    "roles": ["OPERATOR", "MANAGER"]
  },
  "resource_access": {
    "wms-api": {
      "roles": ["stock:read", "stock:write"]
    }
  },
  "exp": 1234567890,
  "iat": 1234567890,
  "iss": "http://keycloak:7080/realms/wms-realm"
}
```

### Token Validation

- **Signature Validation** - Validates token signature using Keycloak public keys
- **Expiration Check** - Validates token has not expired
- **Issuer Validation** - Validates token issuer matches configured Keycloak realm
- **Tenant Validation** - Ensures tenant_id claim is present and valid

---

## Authorization

### Role-Based Access Control (RBAC)

#### System Roles

- **ADMIN** - Full system access across all tenants (system administrators)
- **MANAGER** - Warehouse management access within tenant
- **OPERATOR** - Operational access (picking, counting) within tenant
- **VIEWER** - Read-only access within tenant

#### Permission Model

Permissions are scoped to resources and actions:

- `stock:read` - Read stock information
- `stock:write` - Create/update stock
- `picking:read` - Read picking tasks
- `picking:execute` - Execute picking operations
- `location:read` - Read location information
- `location:write` - Create/update locations

### Authorization Enforcement

1. **Gateway Level:**
    - Validates token contains required roles
    - Rejects requests from unauthorized tenants

2. **Service Level:**
    - `@PreAuthorize` annotations on endpoints
    - Method-level security checks
    - Tenant context validation

3. **Domain Level:**
    - Aggregate-level tenant validation
    - Business rule enforcement

---

## Multi-Tenancy Security

### Tenant Isolation Strategy

**Schema per Tenant (MVP):**

- Each tenant has isolated PostgreSQL schema
- Tenant ID enforced at application layer
- Database-level schema permissions

### Tenant Boundary Enforcement

#### Gateway Level

1. **Tenant Validation Filter:**
    - Extracts tenant_id from JWT token
    - Validates tenant_id matches requested tenant (if specified)
    - Rejects cross-tenant access attempts

2. **Tenant Context Injection:**
    - Injects `X-Tenant-Id` header from JWT
    - Ensures tenant context propagates to backend services

#### Service Level

1. **Tenant Context Interceptor:**
    - Extracts `X-Tenant-Id` header
    - Sets tenant context in ThreadLocal
    - Validates tenant ID is present

2. **Repository Layer:**
    - All queries filtered by tenant ID
    - Schema resolution based on tenant context
    - Prevents cross-tenant data access

3. **Domain Layer:**
    - Aggregate roots validate tenant matches context
    - Tenant ID immutable after creation

### Tenant Validation Rules

1. **JWT Token Must Contain tenant_id Claim**
2. **Request Tenant Must Match Token Tenant**
3. **Aggregate Tenant Must Match Context Tenant**
4. **No Cross-Tenant Data Access**

---

## API Gateway Security

### Gateway Responsibilities

1. **Authentication:**
    - JWT token validation
    - Token signature verification
    - Token expiration check

2. **Tenant Context Management:**
    - Extract tenant_id from JWT
    - Validate tenant boundaries
    - Inject tenant context headers

3. **Rate Limiting:**
    - Per-tenant rate limits
    - Per-user rate limits
    - IP-based fallback

4. **Request Routing:**
    - Route to appropriate backend service
    - Load balancing
    - Circuit breaker protection

5. **CORS Handling (Exclusive Gateway Responsibility):**
    - Configured allowed origins
    - Preflight request handling
    - All CORS headers managed at gateway level
    - Backend services MUST NOT implement CORS configuration

### Gateway Security Filters

- **TenantValidationFilter** - Validates tenant from JWT matches request
- **TenantContextFilter** - Extracts and injects tenant context
- **RateLimiterFilter** - Enforces rate limits per tenant/user

---

## Service-Level Security

### Security Filter Chain

Each service implements:

1. **Tenant Context Interceptor:**
    - Extracts `X-Tenant-Id` header
    - Validates tenant ID presence
    - Sets ThreadLocal context

2. **Security Configuration:**
    - OAuth2 Resource Server configuration
    - JWT decoder configuration
    - Method security enabled

3. **Authorization Checks:**
    - `@PreAuthorize` annotations on endpoints
    - Role-based access control
    - Permission-based access control

### CORS Policy for Services

**Critical Architectural Rule:** Backend microservices MUST NOT implement CORS configuration.

**Rationale:**

1. **Gateway-Only Access:** Services are only accessible through the gateway in a secure private network. Browsers never directly access services.

2. **CORS is Browser-Specific:** CORS is a browser security feature. Since services are not directly accessed by browsers, CORS configuration is unnecessary and should be avoided.

3. **Single Point of Configuration:** CORS is exclusively handled at the gateway level, ensuring consistent policy enforcement and reducing configuration complexity.

4. **Security Best Practice:** Services should not expose CORS headers. The gateway handles all CORS-related headers and preflight requests.

**Service Implementation Rules:**

- ❌ **DO NOT** create `CorsConfigurationSource` beans
- ❌ **DO NOT** configure CORS filters
- ❌ **DO NOT** manually add CORS headers to responses
- ✅ **DO** rely on gateway for all CORS handling
- ✅ **Exception:** Development/testing direct access may require temporary CORS (must be disabled in production)

### Service Security Pattern

```java
@RestController
@RequestMapping("/api/v1/stock-management")
public class StockManagementController {
    
    @GetMapping("/stock-counts/{id}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER')")
    public ResponseEntity<StockCountDto> getStockCount(@PathVariable String id) {
        // Tenant context automatically validated by interceptor
        // Repository queries automatically filtered by tenant
        return ResponseEntity.ok(service.getStockCount(id));
    }
}
```

---

## Data Security

### Encryption

1. **Encryption in Transit:**
    - TLS 1.2+ for all communications
    - HTTPS for external access
    - Mutual TLS for service-to-service (future)

2. **Encryption at Rest:**
    - Database encryption (PostgreSQL TDE)
    - Backup encryption
    - File system encryption

### Data Protection

1. **PII Protection:**
    - Minimal data collection
    - Data anonymization where possible
    - Access logging and audit trails

2. **Data Retention:**
    - Defined retention policies
    - Secure data deletion
    - Compliance with regulations

---

## Network Security

### Network Segmentation

1. **Public Network:**
    - API Gateway exposed
    - TLS termination
    - DDoS protection

2. **Private Network:**
    - Backend services
    - Database
    - Message broker

3. **Network Policies:**
    - Kubernetes network policies
    - Firewall rules
    - VPC isolation

---

## Security Monitoring

### Security Events

1. **Authentication Events:**
    - Login attempts
    - Failed authentication
    - Token refresh

2. **Authorization Events:**
    - Permission denied
    - Cross-tenant access attempts
    - Role changes

3. **Security Incidents:**
    - Rate limit violations
    - Suspicious activity
    - Data access anomalies

### Logging and Auditing

- All security events logged
- Audit trail for sensitive operations
- Centralized log aggregation
- Security event correlation

### Security Metrics

- Authentication success/failure rates
- Authorization denial rates
- Rate limit violations
- Cross-tenant access attempts
- Token expiration rates

---

## Security Best Practices

### Development

1. **Secure Coding:**
    - Input validation
    - Output encoding
    - SQL injection prevention
    - XSS prevention

2. **Dependency Management:**
    - Regular dependency updates
    - Vulnerability scanning
    - Security patches

3. **Code Review:**
    - Security-focused reviews
    - Threat modeling
    - Security testing

### Operations

1. **Access Management:**
    - Principle of least privilege
    - Regular access reviews
    - Account lifecycle management

2. **Incident Response:**
    - Security incident procedures
    - Breach notification
    - Recovery procedures

3. **Compliance:**
    - Regular security audits
    - Compliance monitoring
    - Documentation maintenance

---

## Security Configuration

### Keycloak Configuration

- Realm: `wms-realm`
- Client: `wms-api`
- Token expiration: 1 hour
- Refresh token expiration: 24 hours

### Gateway Configuration

- JWT validation: Enabled
- Rate limiting: Per tenant/user
- CORS: Configured origins (exclusive gateway responsibility)
- Tenant validation: Strict

### Service Configuration

- Tenant context interceptor: Enabled
- Method security: Enabled
- JWT validation: Enabled
- CORS: NOT configured (handled by gateway)

---

## Security Testing

### Security Test Types

1. **Authentication Testing:**
    - Token validation
    - Expiration handling
    - Refresh token flow

2. **Authorization Testing:**
    - Role-based access
    - Permission checks
    - Tenant isolation

3. **Penetration Testing:**
    - Vulnerability scanning
    - Security assessment
    - Threat modeling

---

## References

- [OAuth 2.0 Specification](https://oauth.net/2/)
- [JWT Specification](https://tools.ietf.org/html/rfc7519)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Security Documentation](https://spring.io/projects/spring-security)

---

**Document Status:** Draft  
**Last Updated:** 2025-01  
**Next Review:** 2025-04

