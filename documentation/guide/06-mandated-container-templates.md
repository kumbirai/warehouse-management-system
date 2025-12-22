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

## WebMvcConfig Template (REST API ObjectMapper Configuration)

**CRITICAL**: All services MUST configure ObjectMapper separation for production-grade implementation.

```java
package com.ccbsa.wms.{service}.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @NonNull
    private final RequestLoggingInterceptor requestLoggingInterceptor;

    public WebMvcConfig(@NonNull RequestLoggingInterceptor requestLoggingInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**",
                        "/error",
                        "/swagger-ui/**",
                        "/v3/api-docs/**");
    }

    /**
     * Configures Jackson ObjectMapper for REST API responses.
     * <p>
     * PRODUCTION-GRADE DESIGN: This configuration is explicitly scoped for REST API use only.
     * It does NOT include type information, which is required for Kafka but should NOT be
     * present in REST API responses consumed by frontend clients.
     * <p>
     * Ensures LocalDateTime is serialized as ISO-8601 strings (not timestamps).
     * This is important for frontend compatibility and human readability.
     */
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .modules(new JavaTimeModule())
                .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .postConfigurer(objectMapper -> {
                    // Disable default typing which adds type information to JSON
                    // This ensures REST API responses are clean JSON without @class or array wrappers
                    objectMapper.setDefaultTyping(null);
                });
    }

    /**
     * Primary ObjectMapper bean for REST API HTTP message conversion.
     * <p>
     * PRODUCTION-GRADE DESIGN: This ObjectMapper is marked as @Primary ONLY for HTTP message conversion.
     * This is the default ObjectMapper used by Spring Boot's MappingJackson2HttpMessageConverter
     * for REST API serialization/deserialization.
     * <p>
     * SEPARATION OF CONCERNS:
     * - This ObjectMapper (@Primary): Used by Spring MVC for HTTP message conversion (REST API)
     * - "kafkaObjectMapper" bean: Used explicitly by Kafka components (with type information)
     * - "redisCacheObjectMapper" bean: Used explicitly by Redis cache (with type information)
     * <p>
     * The @Primary annotation here is NOT a workaround - it's a design choice to designate
     * the default ObjectMapper for HTTP. All other ObjectMappers are explicitly named and
     * injected using @Qualifier, ensuring clear separation of concerns.
     * <p>
     * Built from Jackson2ObjectMapperBuilder to ensure proper configuration without type information.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();
        // Explicitly ensure no type information (redundant but defensive)
        mapper.setDefaultTyping(null);
        return mapper;
    }
}
```

**ObjectMapper Separation Requirements:**

1. **REST API ObjectMapper** (`@Primary`):
    - Built from `Jackson2ObjectMapperBuilder`
    - NO type information included
    - Used automatically by Spring MVC for HTTP message conversion

2. **Kafka ObjectMapper** (`kafkaObjectMapper`):
    - Provided by `KafkaConfig` in `common-messaging` module
    - Includes type information (@class property) for polymorphic event deserialization
    - Must be imported via `@Import(KafkaConfig.class)` in service configuration
    - Must be injected with `@Qualifier("kafkaObjectMapper")` in all Kafka-related beans

3. **Redis Cache ObjectMapper** (`redisCacheObjectMapper`):
    - Provided by `CacheConfiguration` in `common-cache` module
    - Includes type information for polymorphic cache value deserialization
    - Must be injected with `@Qualifier("redisCacheObjectMapper")` in cache-related beans

**See**: `documentation/05-development/ObjectMapper_Separation_Strategy.md` for complete details.

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

