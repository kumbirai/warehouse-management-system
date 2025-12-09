# Warehouse Management System Integration

## CCBSA LDP System

**Version:** 1.0.0-SNAPSHOT  
**Java Version:** 21  
**Spring Boot Version:** 3.2.0

## Overview

The Warehouse Management System Integration serves as a bridge between Microsoft Dynamics 365 Finance and Operations (D365) and Local Distribution Partner (LDP) warehouse
operations. The system enables LDPs to manage consigned stock, optimize warehouse operations, maintain accurate inventory levels, and ensure seamless reconciliation with D365.

## Architecture

This system follows:

- **Domain-Driven Design (DDD)** - Business logic drives architecture decisions
- **Clean Hexagonal Architecture** - Clear separation of concerns with domain at center
- **CQRS** - Command Query Responsibility Segregation
- **Event-Driven Choreography** - Loose coupling through domain events
- **Microservices** - Independently deployable services

## Project Structure

```
warehouse-application-system/
├── common/                          # Common modules
│   ├── common-domain/              # Common domain base classes
│   ├── common-messaging/           # Common messaging infrastructure
│   ├── common-application/         # Common application utilities
│   ├── common-infrastructure/      # Common infrastructure utilities
│   ├── common-keycloak/            # Keycloak integration utilities
│   └── common-security/            # Security utilities
├── services/                        # Microservices
│   ├── eureka-server/              # Service discovery server
│   ├── gateway-service/            # API Gateway
│   ├── tenant-service/             # Tenant management service
│   ├── user-service/               # User management service (BFF)
│   ├── notification-service/       # Notification management and delivery
│   ├── stock-management-service/
│   ├── location-management-service/
│   ├── product-service/
│   ├── picking-service/
│   ├── returns-service/
│   ├── reconciliation-service/
│   └── integration-service/
├── frontend-app/                    # Frontend PWA application
├── infrastructure/                  # Infrastructure as code
│   ├── docker/                     # Docker configurations
│   ├── kubernetes/                 # Kubernetes manifests
│   └── terraform/                  # Terraform configurations
└── scripts/                         # Utility scripts
```

## Prerequisites

- **Java 21** - JDK 21 or later
- **Maven 3.8+** - Build tool
- **Docker & Docker Compose** - Containerization
- **PostgreSQL 15+** - Database
- **Keycloak** - Identity and Access Management (IAM) server
- **Node.js 18+** - Frontend development (optional)
- **Git** - Version control

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/kumbirai/warehouse-application-system.git
cd warehouse-application-system
```

### 2. Set Up Environment

```bash
cp .env.example .env
# Edit .env with your configuration
```

### 3. Start Infrastructure Services

```bash
./scripts/start-services.sh
```

### 4. Build Project

```bash
mvn clean install
```

### 5. Run Database Migrations

```bash
mvn flyway:migrate -pl services/*-service
```

### 6. Start Services

Each service can be started individually:

```bash
cd services/stock-management-service/stock-management-container
mvn spring-boot:run
```

## Development

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Skip quality checks (for faster builds during development)
mvn clean install -Pskip-quality-checks
```

### Code Quality

The project uses multiple code quality tools:

- **Checkstyle** - Code style checking
- **PMD** - Code analysis
- **SpotBugs** - Bug detection
- **JaCoCo** - Code coverage (minimum 80%)

Run quality checks:

```bash
mvn checkstyle:check
mvn pmd:check
mvn spotbugs:check
```

## Documentation

### Business Requirements

- [Business Requirements Document](documentation/00-business-requirements/business-requirements-document.md)

### Architecture

- [Service Architecture](documentation/01-architecture/Service_Architecture_Document.md)
- [Domain Model Design](documentation/01-architecture/Domain_Model_Design.md)
- [Frontend Architecture](documentation/01-architecture/Frontend_Architecture_Document.md)
- [Security Architecture](documentation/01-architecture/Security_Architecture_Document.md)

### API Specifications

- [API Specifications](documentation/02-api/API_Specifications.md)

### Security

- [IAM Integration Guide](documentation/03-security/IAM_Integration_Guide.md)
- [Keycloak Client Secret Setup](documentation/03-security/Keycloak_Client_Secret_Setup.md)
- [Multi-Tenancy Enforcement Guide](documentation/03-security/Multi_Tenancy_Enforcement_Guide.md)
- [Security Implementation Summary](documentation/03-security/Security_Implementation_Summary.md)

### Integration

- [Gateway Service Specification](documentation/04-integration/Gateway_Service_Specification.md)
- [Service Integration Guide](documentation/04-integration/Service_Integration_Guide.md)

### Development

- [Development Environment Setup](documentation/05-development/Development_Environment_Setup.md)
- [Clean Code Guidelines](documentation/05-development/clean-code-guidelines-per-module.md)
- [Service Port Assignments](documentation/05-development/Service_Port_Assignments.md)

### Implementation Templates

- [Implementation Template Guide](documentation/guide/mandated-Implementation-template-guide.md)

## Contributing

1. Follow the coding standards defined in [Clean Code Guidelines](documentation/05-development/clean-code-guidelines-per-module.md)
2. Ensure all tests pass
3. Run code quality checks before committing
4. Follow the implementation templates in [Implementation Template Guide](documentation/guide/mandated-Implementation-template-guide.md)
5. Adhere to Domain-Driven Design (DDD), Clean Hexagonal Architecture, CQRS, and Event-Driven Design principles as defined
   in [Service Architecture Document](documentation/01-architecture/Service_Architecture_Document.md)

## License

Copyright © 2025 CCBSA. All rights reserved.

