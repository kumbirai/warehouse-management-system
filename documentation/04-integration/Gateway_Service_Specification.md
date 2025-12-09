# Gateway Service Specification

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Draft  
**Related Documents:**

- [Security Architecture Document](../architecture/Security_Architecture_Document.md)
- [API Specifications](../api/API_Specifications.md)
- [IAM Integration Guide](../security/IAM_Integration_Guide.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Routing Configuration](#routing-configuration)
4. [Security Features](#security-features)
5. [Rate Limiting](#rate-limiting)
6. [Filters](#filters)
7. [CORS Configuration](#cors-configuration)
8. [Configuration](#configuration)
9. [Monitoring](#monitoring)
10. [Deployment](#deployment)

---

## Overview

### Purpose

The Gateway Service is the single entry point for all external API requests. It provides authentication, authorization, routing, rate limiting, and tenant context management.

### Responsibilities

1. **Authentication** - JWT token validation via Keycloak
2. **Authorization** - Role and permission validation
3. **Routing** - Request routing to backend services
4. **Tenant Context** - Extraction and propagation of tenant context
5. **Rate Limiting** - Per-tenant and per-user rate limiting
6. **CORS** - Cross-origin resource sharing handling (exclusive gateway responsibility)
7. **Load Balancing** - Request distribution across service instances

**Important:** CORS is exclusively handled at the gateway level. Backend microservices operate in a secure private network and should NOT implement CORS configuration. All
cross-origin requests are handled by the gateway before routing to backend services.

---

## Architecture

### Technology Stack

- **Spring Cloud Gateway** - API Gateway framework
- **Spring Security** - Security framework
- **OAuth2 Resource Server** - JWT token validation
- **Spring Cloud Netflix Eureka** - Service discovery and registration
- **Spring Cloud LoadBalancer** - Client-side load balancing
- **Redis** - Rate limiting storage
- **Resilience4j** - Circuit breaker and retry

### Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    External Clients                      │
└──────────────────┬──────────────────────────────────────┘
                   │ HTTPS
┌──────────────────▼──────────────────────────────────────┐
│              Gateway Service                             │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Security Layer                                   │  │
│  │  - JWT Validation                                 │  │
│  │  - Tenant Validation                               │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Filter Chain                                     │  │
│  │  - TenantContextFilter                            │  │
│  │  - TenantValidationFilter                         │  │
│  │  - RateLimiterFilter                              │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Routing Layer                                   │  │
│  │  - Route Configuration                             │  │
│  │  - Load Balancing                                 │  │
│  └──────────────────────────────────────────────────┘  │
└──────────────────┬──────────────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
┌───────▼────────┐   ┌────────▼──────────┐
│  Keycloak IAM  │   │  Backend Services│
└────────────────┘   └──────────────────┘
```

---

## Routing Configuration

### Route Definitions

**Stock Management Service:**

- Path: `/api/v1/stock-management/**`
- Target: `lb://stock-management-service`
- Strip Prefix: 2

**Location Management Service:**

- Path: `/api/v1/location-management/**`
- Target: `lb://location-management-service`
- Strip Prefix: 2

**Product Service:**

- Path: `/api/v1/products/**`
- Target: `lb://product-service`
- Strip Prefix: 2

**Picking Service:**

- Path: `/api/v1/picking/**`
- Target: `lb://picking-service`
- Strip Prefix: 2

**Returns Service:**

- Path: `/api/v1/returns/**`
- Target: `lb://returns-service`
- Strip Prefix: 2

**Reconciliation Service:**

- Path: `/api/v1/reconciliation/**`
- Target: `lb://reconciliation-service`
- Strip Prefix: 2

**Integration Service:**

- Path: `/api/v1/integration/**`
- Target: `lb://integration-service`
- Strip Prefix: 2

**User Service (BFF):**

- Path: `/api/v1/bff/**`
- Target: `lb://user-service`
- Strip Prefix: 2
- **Note:** BFF endpoints (login/refresh) are public and do not require tenant validation

**Tenant Service:**

- Path: `/api/v1/tenants/**`
- Target: `lb://tenant-service`
- Strip Prefix: 2

**Notification Service:**

- Path: `/api/v1/notifications/**`
- Target: `lb://notification-service`
- Strip Prefix: 2

### Service Discovery

The gateway uses **Spring Cloud Netflix Eureka** for service discovery. Services automatically register with the Eureka server on startup, and the gateway discovers them
dynamically.

**Eureka Configuration:**

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
    fetch-registry: true
    register-with-eureka: true
  instance:
    prefer-ip-address: false
    hostname: ${EUREKA_INSTANCE_HOSTNAME:localhost}
    lease-renewal-interval-in-seconds: 30
    lease-expiration-duration-in-seconds: 90
```

**Discovery Locator:**

- `enabled: true` - Enables automatic route creation based on registered services
- `lower-case-service-id: true` - Converts service IDs to lowercase for routing

**Service Registration:**
All backend services register with Eureka using their `spring.application.name`:

- Services automatically register on startup
- Services send heartbeats every 30 seconds
- Services are removed from registry if heartbeat fails for 90 seconds

### Load Balancing

- **Strategy:** Round-robin (default via Spring Cloud LoadBalancer)
- **Service Discovery:** Spring Cloud Netflix Eureka
- **Health Checks:** Enabled via Eureka health monitoring
- **Instance Selection:** LoadBalancer selects healthy instances from Eureka registry

---

## Security Features

### JWT Token Validation

**Validation Steps:**

1. Extract token from `Authorization: Bearer <token>` header
2. Validate token signature using Keycloak public keys
3. Validate token expiration
4. Validate token issuer matches configured realm
5. Extract claims: `tenant_id`, `sub`, `realm_access`

**Configuration:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://keycloak:7080/realms/wms-realm
          jwk-set-uri: http://keycloak:7080/realms/wms-realm/protocol/openid-connect/certs
```

### Tenant Validation

**TenantValidationFilter:**

- Extracts `tenant_id` from JWT token
- Validates tenant ID is present
- Validates requested tenant matches JWT tenant (if specified)
- Rejects cross-tenant access attempts
- Returns 403 Forbidden on validation failure

### Tenant Context Extraction

**TenantContextFilter:**

- Extracts `tenant_id` from JWT token
- Extracts `sub` (user ID) from JWT token
- Extracts `realm_access.roles` from JWT token
- Injects headers:
    - `X-Tenant-Id`: Tenant identifier
    - `X-User-Id`: User identifier
    - `X-Role`: Comma-separated roles

---

## Rate Limiting

### Rate Limit Strategy

**Per-Tenant Rate Limiting:**

- Key: `tenant:{tenant_id}`
- Default: 100 requests/second
- Burst: 200 requests

**Per-User Rate Limiting:**

- Key: `user:{user_id}`
- Default: 50 requests/second
- Burst: 100 requests

**Fallback:**

- IP-based rate limiting if tenant/user not available
- Key: `ip:{ip_address}`

### Configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: stock-management-service
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
                key-resolver: "#{@tenantKeyResolver}"
        - id: tenant-service
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 50
                redis-rate-limiter.burstCapacity: 100
                key-resolver: "#{@tenantKeyResolver}"
```

### Redis Configuration

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
```

---

## Filters

### TenantValidationFilter

**Purpose:** Validate tenant from JWT matches request

**Order:** Before routing

**Function:**

1. Extract `tenant_id` from JWT token
2. Validate tenant ID is present
3. If `X-Tenant-Id` header present, validate it matches JWT tenant
4. Reject request if validation fails
5. Inject `X-Tenant-Id` header from JWT

**Error Response:**

```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "Tenant ID mismatch: token tenant does not match requested tenant",
    "timestamp": "2025-01-15T10:30:00Z"
  }
}
```

### TenantContextFilter

**Purpose:** Extract tenant context and inject headers

**Order:** After authentication, before routing

**Function:**

1. Extract `tenant_id` from JWT token
2. Extract `sub` (user ID) from JWT token
3. Extract `realm_access.roles` from JWT token
4. Inject headers: `X-Tenant-Id`, `X-User-Id`, `X-Role`

### RateLimiterFilter

**Purpose:** Enforce rate limits per tenant/user

**Order:** After tenant context extraction

**Function:**

1. Resolve rate limit key (tenant or user)
2. Check rate limit against Redis
3. Reject request if limit exceeded
4. Return 429 Too Many Requests on limit exceeded

---

## CORS Configuration

### CORS as Gateway Responsibility

**Architectural Principle:** CORS (Cross-Origin Resource Sharing) is exclusively a gateway responsibility. This is a critical architectural decision for the following reasons:

1. **Single Entry Point:** The gateway is the only service exposed to external clients (browsers, mobile apps). All cross-origin requests must pass through the gateway.

2. **Secure Network Architecture:** Backend microservices operate in a private secure network and are not directly accessible by browsers. They do not need CORS configuration.

3. **Centralized Configuration:** CORS policies are managed in one place (gateway), ensuring consistency and reducing configuration drift.

4. **Security Best Practice:** CORS is a browser security feature. Since browsers only interact with the gateway, CORS should only be configured at the gateway level.

5. **Performance:** Handling CORS at the gateway eliminates redundant CORS processing in backend services.

### CORS Configuration

**Global CORS Configuration:**

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:3000"
              - "http://localhost:5173"
              - "${CORS_ALLOWED_ORIGINS:}"
            allowedMethods:
              - GET
              - POST
              - PUT
              - PATCH
              - DELETE
              - OPTIONS
            allowedHeaders:
              - "*"
            allowCredentials: true
            maxAge: 3600
```

### CORS Configuration Details

**Allowed Origins:**

- Development: `http://localhost:3000`, `http://localhost:5173`
- Production: Configured via `CORS_ALLOWED_ORIGINS` environment variable
- Wildcard origins (`*`) are NOT allowed when `allowCredentials: true`

**Allowed Methods:**

- Standard HTTP methods: GET, POST, PUT, PATCH, DELETE
- OPTIONS method for preflight requests

**Allowed Headers:**

- All headers allowed (`*`) for flexibility
- Custom headers: `X-Tenant-Id`, `X-User-Id`, `X-Correlation-ID`, `Authorization`

**Credentials:**

- `allowCredentials: true` - Required for JWT token authentication
- Cookies and authorization headers are included in cross-origin requests

**Preflight Caching:**

- `maxAge: 3600` - Preflight responses cached for 1 hour

### Backend Service CORS Policy

**Critical:** Backend microservices MUST NOT implement CORS configuration:

1. **No CORS Configuration:** Services should not have `CorsConfigurationSource` beans or CORS filter configurations.

2. **No CORS Headers:** Services should not manually add CORS headers to responses. The gateway handles all CORS headers.

3. **Private Network:** Services are only accessible through the gateway in a secure private network.

4. **Exception:** Services may have CORS configuration ONLY for development/testing when accessed directly (not through gateway). This should be clearly documented and disabled in
   production.

### CORS Preflight Handling

The gateway automatically handles OPTIONS preflight requests:

1. Browser sends OPTIONS request with CORS headers
2. Gateway validates origin against allowed origins
3. Gateway responds with appropriate CORS headers
4. Browser proceeds with actual request if preflight succeeds

### Environment-Specific Configuration

**Development:**

```yaml
allowedOrigins:
  - "http://localhost:3000"
  - "http://localhost:5173"
```

**Production:**

```yaml
allowedOrigins: ${CORS_ALLOWED_ORIGINS}
# Example: "https://app.example.com,https://admin.example.com"
```

---

## Configuration

### Application Configuration

**application.yml:**

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service
  
  cloud:
    gateway:
      routes:
        # Route definitions
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 50
            redis-rate-limiter.burstCapacity: 100
            key-resolver: "#{@tenantKeyResolver}"
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:3000"
              - "http://localhost:5173"
              - "${CORS_ALLOWED_ORIGINS:}"
            allowedMethods:
              - GET
              - POST
              - PUT
              - PATCH
              - DELETE
              - OPTIONS
            allowedHeaders:
              - "*"
            allowCredentials: true
            maxAge: 3600
  
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI}
          jwk-set-uri: ${KEYCLOAK_JWK_SET_URI}
  
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

# Eureka Client Configuration
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
    fetch-registry: true
    register-with-eureka: true
  instance:
    prefer-ip-address: false
    hostname: ${EUREKA_INSTANCE_HOSTNAME:localhost}
    lease-renewal-interval-in-seconds: 30
    lease-expiration-duration-in-seconds: 90
```

### Environment Variables

- `KEYCLOAK_ISSUER_URI` - Keycloak realm issuer URI
- `KEYCLOAK_JWK_SET_URI` - Keycloak JWK set URI
- `REDIS_HOST` - Redis host
- `REDIS_PORT` - Redis port
- `REDIS_PASSWORD` - Redis password (optional)
- `CORS_ALLOWED_ORIGINS` - Comma-separated list of allowed CORS origins (production)
- `EUREKA_SERVER_URL` - Eureka server URL (default: http://localhost:8761/eureka/)
- `EUREKA_INSTANCE_HOSTNAME` - Hostname for Eureka registration (default: localhost)

---

## Monitoring

### Health Checks

**Endpoints:**

- `/actuator/health` - Health check
- `/actuator/info` - Service information
- `/actuator/metrics` - Metrics endpoint
- `/actuator/gateway` - Gateway routes and filters

### Metrics

**Key Metrics:**

- Request count per route
- Response time per route
- Rate limit violations
- Authentication failures
- Tenant validation failures

### Logging

**Log Levels:**

- `DEBUG` - Gateway routing and filter execution
- `INFO` - Request routing and security events
- `WARN` - Rate limit violations and validation failures
- `ERROR` - Authentication failures and errors

---

## Deployment

### Docker Configuration

**Dockerfile:**

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY gateway-container/target/gateway-container-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes Deployment

**Deployment:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: gateway-service
  template:
    metadata:
      labels:
        app: gateway-service
    spec:
      containers:
      - name: gateway
        image: wms/gateway-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: KEYCLOAK_ISSUER_URI
          value: "http://keycloak:7080/realms/wms-realm"
        - name: REDIS_HOST
          value: "redis"
```

### Service Configuration

**Service:**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: gateway-service
spec:
  selector:
    app: gateway-service
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

---

## Testing

### Unit Tests

**Test Tenant Validation:**

```java
@Test
void testTenantValidation() {
    // Test tenant validation filter
    // Verify tenant ID extracted from JWT
    // Verify headers injected correctly
}
```

### Integration Tests

**Test Routing:**

```java
@Test
void testRouting() {
    // Test request routing to backend services
    // Verify load balancing
    // Verify tenant context propagation
}
```

### End-to-End Tests

**Test Authentication Flow:**

```java
@Test
void testAuthenticationFlow() {
    // Test JWT validation
    // Test tenant context extraction
    // Test rate limiting
}
```

---

## Troubleshooting

### Common Issues

1. **Token Validation Failures:**
    - Verify Keycloak is accessible
    - Check JWK set URI is correct
    - Verify token issuer matches realm

2. **Routing Failures:**
    - Verify Eureka server is running and accessible at http://localhost:8761
    - Check Eureka dashboard to see if services are registered
    - Verify backend services are running and registered with Eureka
    - Check Eureka client configuration in service application.yml files
    - Verify service names match between gateway routes and Eureka registrations
    - Check load balancer configuration and Eureka integration

3. **Rate Limiting Issues:**
    - Verify Redis is accessible
    - Check rate limit configuration
    - Verify key resolver is configured

---

## References

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [Security Architecture Document](../architecture/Security_Architecture_Document.md)
- [IAM Integration Guide](../security/IAM_Integration_Guide.md)

---

**Document Status:** Draft  
**Last Updated:** 2025-01  
**Next Review:** 2025-04

