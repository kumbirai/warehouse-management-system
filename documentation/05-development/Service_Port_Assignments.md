# Service Port Assignments

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Draft

---

## Port Assignments

### Infrastructure Services

| Service             | Port | Description                        |
|---------------------|------|------------------------------------|
| PostgreSQL          | 5432 | Main application database          |
| PostgreSQL Keycloak | 5433 | Keycloak database                  |
| Zookeeper           | 2181 | Kafka coordination                 |
| Kafka               | 9092 | Message broker                     |
| Redis               | 6379 | Rate limiting and caching          |
| Keycloak            | 7080 | Identity and Access Management     |
| Eureka Server       | 8761 | Service discovery and registration |

### Application Services

| Service                     | Port | Description                                   |
|-----------------------------|------|-----------------------------------------------|
| Gateway Service             | 8080 | API Gateway (entry point)                     |
| Stock Management Service    | 8081 | Stock consignment and management              |
| Location Management Service | 8082 | Warehouse location management                 |
| Product Service             | 8083 | Product catalog management                    |
| Picking Service             | 8084 | Picking operations                            |
| Returns Service             | 8085 | Returns processing                            |
| Reconciliation Service      | 8086 | Stock reconciliation                          |
| Integration Service         | 8087 | External system integration                   |
| User Service                | 8088 | User management and IAM                       |
| Tenant Service              | 8089 | Tenant lifecycle and configuration management |
| Notification Service        | 8090 | Notification management and delivery          |

---

## Access URLs

### Development Environment

- **Eureka Dashboard:** http://localhost:8761
- **Keycloak Admin Console:** http://localhost:7080
- **Gateway Service:** http://localhost:8080
- **Stock Management Service:** http://localhost:8081
- **Location Management Service:** http://localhost:8082
- **Product Service:** http://localhost:8083
- **Picking Service:** http://localhost:8084
- **Returns Service:** http://localhost:8085
- **Reconciliation Service:** http://localhost:8086
- **Integration Service:** http://localhost:8087
- **User Service:** http://localhost:8088
- **Tenant Service:** http://localhost:8089
- **Notification Service:** http://localhost:8090

### API Endpoints

All API requests should go through the Gateway Service:

- **Base URL:** http://localhost:8080
- **BFF Authentication:** http://localhost:8080/api/v1/bff/**
- **Stock Management:** http://localhost:8080/api/v1/stock-management/**
- **Location Management:** http://localhost:8080/api/v1/location-management/**
- **Products:** http://localhost:8080/api/v1/products/**
- **Picking:** http://localhost:8080/api/v1/picking/**
- **Returns:** http://localhost:8080/api/v1/returns/**
- **Reconciliation:** http://localhost:8080/api/v1/reconciliation/**
- **Integration:** http://localhost:8080/api/v1/integration/**
- **Tenants:** http://localhost:8080/api/v1/tenants/**
- **Notifications:** http://localhost:8080/api/v1/notifications/**

---

## Configuration

### Keycloak Configuration

All services reference Keycloak at:

- **Issuer URI:** http://localhost:7080/realms/wms-realm
- **JWK Set URI:** http://localhost:7080/realms/wms-realm/protocol/openid-connect/certs

### Service Discovery

The system uses **Spring Cloud Netflix Eureka** for service discovery and registration. All microservices register with the Eureka server and the gateway discovers services
dynamically.

**Eureka Server:**

- **URL:** http://localhost:8761
- **Dashboard:** http://localhost:8761 (web UI for viewing registered services)
- **Default Zone:** http://localhost:8761/eureka/

**Service Registration:**
All services automatically register with Eureka on startup. Services are identified by their `spring.application.name`:

- `gateway-service`
- `stock-management-service`
- `location-management-service`
- `product-service`
- `picking-service`
- `returns-service`
- `reconciliation-service`
- `integration-service`
- `user-service`
- `tenant-service`
- `notification-service`

**Gateway Routing:**
The gateway routes requests using Eureka service discovery with load balancing:

- `lb://stock-management-service`
- `lb://location-management-service`
- `lb://product-service`
- `lb://picking-service`
- `lb://returns-service`
- `lb://reconciliation-service`
- `lb://integration-service`
- `lb://user-service`
- `lb://tenant-service`
- `lb://notification-service`

**Eureka Client Configuration:**
All services include the following Eureka client configuration:

```yaml
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

**Benefits:**

- **Dynamic Service Discovery:** Services automatically register and deregister
- **Load Balancing:** Multiple instances of the same service are automatically load-balanced
- **Health Monitoring:** Eureka tracks service health and removes unhealthy instances
- **Scalability:** Easy to add more service instances without configuration changes

---

## Port Conflicts

If any port is already in use:

1. **Check port usage:**
   ```bash
   # Linux/Mac
   lsof -i :8080
   # Windows
   netstat -ano | findstr :8080
   ```

2. **Update application.yml:**
   Change the `server.port` value in the service's `application.yml` file.

3. **Update docker-compose.dev.yml:**
   For infrastructure services, update the port mapping in `infrastructure/docker/docker-compose.dev.yml`.

---

**Document Status:** Draft  
**Last Updated:** 2025-01

