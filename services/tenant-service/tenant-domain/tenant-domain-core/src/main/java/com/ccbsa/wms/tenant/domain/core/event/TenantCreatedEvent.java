package com.ccbsa.wms.tenant.domain.core.event;

import java.util.Optional;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantName;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantStatus;

/**
 * Domain Event: TenantCreatedEvent
 * <p>
 * Published when a new tenant is created.
 */
public final class TenantCreatedEvent
        extends TenantEvent<TenantId> {
    private final TenantName name;
    private final TenantStatus status;
    private final EmailAddress email;

    public TenantCreatedEvent(TenantId tenantId, TenantName name, TenantStatus status, EmailAddress email) {
        super(tenantId);
        if (name == null) {
            throw new IllegalArgumentException("Tenant name cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Tenant status cannot be null");
        }
        this.name = name;
        this.status = status;
        this.email = email; // Email is optional, can be null
    }

    public TenantCreatedEvent(TenantId tenantId, TenantName name, TenantStatus status, EmailAddress email, EventMetadata metadata) {
        super(tenantId, metadata);
        if (name == null) {
            throw new IllegalArgumentException("Tenant name cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Tenant status cannot be null");
        }
        this.name = name;
        this.status = status;
        this.email = email; // Email is optional, can be null
    }

    public TenantName getName() {
        return name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    /**
     * Gets the email address of the tenant.
     *
     * @return Optional containing the email address, or empty if not provided
     */
    public Optional<EmailAddress> getEmail() {
        return Optional.ofNullable(email);
    }
}

