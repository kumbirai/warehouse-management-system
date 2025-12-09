# System Administration Documentation

## Overview

This directory contains comprehensive implementation plans for system administration features in the Warehouse Management System (WMS). All plans follow **Domain-Driven Design**, *
*Clean Hexagonal Architecture**, **CQRS**, and **Event-Driven Choreography** principles.

## Schema-Per-Tenant Implementation

All backend microservices (except `tenant-service`) implement the schema-per-tenant pattern for multi-tenant data isolation. Services consume `TenantSchemaCreatedEvent` to
programmatically create tenant schemas and run Flyway migrations.

**Key Documents:**

- [Schema-Per-Tenant Service Implementation Guide](./Schema_Per_Tenant_Service_Implementation_Guide.md) - Step-by-step implementation guide for services
- [Schema-Per-Tenant Implementation Pattern](../01-architecture/Schema_Per_Tenant_Implementation_Pattern.md) - Architectural pattern documentation

**Reference Implementation:**

- `notification-service` - Complete implementation example

---

## Document Structure

### Tenant Management

Located in [`tenant-management/`](tenant-management/)

1. **[Tenant Management Implementation Plan](tenant-management/01-Tenant_Management_Implementation_Plan.md)**
    - Comprehensive overview of tenant management
    - Architecture alignment
    - UI/UX design patterns
    - Data flow architecture
    - Keycloak integration (Single Realm strategy)

2. **[Tenant Creation Plan](tenant-management/02-Tenant_Creation_Plan.md)**
    - Detailed tenant creation workflow
    - Form design and validation
    - Backend implementation
    - Keycloak group creation

3. **[Tenant Activation Plan](tenant-management/03-Tenant_Activation_Plan.md)**
    - Tenant activation workflow
    - Keycloak group enabling
    - Event publishing
    - Error handling

4. **[Tenant Management Plan](tenant-management/04-Tenant_Management_Plan.md)**
    - Tenant list and detail views
    - Status management
    - Configuration updates
    - Query implementations

5. **[Notification Service Integration Plan](tenant-management/05-Notification_Service_Integration_Plan.md)** ⭐ **NEW**
    - Event-driven notifications for tenant lifecycle
    - Kafka event consumption
    - Email template management
    - Retry strategies and error handling

### User Management

Located in [`user-management/`](user-management/) ⭐ **NEW**

1. **[User Management Implementation Plan](user-management/01-User_Management_Implementation_Plan.md)**
    - Comprehensive overview of user management
    - Architecture alignment with tenant management
    - Multi-tenant user management
    - Keycloak integration (Single Realm with tenant attributes)
    - Role-based access control

2. **[User Creation Plan](user-management/02-User_Creation_Plan.md)**
    - Detailed user creation workflow
    - Tenant validation and selection
    - Form design and validation
    - Keycloak user creation with tenant attributes
    - Tenant group assignment

3. **[User Profile Management Plan](user-management/03-User_Profile_Management_Plan.md)**
    - Profile view and edit functionality
    - Data synchronization with Keycloak
    - Permission-based access
    - Update workflows

4. **[User Lifecycle Management Plan](user-management/04-User_Lifecycle_Management_Plan.md)**
    - User activation, deactivation, and suspension
    - Status transitions and business rules
    - Keycloak account enable/disable
    - Event publishing

5. **[User Role Management Plan](user-management/05-User_Role_Management_Plan.md)**
    - Role assignment and removal
    - Available roles (SYSTEM_ADMIN, TENANT_ADMIN, WAREHOUSE_MANAGER, PICKER, USER)
    - Keycloak role synchronization
    - Permission enforcement

---

## Key Architectural Patterns

### 1. Clean Hexagonal Architecture

All implementations follow a layered architecture:

```
Application Layer (REST Controllers)
    ↓
Application Service Layer (Command/Query Handlers)
    ↓
Domain Core (Aggregates, Value Objects, Domain Events)
    ↓
Infrastructure Layer (Repository Adapters, Messaging)
```

### 2. CQRS (Command Query Responsibility Segregation)

- **Commands:** Write operations (Create, Update, Delete)
- **Queries:** Read operations (Get, List, Search)
- Separate controllers for commands and queries
- Separate handlers for commands and queries

### 3. Event-Driven Choreography

- Domain events published to Kafka
- Services react to events asynchronously
- Loose coupling between services
- Event sourcing for audit trail

### 4. Multi-Tenancy

**Single Realm Strategy (Keycloak):**

- All tenants share `wms-realm`
- Tenant differentiation via:
    - `tenant_id` user attribute (primary mechanism)
    - Tenant groups: `tenant-{tenantId}`
- Tenant context propagation via JWT and headers
- Repository-level tenant filtering

