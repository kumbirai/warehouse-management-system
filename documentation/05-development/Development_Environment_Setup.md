# Development Environment Setup

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-11  
**Status:** Draft  
**Related Documents:**

- [Service Architecture Document](../architecture/Service_Architecture_Document.md)
- [Clean Code Guidelines](clean-code-guidelines-per-module.md)
- [Mandated Implementation Template Guide](../../guide/mandated-Implementation-template-guide.md)
- [Project Roadmap](../project-management/project-roadmap.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Project Structure](#project-structure)
3. [Development Tools](#development-tools)
4. [CI/CD Pipeline](#cicd-pipeline)
5. [Local Development Setup](#local-development-setup)
6. [Database Setup](#database-setup)
7. [Docker Setup](#docker-setup)
8. [IDE Configuration](#ide-configuration)

---

## Overview

### Purpose

This document provides comprehensive guidance for setting up the development environment for the Warehouse Management System Integration project. It covers project structure,
development tools, CI/CD pipeline configuration, and local development setup.

### Prerequisites

- **Java 21** - JDK 21 or later
- **Maven 3.8+** - Build tool
- **Docker & Docker Compose** - Containerization
- **PostgreSQL 15+** - Database
- **Node.js 18+** - Frontend development
- **Git** - Version control
- **IDE** - IntelliJ IDEA (recommended) or Eclipse

---

## Project Structure

### Root Project Structure

```
warehouse-application-system/
├── pom.xml                          # Root POM (parent)
├── README.md                        # Project README
├── .gitignore                       # Git ignore rules
├── .editorconfig                    # Editor configuration
├── documentation/                   # Project documentation
│   ├── 00-business-requiremants/
│   └── 01-project-planning/
├── common/                          # Common modules
│   ├── common-domain/              # Common domain base classes
│   ├── common-messaging/           # Common messaging infrastructure
│   ├── common-application/         # Common application utilities
│   ├── common-infrastructure/      # Common infrastructure utilities
│   └── common-security/            # Common security utilities (tenant context, interceptors)
├── services/                        # Microservices
│   ├── gateway-service/            # API Gateway (routing, security, rate limiting)
│   ├── user-service/               # User Management Service (IAM integration)
│   ├── tenant-service/             # Tenant Management Service (tenant lifecycle and configuration)
│   ├── notification-service/        # Notification Service (notification management and delivery)
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
    ├── setup-dev-env.sh
    ├── start-services.sh
    └── run-tests.sh
```

### Service Module Structure

Each service follows the standard module structure:

```
{service}-service/
├── pom.xml                          # Service parent POM
├── README.md                        # Service README
├── {service}-application/          # REST API layer (CQRS controllers)
│   ├── pom.xml
│   └── src/main/java/com/ccbsa/wms/{service}/application/
│       ├── command/                # Command controllers
│       ├── query/                  # Query controllers
│       ├── dto/                   # DTOs and mappers
│       └── exception/             # Exception handlers
├── {service}-container/            # Application bootstrap
│   ├── pom.xml
│   └── src/main/java/com/ccbsa/wms/{service}/container/
│       ├── config/                # Configuration classes
│       ├── health/                # Health checks
│       └── {Service}Application.java
├── {service}-dataaccess/           # Database access layer
│   ├── pom.xml
│   └── src/main/java/com/ccbsa/wms/{service}/dataaccess/
│       ├── adapter/               # Repository adapters
│       ├── entity/                # JPA entities
│       ├── mapper/                # Entity mappers
│       └── cache/                 # Cache decorators
├── {service}-domain/               # Domain layer
│   ├── pom.xml
│   ├── {service}-application-service/  # Application services
│   │   └── src/main/java/com/ccbsa/wms/{service}/application/service/
│   │       ├── command/           # Command handlers
│   │       ├── query/            # Query handlers
│   │       └── port/             # Port interfaces
│   └── {service}-domain-core/         # Core domain
│       └── src/main/java/com/ccbsa/wms/{service}/domain/core/
│           ├── entity/           # Domain entities
│           ├── valueobject/       # Value objects
│           ├── event/            # Domain events
│           └── exception/         # Domain exceptions
└── {service}-messaging/            # Event messaging layer
    ├── pom.xml
    └── src/main/java/com/ccbsa/wms/{service}/messaging/
        ├── publisher/             # Event publishers
        ├── listener/              # Event listeners
        └── config/                # Kafka configuration
```

### Common Modules Structure

```
common/
├── common-domain/                  # Common domain base classes
│   ├── pom.xml
│   └── src/main/java/com/ccbsa/common/domain/
│       ├── AggregateRoot.java
│       ├── TenantAwareAggregateRoot.java
│       ├── DomainEvent.java
│       └── valueobject/          # Common value objects
├── common-messaging/               # Common messaging infrastructure
│   ├── pom.xml
│   └── src/main/java/com/ccbsa/common/messaging/
│       ├── EventPublisher.java
│       ├── EventConsumer.java
│       └── config/                # Kafka configuration
├── common-application/             # Common application utilities
│   ├── pom.xml
│   └── src/main/java/com/ccbsa/common/application/
│       ├── command/              # Base command classes
│       ├── query/                # Base query classes
│       └── exception/            # Common exceptions
└── common-infrastructure/          # Common infrastructure utilities
    ├── pom.xml
    └── src/main/java/com/ccbsa/common/infrastructure/
        ├── config/               # Common configuration
        ├── security/             # Security utilities
        └── monitoring/           # Monitoring utilities
```

### Frontend Structure

```
frontend-app/
├── package.json                    # NPM dependencies
├── tsconfig.json                   # TypeScript configuration
├── vite.config.ts                  # Vite configuration
├── .eslintrc.js                    # ESLint configuration
├── .prettierrc                     # Prettier configuration
├── public/                         # Static assets
│   ├── manifest.json              # PWA manifest
│   ├── sw.js                      # Service worker (generated)
│   └── locales/                   # Translation files
├── src/
│   ├── components/                # Reusable components
│   ├── features/                  # Feature modules
│   ├── hooks/                     # Custom hooks
│   ├── services/                  # API services
│   ├── store/                     # Redux store
│   ├── types/                     # TypeScript types
│   ├── utils/                     # Utility functions
│   └── App.tsx                    # Root component
└── tests/                         # Test files
```

---

## Development Tools

### Build Tool: Maven

**Root POM Configuration:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.ccbsa.wms</groupId>
    <artifactId>warehouse-application-system</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>3.2.0</spring-boot.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <kafka.version>3.6.0</kafka.version>
        <postgresql.version>42.7.1</postgresql.version>
    </properties>
    
    <modules>
        <module>common/common-domain</module>
        <module>common/common-messaging</module>
        <module>common/common-application</module>
        <module>common/common-infrastructure</module>
        <module>services/stock-management-service</module>
        <module>services/location-management-service</module>
        <module>services/product-service</module>
        <module>services/picking-service</module>
        <module>services/returns-service</module>
        <module>services/reconciliation-service</module>
        <module>services/integration-service</module>
    </modules>
    
    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Spring Cloud BOM -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### Code Quality Tools

#### Checkstyle

**Configuration:** `.checkstyle.xml`

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
    <module name="TreeWalker">
        <module name="ConstantName"/>
        <module name="LocalFinalVariableName"/>
        <module name="LocalVariableName"/>
        <module name="MemberName"/>
        <module name="MethodName"/>
        <module name="PackageName"/>
        <module name="ParameterName"/>
        <module name="StaticVariableName"/>
        <module name="TypeName"/>
        <module name="AvoidStarImport"/>
        <module name="IllegalImport"/>
        <module name="RedundantImport"/>
        <module name="UnusedImports"/>
        <module name="LineLength">
            <property name="max" value="120"/>
        </module>
    </module>
</module>
```

#### PMD

**Configuration:** `.pmd.xml`

```xml
<?xml version="1.0"?>
<ruleset name="CCBSA WMS Rules"
         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0
         https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
    <description>PMD rules for CCBSA WMS</description>
    <rule ref="category/java/bestpractices.xml"/>
    <rule ref="category/java/codestyle.xml"/>
    <rule ref="category/java/design.xml"/>
    <rule ref="category/java/errorprone.xml"/>
    <rule ref="category/java/performance.xml"/>
</ruleset>
```

#### SpotBugs

**Configuration:** `spotbugs-exclude.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
    <!-- Exclude JPA entities from certain checks -->
    <Match>
        <Class name="~.*Entity"/>
        <Bug pattern="EI_EXPOSE_REP"/>
    </Match>
</FindBugsFilter>
```

### Testing Frameworks

#### Unit Testing

- **JUnit 5** - Test framework
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions

#### Integration Testing

- **Spring Boot Test** - Integration testing
- **Testcontainers** - Docker-based testing
- **WireMock** - API mocking

#### Test Configuration

**Maven Surefire Plugin:**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.2</version>
    <configuration>
        <includes>
            <include>**/*Test.java</include>
            <include>**/*Tests.java</include>
        </includes>
    </configuration>
</plugin>
```

---

## CI/CD Pipeline

### GitHub Actions Workflow

**File:** `.github/workflows/ci.yml`

```yaml
name: CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: secret
          POSTGRES_DB: test_db
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
      
      kafka:
        image: confluentinc/cp-kafka:latest
        env:
          KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
        ports:
          - 9092:9092
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      
      - name: Run Checkstyle
        run: mvn checkstyle:check
      
      - name: Run PMD
        run: mvn pmd:check
      
      - name: Run SpotBugs
        run: mvn spotbugs:check
      
      - name: Run Tests
        run: mvn test
      
      - name: Build
        run: mvn clean package -DskipTests
      
      - name: Upload Test Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: '**/target/surefire-reports/*.xml'
```

### Code Quality Gates

**SonarQube Configuration:** `sonar-project.properties`

```properties
sonar.projectKey=ccbsa-wms
sonar.projectName=Warehouse Management System
sonar.projectVersion=1.0.0
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.coveragePlugin=jacoco
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
sonar.exclusions=**/dto/**,**/entity/**,**/config/**
```

---

## Local Development Setup

### Prerequisites Installation

**macOS (Homebrew):**

```bash
brew install openjdk@21
brew install maven
brew install postgresql@15
brew install docker docker-compose
brew install node@18
```

**Linux (Ubuntu/Debian):**

```bash
sudo apt update
sudo apt install openjdk-21-jdk maven postgresql-15 docker.io docker-compose nodejs npm
```

**Windows:**

- Install Java 21 from Oracle or Adoptium
- Install Maven from Apache Maven website
- Install PostgreSQL 15 from PostgreSQL website
- Install Docker Desktop
- Install Node.js 18 from Node.js website

### Environment Variables

**File:** `.env.example`

```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=wms_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=secret

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_GROUP_ID=wms-service-group

# D365 Integration
D365_BASE_URL=https://api.d365.example.com
D365_CLIENT_ID=your-client-id
D365_CLIENT_SECRET=your-client-secret
D365_TENANT_ID=your-tenant-id

# Application
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
```

### Setup Script

**File:** `scripts/setup-dev-env.sh`

```bash
#!/bin/bash

echo "Setting up development environment..."

# Create .env file from example
if [ ! -f .env ]; then
    cp .env.example .env
    echo "Created .env file. Please update with your values."
fi

# Start Docker services
echo "Starting Docker services..."
docker-compose up -d postgres kafka zookeeper

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 10

# Run database migrations
echo "Running database migrations..."
mvn flyway:migrate -pl services/*-service

# Build project
echo "Building project..."
mvn clean install -DskipTests

echo "Development environment setup complete!"
```

---

## Database Setup

### PostgreSQL Configuration

**Docker Compose:** `infrastructure/docker/docker-compose.yml`

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    container_name: wms-postgres
    environment:
      POSTGRES_DB: wms_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: secret
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

### Database Schema Setup

**Flyway Migration:** `services/{service}-dataaccess/src/main/resources/db/migration/V1__Initial_schema.sql`

```sql
-- Create schema per tenant (example for stock-management-service)
CREATE SCHEMA IF NOT EXISTS stock_management;

-- Create tables
CREATE TABLE stock_management.stock_consignments (
    id UUID PRIMARY KEY,
    consignment_reference VARCHAR(50) NOT NULL UNIQUE,
    tenant_id VARCHAR(50) NOT NULL,
    warehouse_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stock_consignments_tenant ON stock_management.stock_consignments(tenant_id);
CREATE INDEX idx_stock_consignments_status ON stock_management.stock_consignments(status);
```

### Multi-Tenant Schema Strategy

**Schema per Tenant:**

- Each tenant (LDP) has its own schema
- Schema name: `{tenant_id}_schema`
- Dynamic schema resolution in application

**Configuration:**

```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    public TenantSchemaResolver tenantSchemaResolver() {
        return new TenantSchemaResolver();
    }
}
```

---

## Docker Setup

### Docker Compose for Local Development

**File:** `infrastructure/docker/docker-compose.dev.yml`

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    container_name: wms-postgres-dev
    environment:
      POSTGRES_DB: wms_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: secret
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    container_name: wms-zookeeper-dev
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    container_name: wms-kafka-dev
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: wms-redis-dev
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

### Start Services Script

**File:** `scripts/start-services.sh`

```bash
#!/bin/bash

echo "Starting development services..."

cd infrastructure/docker
docker-compose -f docker-compose.dev.yml up -d

echo "Services started. Waiting for health checks..."
sleep 15

echo "Services are ready!"
echo "PostgreSQL: localhost:5432"
echo "Kafka: localhost:9092"
echo "Redis: localhost:6379"
```

---

## IDE Configuration

### IntelliJ IDEA Configuration

#### Project Settings

1. **Java SDK:** Set to Java 21
2. **Maven:** Use bundled Maven or external Maven 3.8+
3. **Code Style:** Import `.editorconfig` file
4. **Inspections:** Enable all Java inspections

#### Plugins

- **Checkstyle-IDEA** - Checkstyle integration
- **SonarLint** - Code quality analysis
- **Lombok** - Lombok support (for DTOs only, not domain core)
- **Database Navigator** - Database tooling

#### Run Configurations

**Application Run Configuration:**

- Main class: `com.ccbsa.wms.{service}.container.{Service}Application`
- VM options: `-Dspring.profiles.active=dev`
- Environment variables: Load from `.env` file

**Test Configuration:**

- Test runner: JUnit 5
- Coverage: JaCoCo

### VS Code Configuration

**File:** `.vscode/settings.json`

```json
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.compile.nullAnalysis.mode": "automatic",
    "java.format.settings.url": ".vscode/java-formatter.xml",
    "editor.formatOnSave": true,
    "editor.codeActionsOnSave": {
        "source.organizeImports": true
    }
}
```

---

## Summary

### Setup Checklist

- [ ] Install prerequisites (Java 21, Maven, PostgreSQL, Docker, Node.js)
- [ ] Clone repository
- [ ] Copy `.env.example` to `.env` and configure
- [ ] Start Docker services (`docker-compose up -d`)
- [ ] Run database migrations (`mvn flyway:migrate`)
- [ ] Build project (`mvn clean install`)
- [ ] Configure IDE (IntelliJ IDEA or VS Code)
- [ ] Run tests (`mvn test`)
- [ ] Start services locally

### Next Steps

1. Set up project structure following this guide
2. Initialize common modules
3. Create first service (Stock Management Service)
4. Set up CI/CD pipeline
5. Begin Sprint 1 development

---

**Document Control**

- **Version History:** This document will be version controlled with change tracking
- **Review Cycle:** This document will be reviewed when environment changes
- **Distribution:** This document will be distributed to all development team members

