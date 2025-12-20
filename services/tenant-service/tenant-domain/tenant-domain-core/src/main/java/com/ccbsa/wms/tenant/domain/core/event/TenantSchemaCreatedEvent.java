package com.ccbsa.wms.tenant.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Domain Event: TenantSchemaCreatedEvent
 * <p>
 * Published when a tenant schema is created (for schema-per-tenant isolation).
 */
public final class TenantSchemaCreatedEvent extends TenantEvent<TenantId> {
    private final String schemaName;

    public TenantSchemaCreatedEvent(TenantId tenantId, String schemaName) {
        super(tenantId);
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }
        this.schemaName = schemaName.trim();
    }

    public TenantSchemaCreatedEvent(TenantId tenantId, String schemaName, EventMetadata metadata) {
        super(tenantId, metadata);
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }
        this.schemaName = schemaName.trim();
    }

    public String getSchemaName() {
        return schemaName;
    }
}

