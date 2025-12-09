# Mandated Container Templates

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Approved

---

## Overview

Templates for the **Container** module (`{service}-container`). Bootstraps application and manages configuration.

---

## Package Structure

The Container module (`{service}-container`) follows a strict package structure for application bootstrap and configuration:

```
com.ccbsa.wms.{service}.container/
├── {Service}Application.java          # Main application class
├── config/                           # Configuration classes
│   ├── {Service}Configuration.java
│   ├── DatabaseConfig.java
│   ├── KafkaConfig.java
│   └── SecurityConfig.java
└── health/                           # Health indicators (optional)
    └── {Component}HealthIndicator.java
```

**Package Naming Convention:**

- Base package: `com.ccbsa.wms.{service}.container`
- Replace `{service}` with actual service name (e.g., `stock`, `location`, `product`)
- Main application class: `{Service}Application` (e.g., `StockManagementApplication`)

**Package Responsibilities:**

| Package  | Responsibility        | Contains                                                                                      |
|----------|-----------------------|-----------------------------------------------------------------------------------------------|
| Root     | Application bootstrap | Main application class with `@SpringBootApplication`, `@EnableJpaRepositories`, `@EntityScan` |
| `config` | Configuration classes | Spring configuration classes for database, Kafka, security, annotated with `@Configuration`   |
| `health` | Health indicators     | Custom health indicators for monitoring, implements `HealthIndicator`                         |

**Important Package Rules:**

- **Bootstrap**: Main application class bootstraps Spring Boot application
- **Configuration**: All configuration classes in `config` package
- **Dependency injection**: Configuration classes wire infrastructure components
- **Multi-tenant**: Configuration includes tenant-aware resolvers
- **Monitoring**: Health indicators expose service health status

---

## Important Domain Driven Design, Clean Hexagonal Architecture principles, CQRS and Event Driven Design Notes

**Domain-Driven Design (DDD) Principles:**

- Container layer bootstraps application and wires components
- No business logic in container layer
- Configuration adapts infrastructure to application needs

**Clean Hexagonal Architecture Principles:**

- Container layer is the outermost infrastructure layer
- Bootstraps all adapters and wires them to ports
- Dependency injection configured here
- Infrastructure concerns isolated to this layer

**CQRS Principles:**

- Container configures both command and query infrastructure
- Separate data sources can be configured for read/write
- Event publishing infrastructure configured here

**Event-Driven Design Principles:**

- Kafka configuration in container layer
- Event publisher beans configured here
- Event listener beans configured here
- Dead letter queue configuration here
- Correlation context interceptors configured here for traceability

---

## Application Bootstrap Template

```java
package com.ccbsa.wms.{service}.container;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.ccbsa.wms.{service}.dataaccess.jpa")
@EntityScan(basePackages = "com.ccbsa.wms.{service}.dataaccess.entity")
public class {Service}Application {
    
    public static void main(String[] args) {
        SpringApplication.run({Service}Application.class, args);
    }
}
```

## Configuration Template

```java
package com.ccbsa.wms.{service}.container.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.ccbsa.wms.common.security.ServiceSecurityConfig;
import com.ccbsa.wms.{service}.config.RequestLoggingInterceptor;

@Configuration
@Import(ServiceSecurityConfig.class)
public class {Service}Configuration implements WebMvcConfigurer {
    
    private final RequestLoggingInterceptor requestLoggingInterceptor;
    
    public {Service}Configuration(RequestLoggingInterceptor requestLoggingInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Request logging interceptor extracts correlation ID from headers
        // and sets it in CorrelationContext and MDC
        registry.addInterceptor(requestLoggingInterceptor);
    }
}
```

**Note:** The `RequestLoggingInterceptor` automatically:

- Extracts `X-Correlation-Id` header from incoming requests
- Generates new correlation ID if header is missing
- Sets correlation ID in `CorrelationContext` (ThreadLocal)
- Sets correlation ID in MDC for logging
- Clears correlation context after request completion

## Traceability Requirements

**Correlation Context Configuration:**

1. **Request Interceptor**: Configure `RequestLoggingInterceptor` to extract correlation ID
2. **Context Management**: Interceptor sets correlation ID in `CorrelationContext` for request lifecycle
3. **Logging Integration**: Correlation ID automatically included in MDC for log correlation
4. **Cleanup**: Correlation context cleared after request completion

**Implementation Checklist:**

- [ ] Request interceptor registered in WebMvcConfigurer
- [ ] Interceptor extracts X-Correlation-Id header
- [ ] Interceptor sets correlation ID in CorrelationContext
- [ ] Interceptor clears correlation context after request

---

**Document Control**

- **Version History:** v1.0 (2025-01) - Initial template creation
- **Review Cycle:** Review when container patterns change

