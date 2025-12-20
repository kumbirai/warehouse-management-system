package com.ccbsa.wms.tenant.domain.core.event;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Base class for all Tenant domain events.
 * <p>
 * All tenant-specific events extend this class. Note: Tenant events do not include tenantId because Tenant IS the tenant.
 * <p>
 * The aggregateId in DomainEvent now stores the tenant ID as a String directly, preserving the original tenant ID value.
 *
 * @param <T> The aggregate root type (TenantId)
 */
public abstract class TenantEvent<T> extends DomainEvent<T> {
    /**
     * Constructor for Tenant events without metadata.
     *
     * @param aggregateId Aggregate identifier (TenantId)
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Tenant events must fail fast if aggregate identifiers are invalid")
    protected TenantEvent(TenantId aggregateId) {
        super(extractTenantIdString(aggregateId), "Tenant");
    }

    /**
     * Extracts the TenantId string value from the aggregate identifier.
     *
     * @param aggregateId Aggregate identifier (TenantId)
     * @return The tenant ID string value
     */
    private static String extractTenantIdString(TenantId aggregateId) {
        if (aggregateId == null) {
            throw new IllegalArgumentException("Aggregate ID cannot be null");
        }
        return aggregateId.getValue();
    }

    /**
     * Constructor for Tenant events with metadata.
     *
     * @param aggregateId Aggregate identifier (TenantId)
     * @param metadata    Event metadata for traceability
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Tenant events must fail fast if aggregate identifiers are invalid")
    protected TenantEvent(TenantId aggregateId, EventMetadata metadata) {
        super(extractTenantIdString(aggregateId), "Tenant", metadata);
    }

    /**
     * Constructor with explicit version (for deserialization).
     *
     * @param aggregateId  Aggregate identifier (TenantId)
     * @param eventVersion Event version
     * @deprecated Use constructor with EventMetadata parameter instead
     */
    @Deprecated
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Tenant events must fail fast if aggregate identifiers are invalid")
    protected TenantEvent(TenantId aggregateId, int eventVersion) {
        super(extractTenantIdString(aggregateId), "Tenant");
        // Note: DomainEvent base class handles versioning
    }

    /**
     * Gets the tenant ID string value from aggregateId. Since aggregateId is now a String, it directly contains the tenant ID.
     *
     * @return The tenant ID string value
     */
    public String getTenantId() {
        return getAggregateId();
    }
}

