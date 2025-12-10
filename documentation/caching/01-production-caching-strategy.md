# Production-Grade Caching Strategy

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0
**Date:** 2025-12-09
**Status:** Approved
**Classification:** Mandatory Implementation Standard

---

## Executive Summary

This document defines the **mandatory** production-grade distributed caching strategy for the Warehouse Management System (WMS). Caching is **not optional** and must be implemented across all services to ensure:

- **Performance**: Sub-100ms response times for frequently accessed data
- **Scalability**: Reduced database load supporting 10,000+ concurrent users
- **Availability**: Circuit breaker pattern with cache fallback
- **Multi-tenancy**: Tenant-isolated cache namespaces
- **Consistency**: Event-driven cache invalidation across service boundaries

**Architecture Alignment:**

This caching strategy is designed to maintain strict adherence to:

- ✅ **Domain-Driven Design (DDD)** - Cache as infrastructure concern, transparent to domain
- ✅ **Clean Hexagonal Architecture** - Cache adapters implement repository ports
- ✅ **CQRS** - Separate caching strategies for write and read models
- ✅ **Event-Driven Choreography** - Cache invalidation via domain events
- ✅ **Multi-tenant Isolation** - Schema-per-tenant extended to cache-per-tenant

---

## Table of Contents

1. [Overview](#overview)
2. [Architectural Principles](#architectural-principles)
3. [Technology Stack](#technology-stack)
4. [Common Module Infrastructure](#common-module-infrastructure)
5. [Cache Key Strategy](#cache-key-strategy)
6. [Cache Invalidation Strategy](#cache-invalidation-strategy)
7. [Repository Adapter Pattern](#repository-adapter-pattern)
8. [Cache Configuration](#cache-configuration)
9. [Multi-Tenant Caching](#multi-tenant-caching)
10. [Cache Warming Strategy](#cache-warming-strategy)
11. [Monitoring and Observability](#monitoring-and-observability)
12. [Implementation Roadmap](#implementation-roadmap)
13. [Testing Strategy](#testing-strategy)
14. [References](#references)

---

## 1. Overview

### 1.1 Purpose

This document establishes the mandatory caching infrastructure and patterns for the WMS. All services MUST implement distributed caching to meet production-grade performance requirements.

### 1.2 Scope

**In Scope:**
- Distributed caching infrastructure (Redis)
- Cache abstraction layer in common modules
- Tenant-aware cache key generation
- Event-driven cache invalidation
- Repository adapter caching decorators
- Cache warming strategies
- Monitoring and observability

**Out of Scope:**
- HTTP response caching (handled by API Gateway)
- Browser caching (frontend concern)
- CDN caching (infrastructure layer)
- Query result caching (JPA second-level cache - explicitly excluded)

### 1.3 Document Audience

- **Software Architects** - System design and infrastructure decisions
- **Backend Developers** - Repository adapter and service implementation
- **DevOps Engineers** - Redis cluster deployment and monitoring
- **QA Engineers** - Cache-related test scenarios

---

## 2. Architectural Principles

### 2.1 Domain-Driven Design Principles

**Cache as Infrastructure Concern:**

```
┌─────────────────────────────────────────────────────┐
│ Domain Layer (Pure Business Logic)                  │
│ - Aggregates, Entities, Value Objects               │
│ - Domain Events                                      │
│ - NO cache awareness                                 │
└─────────────────────────────────────────────────────┘
                         ▲
                         │ Implements Port
                         │
┌─────────────────────────────────────────────────────┐
│ Application Service Layer                           │
│ - Repository Ports (interfaces)                     │
│ - Command/Query Handlers                            │
│ - NO cache implementation                           │
└─────────────────────────────────────────────────────┘
                         ▲
                         │ Implements Port
                         │
┌─────────────────────────────────────────────────────┐
│ Infrastructure Layer (Data Access)                  │
│ - CachedRepositoryAdapter (Decorator Pattern)       │
│   ├── Cache Read/Write                              │
│   ├── Cache Miss → Delegate to JPA Repository       │
│   └── Cache Invalidation Event Listeners            │
│ - RepositoryAdapter (JPA Implementation)            │
│ - JPA Entities and Mappers                          │
└─────────────────────────────────────────────────────┘
```

**Key Principles:**

- ✅ Domain entities are **cache-agnostic** - no cache annotations or logic
- ✅ Application service layer works with **port interfaces** - unaware of caching
- ✅ Infrastructure adapters handle caching **transparently** via decorator pattern
- ✅ Cache invalidation driven by **domain events** - maintains domain purity

### 2.2 Clean Hexagonal Architecture Principles

**Dependency Direction:**

```
Application Service → Port Interface ← Cached Adapter → Base Adapter → Database
                                            ↓
                                        Redis Cache
```

**Adapter Hierarchy:**

1. **Port Interface** (`UserRepository`) - Defined in application service layer
2. **Cached Adapter** (`CachedUserRepositoryAdapter`) - Implements port, delegates to base
3. **Base Adapter** (`UserRepositoryAdapter`) - JPA implementation
4. **Cache Manager** - Injected into cached adapter, managed by Spring

**Benefits:**

- ✅ **Testability** - Mock cache layer independently
- ✅ **Replaceability** - Swap Redis for Hazelcast without domain changes
- ✅ **Composability** - Chain decorators (cache → circuit breaker → metrics)

### 2.3 CQRS Principles

**Separate Caching Strategies for Write and Read Models:**

| Aspect | Write Model (Commands) | Read Model (Queries) |
|--------|------------------------|----------------------|
| **Cache Strategy** | Write-through cache | Read-through cache with TTL |
| **Invalidation** | Immediate on write | Event-driven eventual consistency |
| **TTL** | Short (5-15 minutes) | Longer (30-60 minutes) |
| **Cache Keys** | Aggregate ID based | Query parameter based |
| **Consistency** | Strong (write-through) | Eventual (event-driven) |
| **Example** | `tenant:123:user:uuid` | `tenant:123:users:active:page:0` |

**Write Model Caching:**

```java
@Override
public void save(User user) {
    // 1. Persist to database (source of truth)
    User savedUser = baseRepository.save(user);

    // 2. Write-through to cache
    cacheManager.put(
        generateCacheKey(user.getTenantId(), user.getId()),
        savedUser
    );

    // 3. Publish domain event (invalidates read model caches)
    eventPublisher.publish(new UserUpdatedEvent(user));
}
```

**Read Model Caching:**

```java
@Override
public List<UserView> findActiveUsersByTenant(TenantId tenantId) {
    String cacheKey = String.format("tenant:%s:users:active", tenantId.getValue());

    // 1. Check cache first
    List<UserView> cached = cacheManager.get(cacheKey, List.class);
    if (cached != null) {
        return cached;
    }

    // 2. Cache miss - query database
    List<UserView> users = viewRepository.findActiveUsersByTenant(tenantId);

    // 3. Populate cache with TTL
    cacheManager.put(cacheKey, users, Duration.ofMinutes(30));

    return users;
}
```

### 2.4 Event-Driven Choreography Principles

**Cache Invalidation via Domain Events:**

```
┌──────────────────────────────────────────────────────────────┐
│ Service A: User Service                                      │
│                                                               │
│ UserCommandHandler.updateUser()                              │
│   ├── Update user aggregate                                  │
│   ├── userRepository.save(user) → Write-through cache        │
│   └── eventPublisher.publish(UserUpdatedEvent)               │
│                         │                                     │
└─────────────────────────┼─────────────────────────────────────┘
                          │
                          ▼ Kafka Topic: user-events
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Service A    │  │ Service B    │  │ Service C    │
│ (Self-invalidate) │ (Cross-service) │ (Cross-service) │
│              │  │              │  │              │
│ Invalidates: │  │ Invalidates: │  │ Invalidates: │
│ - Write cache│  │ - Read cache │  │ - Denorm data│
│ - Read cache │  │   (if subscribed) │   (if relevant)  │
└──────────────┘  └──────────────┘  └──────────────┘
```

**Event-Driven Invalidation Pattern:**

1. **Domain Event Published** - Command handler publishes event after successful write
2. **Local Invalidation** - Same service invalidates its own caches
3. **Distributed Invalidation** - Other services listen and invalidate stale caches
4. **Eventual Consistency** - Read models eventually consistent with write model

**Benefits:**

- ✅ **Decoupling** - Services don't directly call each other for invalidation
- ✅ **Scalability** - Asynchronous invalidation doesn't block writes
- ✅ **Resilience** - Failed invalidation retried via Kafka consumer groups
- ✅ **Auditability** - Event log provides invalidation history

### 2.5 Multi-Tenant Isolation Principles

**Cache Namespace Isolation:**

```
Redis Keyspace Structure:
├── tenant:tenant_a:user:uuid_1 → User aggregate (Tenant A)
├── tenant:tenant_a:users:active → User list cache (Tenant A)
├── tenant:tenant_b:user:uuid_2 → User aggregate (Tenant B)
├── tenant:tenant_b:users:active → User list cache (Tenant B)
└── global:tenant:tenant_a → Tenant metadata (cross-tenant)
```

**Isolation Rules:**

- ✅ **Tenant prefix mandatory** - All cache keys MUST include `tenant:{tenantId}:` prefix
- ✅ **Global namespace reserved** - Only for cross-tenant admin operations
- ✅ **Schema alignment** - Cache namespace matches PostgreSQL schema isolation
- ✅ **Access control** - Cache key generation validates tenant context

---

## 3. Technology Stack

### 3.1 Core Technologies

| Component | Technology | Version | Justification |
|-----------|-----------|---------|---------------|
| **Cache Store** | Redis | 7.x | Industry-standard, high-performance, supports advanced data structures |
| **Client Library** | Spring Data Redis | 3.x | Native Spring Boot integration, connection pooling |
| **Cache Abstraction** | Spring Cache | 6.x | Declarative caching, provider-agnostic |
| **Serialization** | Jackson JSON | 2.15+ | Existing dependency, type-safe, human-readable |
| **Connection Pool** | Lettuce | 6.x | Default Spring Data Redis client, async/reactive support |

### 3.2 Redis Deployment Architecture

**Production Deployment: Redis Cluster with Sentinel**

```
┌─────────────────────────────────────────────────────────────┐
│ Redis Cluster (High Availability)                           │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Redis Master │  │ Redis Master │  │ Redis Master │      │
│  │ Shard 1      │  │ Shard 2      │  │ Shard 3      │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                 │                 │               │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐      │
│  │ Redis Replica│  │ Redis Replica│  │ Redis Replica│      │
│  │ Shard 1      │  │ Shard 2      │  │ Shard 3      │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                              │
│  ┌────────────────────────────────────────────────┐         │
│  │ Redis Sentinel (3 nodes)                       │         │
│  │ - Health monitoring                            │         │
│  │ - Automatic failover                           │         │
│  │ - Configuration provider                       │         │
│  └────────────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

**Configuration:**

- **3 Master Shards** - Data partitioned by key hash
- **3 Replicas** - One replica per master for failover
- **3 Sentinel Nodes** - Quorum-based master election
- **Persistence** - AOF (Append-Only File) + RDB snapshots
- **Eviction Policy** - `allkeys-lru` (Least Recently Used)

### 3.3 Development Environment

**Docker Compose Configuration** (already deployed):

```yaml
redis:
  image: redis:7-alpine
  container_name: wms-redis-dev
  ports:
    - "6379:6379"
  networks:
    - wms-dev-network
  restart: unless-stopped
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
```

**Status:** ✅ Already deployed, ready for integration

---
