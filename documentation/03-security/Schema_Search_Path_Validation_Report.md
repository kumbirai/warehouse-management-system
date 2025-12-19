# Schema Search Path Pattern Validation Report

## Overview

This report validates that all repository adapters correctly set the PostgreSQL `search_path` before querying tenant-specific data. This is **critical** for the schema-per-tenant
pattern to work correctly.

## Pattern Requirement

**MANDATORY:** All repository methods that query by `tenantId` MUST:

1. Verify `TenantContext` is set
2. Resolve schema name using `TenantSchemaResolver`
3. Set PostgreSQL `search_path` to the resolved schema
4. Then execute the JPA repository query

## Pattern Implementation

```java
@Override
public List<{DomainObject}> findByTenantId(TenantId tenantId) {
    // 1. Verify TenantContext is set (critical for schema resolution)
    TenantId contextTenantId = TenantContext.getTenantId();
    if (contextTenantId == null) {
        throw new IllegalStateException("TenantContext must be set before querying");
    }
    
    // 2. Verify tenantId matches TenantContext
    if (!contextTenantId.getValue().equals(tenantId.getValue())) {
        throw new IllegalStateException("TenantContext tenantId does not match requested tenantId");
    }
    
    // 3. Get the actual schema name from TenantSchemaResolver
    String schemaName = schemaResolver.resolveSchema();
    
    // 4. On-demand safety: ensure schema exists and migrations are applied
    schemaProvisioner.ensureSchemaReady(schemaName);
    
    // 5. Validate schema name format before use
    validateSchemaName(schemaName);
    
    // 6. Set the search_path explicitly on the database connection
    Session session = entityManager.unwrap(Session.class);
    setSearchPath(session, schemaName);
    
    // 7. Now query using JPA repository (will use the schema set in search_path)
    return jpaRepository.findByTenantId(tenantId.getValue())
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
}
```

## Validation Results

### ✅ Services with Correct Pattern

1. **user-service** ✅
    - `UserRepositoryAdapter.findByTenantId()` - ✅ Has schema setting
    - `UserRepositoryAdapter.findByTenantIdAndStatus()` - ✅ Has schema setting
    - `UserRepositoryAdapter.save()` - ✅ Has schema setting

### ❌ Services Missing Pattern

2. **product-service** ❌
    - `ProductRepositoryAdapter.findByTenantId()` - ❌ Missing schema setting
    - `ProductRepositoryAdapter.findByIdAndTenantId()` - ❌ Missing schema setting
    - `ProductRepositoryAdapter.findByProductCodeAndTenantId()` - ❌ Missing schema setting
    - `ProductRepositoryAdapter.save()` - ❌ Missing schema setting

3. **location-management-service** ❌
    - `LocationRepositoryAdapter.findByTenantId()` - ❌ Missing schema setting
    - `LocationRepositoryAdapter.findByIdAndTenantId()` - ❌ Missing schema setting
    - `LocationRepositoryAdapter.save()` - ❌ Missing schema setting

4. **stock-management-service** ❌
    - `StockConsignmentRepositoryAdapter.findByIdAndTenantId()` - ❌ Missing schema setting
    - `StockConsignmentRepositoryAdapter.findByConsignmentReferenceAndTenantId()` - ❌ Missing schema setting
    - `StockConsignmentRepositoryAdapter.save()` - ❌ Missing schema setting

5. **notification-service** ❌
    - `NotificationRepositoryAdapter.findById()` - ❌ Missing schema setting (uses TenantContext but doesn't set search_path)
    - `NotificationRepositoryAdapter.findByRecipientUserId()` - ❌ Missing schema setting
    - `NotificationRepositoryAdapter.findByRecipientUserIdAndStatus()` - ❌ Missing schema setting
    - `NotificationRepositoryAdapter.findByType()` - ❌ Missing schema setting
    - `NotificationRepositoryAdapter.save()` - ❌ Missing schema setting

## Impact

**Critical:** Without setting the `search_path`, queries will execute against the wrong schema (likely the default/public schema), resulting in:

- Empty query results even when data exists
- Data isolation violations
- Incorrect tenant data being returned
- Save operations writing to wrong schema

## Required Dependencies

Repository adapters need:

1. `TenantSchemaResolver` - injected dependency
2. `TenantSchemaProvisioner` - injected dependency
3. `EntityManager` - injected via `@PersistenceContext`
4. `setSearchPath()` helper method
5. `validateSchemaName()` helper method
6. `executeSetSearchPath()` helper method

## Template Update Required

The mandated templates in `documentation/guide/@04-mandated-data-access-templates.md` need to be updated to:

1. Document the schema search path pattern requirement
2. Include the pattern in all `findByTenantId*` method templates
3. Include the pattern in the `save()` method template
4. Include helper methods for schema management

---

**Status:** ❌ **CRITICAL ISSUES FOUND** - Multiple services missing required pattern
**Date:** 2025-12-17
**Action Required:** Fix all repository adapters and update templates

