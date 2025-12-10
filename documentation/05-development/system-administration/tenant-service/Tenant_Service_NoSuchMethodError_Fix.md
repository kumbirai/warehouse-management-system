# Tenant Service NoSuchMethodError Fix

## Error Analysis

**Error:**

```
java.lang.NoSuchMethodError: 'void com.ccbsa.wms.tenant.domain.core.entity.Tenant.access$1(com.ccbsa.wms.tenant.domain.core.entity.Tenant, java.lang.Object)'
	at com.ccbsa.wms.tenant.domain.core.entity.Tenant$Builder.tenantId(Tenant.java:292)
```

**Root Cause:**

- Binary incompatibility between compiled classes
- The `Tenant` class was recently modified (changed `resolveSchemaName()` method)
- The running service has old compiled classes that don't match the current source code
- The Builder inner class is trying to access `tenant.setId()` which generates a synthetic accessor method
- The synthetic accessor method signature changed when `resolveSchemaName()` was modified

**Impact:**

- All tenant creation requests return 500 INTERNAL_SERVER_ERROR
- Tests fail with `NoSuchMethodError`

## Solution

The service needs to be fully rebuilt and restarted to ensure all compiled classes are in sync.

### Steps to Fix:

1. **Stop the running tenant-service** (if running via IDE or process manager)

2. **Clean and rebuild the service:**
   ```bash
   cd /home/coach/Dev/Java/warehouse-management-system
   mvn clean install -DskipTests -pl services/tenant-service -am
   ```

3. **Restart the tenant-service**

4. **Verify the fix:**
    - Run the tests again
    - Check that tenant creation returns 201 CREATED instead of 500 INTERNAL_SERVER_ERROR

## Technical Details

### Why This Happens

When an inner class (Builder) accesses a private or protected member of the outer class (Tenant), the Java compiler generates synthetic accessor methods (e.g., `access$1`,
`access$2`). These methods have specific signatures based on the class structure.

When the class structure changes (e.g., adding/removing private methods, changing method signatures), the synthetic accessor methods change, causing binary incompatibility if the
classes aren't recompiled together.

### Prevention

- Always rebuild services after modifying domain classes
- Use `mvn clean install` to ensure all classes are recompiled
- Restart services after code changes
- Consider using build tools that detect class changes automatically

## Related Changes

The `resolveSchemaName()` method in `Tenant.java` was modified to:

- Match the `TenantSchemaResolver` convention (`tenant_{sanitized_tenant_id}_schema`)
- Use proper sanitization logic
- Ensure PostgreSQL identifier compliance

This change affected the class structure, requiring a full rebuild.

