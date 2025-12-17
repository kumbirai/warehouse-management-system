package com.ccbsa.wms.user.domain.core.event;

import java.util.Objects;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Base class for all User domain events.
 * <p>
 * All user-specific events extend this class. Includes tenantId for tenant-aware aggregates.
 */
public abstract class UserEvent
        extends DomainEvent<UserId> {
    private final TenantId tenantId;

    /**
     * Constructor for User events without metadata.
     *
     * @param aggregateId Aggregate identifier (UserId)
     * @param tenantId    Tenant identifier
     * @throws IllegalArgumentException if tenantId is null
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "User events must fail fast if aggregate identifiers are invalid")
    protected UserEvent(UserId aggregateId, TenantId tenantId) {
        super(extractUserIdString(aggregateId), "User");
        this.tenantId = Objects.requireNonNull(tenantId, "TenantId cannot be null for user events");
    }

    /**
     * Extracts the UserId string value from the aggregate identifier.
     *
     * @param userId User identifier
     * @return String representation of user ID
     */
    private static String extractUserIdString(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        return userId.getValue();
    }

    /**
     * Constructor for User events with metadata.
     *
     * @param aggregateId Aggregate identifier (UserId)
     * @param tenantId    Tenant identifier
     * @param metadata    Event metadata for traceability
     * @throws IllegalArgumentException if tenantId is null
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "User events must fail fast if aggregate identifiers are invalid")
    protected UserEvent(UserId aggregateId, TenantId tenantId, EventMetadata metadata) {
        super(extractUserIdString(aggregateId), "User", metadata);
        this.tenantId = Objects.requireNonNull(tenantId, "TenantId cannot be null for user events");
    }

    public TenantId getTenantId() {
        return tenantId;
    }
}