### 5. Domain-Driven Design

- **Aggregates:** Tenant, User
- **Value Objects:** TenantId, UserId, Email, Username, TenantStatus, UserStatus
- **Domain Events:** TenantCreatedEvent, UserCreatedEvent, etc.
- **Business Rules:** Encapsulated in domain entities

---

## Implementation Checklist

### Tenant Management

- ✅ Domain core implementation
- ✅ Application service layer
- ✅ REST API controllers
- ✅ Keycloak integration (group management)
- ✅ Frontend components and pages
- ✅ Event publishing
- ⚠️ **NEW:** Notification service integration documented (implementation pending)

### User Management

- ✅ Domain core implementation (User aggregate exists)
- ✅ BFF authentication endpoints
- ⚠️ **MISSING:** User CRUD command handlers (documented, implementation pending)
- ⚠️ **MISSING:** User query handlers (documented, implementation pending)
- ⚠️ **MISSING:** User lifecycle management (documented, implementation pending)
- ⚠️ **MISSING:** Role management endpoints (documented, implementation pending)
- ⚠️ **MISSING:** Frontend components and pages (documented, implementation pending)

---

## Technology Stack

### Backend

- **Framework:** Spring Boot 3.x
- **Language:** Java 17
- **Database:** PostgreSQL (tenant-per-schema or shared schema)
- **Message Broker:** Apache Kafka
- **IAM:** Keycloak (wms-realm)
- **API Gateway:** Spring Cloud Gateway

### Frontend

- **Framework:** React 18 with TypeScript
- **UI Library:** Material-UI (MUI)
- **State Management:** Redux Toolkit
- **Data Fetching:** React Query (TanStack Query)
- **Routing:** React Router v6
- **Form Management:** React Hook Form
- **Validation:** Yup

### Infrastructure

- **Containerization:** Docker
- **Orchestration:** Kubernetes
- **Service Discovery:** Eureka
- **API Gateway:** Spring Cloud Gateway

---

## Security Considerations

### Authentication

- Keycloak OAuth 2.0 / OIDC
- JWT tokens with tenant claims
- Refresh token rotation (httpOnly cookies)

### Authorization

- Role-based access control (RBAC)
- Tenant-based access control
- Permission enforcement at multiple layers:
    - API Gateway
    - Application Service
    - Domain Core

### Multi-Tenancy Enforcement

- Tenant context in JWT
- Tenant context propagation via headers
- Repository-level filtering
- Keycloak tenant attributes and groups

### Data Isolation

- Tenant-aware repositories
- Row-level security
- Tenant validation before operations

---

## Testing Strategy

### Frontend Testing

- **Unit Tests:** Components, hooks, services
- **Integration Tests:** API integration, user flows
- **E2E Tests:** Complete feature flows
- **Tools:** Vitest, React Testing Library, Playwright

### Backend Testing

- **Unit Tests:** Domain logic, business rules
- **Integration Tests:** API endpoints, repositories
- **Security Tests:** Permission enforcement, tenant isolation
- **Tools:** JUnit 5, Mockito, TestContainers

---

## Next Steps

### Immediate (Week 1-2)

1. Implement User CRUD command handlers
2. Implement User query handlers
3. Implement frontend user management components
4. Set up basic notification service structure

### Short-term (Week 3-4)

1. Implement user lifecycle management
2. Implement role management
3. Implement notification service event listeners
4. Complete frontend user management pages

### Medium-term (Month 2)

1. Complete notification service with all channels
2. Implement in-app notifications
3. Add SMS notification support
4. Comprehensive security testing

### Long-term (Month 3+)

1. Advanced reporting and analytics
2. Audit log UI
3. Bulk user operations
4. Advanced tenant configuration options

---

## Related Documentation

- [Service Architecture Document](../../01-architecture/Service_Architecture_Document.md)
- [Frontend Architecture Document](../../01-architecture/Frontend_Architecture_Document.md)
- [API Specifications](../../02-api/API_Specifications.md)
- [Security Architecture Document](../../01-architecture/Security_Architecture_Document.md)
- [IAM Integration Guide](../../03-security/IAM_Integration_Guide.md)
- [Multi-Tenancy Enforcement Guide](../../03-security/Multi_Tenancy_Enforcement_Guide.md)
- [Mandated Implementation Template Guide](../../guide/mandated-Implementation-template-guide.md)

---

## Contributing

When adding new implementation plans:

1. Follow the existing document structure
2. Include complete data flows
3. Provide frontend and backend implementation details
4. Document Keycloak integration
5. Include testing strategies
6. Update this README with links

---

**Last Updated:** 2025-12-04
**Maintained By:** Development Team
