# IAM Integration Guide - Keycloak

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Draft  
**Related Documents:**

- [Security Architecture Document](../architecture/Security_Architecture_Document.md)
- [Gateway Service Specification](../integration/Gateway_Service_Specification.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Keycloak Setup](#keycloak-setup)
3. [Realm Configuration](#realm-configuration)
4. [Client Configuration](#client-configuration)
5. [User Management](#user-management)
6. [Token Configuration](#token-configuration)
7. [Integration with Gateway](#integration-with-gateway)
8. [Integration with Services](#integration-with-services)
9. [Testing](#testing)
10. [Troubleshooting](#troubleshooting)

---

## Overview

### Purpose

This guide describes how to integrate Keycloak as the Identity and Access Management (IAM) solution for the Warehouse Management System.

### Keycloak Features Used

- **OAuth 2.0 / OpenID Connect** - Authentication protocol
- **JWT Tokens** - Access and refresh tokens
- **Realm Management** - Tenant isolation
- **Client Management** - API client configuration
- **User Management** - User accounts and roles
- **Role-Based Access Control** - Role and permission management

---

## Keycloak Setup

### Installation

**Docker Compose:**

```yaml
version: '3.8'

services:
  keycloak:
    image: quay.io/keycloak/keycloak:25.0.0
    container_name: wms-keycloak
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: keycloak
      KC_HOSTNAME: localhost
      KC_HTTP_PORT: 8080
    ports:
      - "8080:8080"
    command: start-dev
    depends_on:
      - postgres-keycloak
  
  postgres-keycloak:
    image: postgres:15
    container_name: wms-keycloak-db
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: keycloak
    ports:
      - "5433:5432"
    volumes:
      - keycloak_data:/var/lib/postgresql/data

volumes:
  keycloak_data:
```

### Initial Admin Setup

1. Start Keycloak: `docker-compose up -d`
2. Access Admin Console: `http://localhost:7080`
3. Login with admin credentials
4. Create initial realm: `wms-realm`

---

## Realm Configuration

### Realm Creation

1. **Create Realm:**
    - Name: `wms-realm`
    - Display Name: `Warehouse Management System`
    - Enabled: `true`

2. **Realm Settings:**
    - **Login:** Enable "User registration"
    - **Email:** Configure SMTP settings
    - **Tokens:** Configure token expiration
        - Access Token Lifespan: `1 hour`
        - Refresh Token Lifespan: `24 hours`

### Token Configuration

**Access Token:**

- Lifespan: 1 hour (3600 seconds)
- Signature Algorithm: RS256
- Include claims: `tenant_id`, `realm_access`, `resource_access`

**Refresh Token:**

- Lifespan: 24 hours (86400 seconds)
- Reuse: Disabled (rotate refresh tokens)

---

## Client Configuration

### API Client Setup

1. **Create Client:**
    - Client ID: `wms-api`
    - Client Protocol: `openid-connect`
    - Access Type: `confidential`
    - Standard Flow Enabled: `true`
    - Direct Access Grants Enabled: `true`
    - Valid Redirect URIs: `*`
    - Web Origins: `*`

2. **Client Credentials:**
    - Generate client secret
    - Store securely (environment variables)

3. **Mappers:**
    - **Tenant ID Mapper:**
        - Name: `tenant-id-mapper`
        - Mapper Type: `User Attribute`
        - User Attribute: `tenant_id`
        - Token Claim Name: `tenant_id`
        - Claim JSON Type: `String`
        - Add to ID token: `true`
        - Add to access token: `true`

### Frontend Client Setup

1. **Create Client:**
    - Client ID: `wms-frontend`
    - Client Protocol: `openid-connect`
    - Access Type: `public`
    - Standard Flow Enabled: `true`
    - Valid Redirect URIs: `http://localhost:3000/*`
    - Web Origins: `http://localhost:3000`

---

## User Management

### User Creation

1. **Create User:**
    - Username: `user@ldp-123.com`
    - Email: `user@ldp-123.com`
    - First Name: `John`
    - Last Name: `Doe`
    - Enabled: `true`
    - Email Verified: `true`

2. **Set Password:**
    - Temporary: `false`
    - Password: Set secure password

3. **Set Tenant ID:**
    - Attributes tab
    - Add attribute: `tenant_id` = `ldp-123`
    - **Note:** Tenant must exist in Tenant Service before assigning to user
    - Tenant ID should match TenantId from Tenant Service

### Role Assignment

1. **Assign Realm Roles:**
    - Roles tab
    - Assign roles: `OPERATOR`, `MANAGER`

2. **Role Mapping:**
    - Role: `OPERATOR`
    - Permissions: `stock:read`, `picking:execute`
    - Role: `MANAGER`
    - Permissions: `stock:read`, `stock:write`, `location:write`

---

## Token Configuration

### JWT Token Structure

**Access Token Claims:**

```json
{
  "sub": "user-uuid",
  "emailAddress": "user@ldp-123.com",
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
  "iss": "http://localhost:7080/realms/wms-realm"
}
```

### Custom Claims

**Tenant ID Claim:**

- Source: User attribute `tenant_id`
- Claim name: `tenant_id`
- Included in: Access token, ID token

**Roles Claim:**

- Source: Realm roles and client roles
- Claim name: `realm_access.roles` and `resource_access.wms-api.roles`
- Included in: Access token

---

## Integration with Gateway

### Gateway Configuration

**application.yml:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:7080/realms/wms-realm
          jwk-set-uri: http://localhost:7080/realms/wms-realm/protocol/openid-connect/certs
```

### JWT Validation

**GatewaySecurityConfig:**

```java
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {
    
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;
    
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
```

### Token Extraction

**TenantContextFilter:**

```java
@Component
public class TenantContextFilter extends AbstractGatewayFilterFactory<Config> {
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return ReactiveSecurityContextHolder.getContext()
                .cast(SecurityContext.class)
                .map(SecurityContext::getAuthentication)
                .cast(Jwt.class)
                .flatMap(jwt -> {
                    // Extract tenant_id from JWT
                    String tenantId = jwt.getClaim("tenant_id");
                    
                    // Inject header
                    ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-Tenant-Id", tenantId)
                        .header("X-User-Id", jwt.getSubject())
                        .build();
                    
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                });
        };
    }
}
```

---

## Integration with Services

### Service Configuration

**application.yml:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:7080/realms/wms-realm
          jwk-set-uri: http://localhost:7080/realms/wms-realm/protocol/openid-connect/certs
```

### Security Configuration

**ServiceSecurityConfig:**

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ServiceSecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(jwtDecoder())
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
```

---

## Testing

### Token Acquisition

**Using Direct Access Grant:**

```bash
curl -X POST http://localhost:7080/realms/wms-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=wms-api" \
  -d "client_secret=your-client-secret" \
  -d "username=user@ldp-123.com" \
  -d "password=user-password"
```

**Response:**

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_expires_in": 86400
}
```

### API Request with Token

```bash
curl -X GET http://localhost:8080/api/v1/stock-management/stock-counts \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-Tenant-Id: ldp-123"
```

### Token Refresh

```bash
curl -X POST http://localhost:7080/realms/wms-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token" \
  -d "client_id=wms-api" \
  -d "client_secret=your-client-secret" \
  -d "refresh_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## Troubleshooting

### Token Validation Failures

**Issue:** Token validation fails in gateway

**Solutions:**

1. Verify `jwk-set-uri` is correct
2. Check Keycloak is accessible from gateway
3. Verify token issuer matches realm URL
4. Check token expiration

### Missing Tenant ID Claim

**Issue:** `tenant_id` claim not present in token

**Solutions:**

1. Verify user has `tenant_id` attribute set
2. Check tenant-id-mapper is configured
3. Verify mapper is added to client
4. Regenerate token after mapper changes

### Cross-Tenant Access

**Issue:** User can access data from different tenant

**Solutions:**

1. Verify tenant validation filter is enabled
2. Check JWT contains correct tenant_id
3. Ensure repository queries filter by tenant
4. Validate tenant context in services

---

## Production Considerations

### High Availability

- Keycloak cluster setup
- Database replication
- Load balancing

### Security

- HTTPS/TLS for all communications
- Secure client secret storage
- Regular key rotation
- Audit logging enabled

### Performance

- Token caching
- Connection pooling
- Database optimization

---

## Tenant Service Integration

### Realm Strategy

**Two Approaches Supported:**

1. **Single Realm (Recommended for MVP):**
    - All tenants share one Keycloak realm: `wms-realm`
    - Users differentiated by `tenant_id` user attribute
    - Optional tenant groups for organization
    - Simpler to manage and configure

2. **Per-Tenant Realms:**
    - Each tenant has its own Keycloak realm
    - Realm name pattern: `tenant-{tenantId}` or from tenant configuration
    - Better isolation but more complex to manage
    - Realm name stored in tenant configuration

### Tenant Lifecycle Synchronization

**Tenant Service → Keycloak:**

- When tenant is created in Tenant Service:
    - If using per-tenant realms: optionally create Keycloak realm
    - If using single realm: optionally create tenant group
- When tenant is activated:
    - If using per-tenant realms: create/enable Keycloak realm, store realm name in tenant configuration
    - If using single realm: create/enable tenant group (optional)
- When tenant is deactivated: disable Keycloak realm/group
- When tenant is suspended: disable Keycloak realm/group

**Keycloak → Tenant Service:**

- Tenant IDs in Keycloak user attributes must match Tenant Service
- User creation should validate tenant exists in Tenant Service
- Tenant status changes should be reflected in Keycloak

### User Service Realm Determination

**User Service queries Tenant Service to determine correct realm:**

1. **Query Tenant Service:** `GET /api/v1/tenants/{id}/realm`
    - Returns realm name if tenant has specific realm configured
    - Returns empty if tenant uses default realm

2. **Fallback Strategy:**
    - If tenant-specific realm not found, use default realm from configuration
    - Default realm: `wms-realm` (configurable via `keycloak.admin.default-realm`)

3. **Validation:**
    - Validate tenant exists in Tenant Service
    - Validate tenant status is ACTIVE
    - Reject user creation for non-ACTIVE tenants

### Tenant Validation

**Before User Creation:**

1. Query Tenant Service for realm name: `GET /api/v1/tenants/{id}/realm`
2. Validate tenant exists in Tenant Service: `GET /api/v1/tenants/{id}`
3. Validate tenant status is ACTIVE
4. Determine realm name (tenant-specific or default)
5. Create user in Keycloak with `tenant_id` attribute
6. Optionally assign user to tenant group (if using groups)

**During Authentication:**

1. Extract `tenant_id` from JWT token
2. Validate tenant exists in Tenant Service (optional, cached)
3. Validate tenant status is ACTIVE
4. Reject authentication for non-ACTIVE tenants

---

## References

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [OAuth 2.0 Specification](https://oauth.net/2/)
- [OpenID Connect Specification](https://openid.net/connect/)
- [JWT Specification](https://tools.ietf.org/html/rfc7519)
- [Tenant Service Implementation Plan](../architecture/Tenant_Service_Implementation_Plan.md)
- [Multi-Tenancy Enforcement Guide](Multi_Tenancy_Enforcement_Guide.md)

---

**Document Status:** Draft  
**Last Updated:** 2025-01  
**Next Review:** 2025-04

