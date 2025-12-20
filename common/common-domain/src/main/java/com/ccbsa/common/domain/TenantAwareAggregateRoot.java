package com.ccbsa.common.domain;

import com.ccbsa.common.domain.valueobject.TenantId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Base class for aggregate roots that are tenant-aware. Tenant-aware aggregates belong to a specific tenant (LDP) and must include tenant ID.
 *
 * @param <ID> The type of the aggregate identifier
 */
public abstract class TenantAwareAggregateRoot<ID> extends AggregateRoot<ID> {
    protected TenantId tenantId;

    /**
     * Protected no-arg constructor for builder pattern and reflection-based construction. Subclasses should use the constructor that requires TenantId when possible. The tenantId
     * must be set via setTenantId() before the aggregate is used.
     */
    protected TenantAwareAggregateRoot() {
        super();
    }

    /**
     * Protected constructor that ensures TenantId is always set. This is the preferred constructor for subclasses.
     *
     * @param tenantId The tenant identifier (must not be null)
     * @throws IllegalArgumentException if tenantId is null
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Tenant-aware aggregates must fail fast when tenant context is missing; "
            + "the aggregate instance is not published if validation fails.")
    protected TenantAwareAggregateRoot(TenantId tenantId) {
        this(validateTenantId(tenantId), true);
    }

    private TenantAwareAggregateRoot(TenantId tenantId, boolean validated) {
        super();
        this.tenantId = tenantId;
    }

    private static TenantId validateTenantId(TenantId tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null for tenant-aware aggregates");
        }
        return tenantId;
    }

    /**
     * Returns the tenant ID. Validates that tenantId is set before returning.
     *
     * @return The tenant identifier
     * @throws IllegalStateException if tenantId has not been set
     */
    public TenantId getTenantId() {
        if (tenantId == null) {
            throw new IllegalStateException("TenantId has not been initialized for this tenant-aware aggregate");
        }
        return tenantId;
    }

    /**
     * Sets the tenant ID with validation. This method ensures tenantId is never null.
     *
     * @param tenantId The tenant identifier (must not be null)
     * @throws IllegalArgumentException if tenantId is null
     */
    protected void setTenantId(TenantId tenantId) {
        this.tenantId = validateTenantId(tenantId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        TenantAwareAggregateRoot<?> that = (TenantAwareAggregateRoot<?>) o;
        return tenantId != null && tenantId.equals(that.tenantId);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
        return result;
    }
}

