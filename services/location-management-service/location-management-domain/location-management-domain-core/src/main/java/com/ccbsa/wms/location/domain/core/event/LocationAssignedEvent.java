package com.ccbsa.wms.location.domain.core.event;

import java.math.BigDecimal;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Domain Event: LocationAssignedEvent
 * <p>
 * Published when stock is assigned to a location.
 * <p>
 * This event indicates that:
 * - Stock item has been assigned to this location
 * - Location capacity and status have been updated
 * - Used for event-driven choreography with Stock Management Service
 */
public class LocationAssignedEvent extends LocationManagementEvent {

    private final LocationId locationId;
    private final TenantId tenantId;
    private final String stockItemId; // Cross-service reference (String)
    private final BigDecimal quantity;

    /**
     * Constructor for LocationAssignedEvent.
     *
     * @param locationId  Location identifier
     * @param tenantId    Tenant identifier
     * @param stockItemId Stock item identifier (as String, cross-service reference)
     * @param quantity    Quantity assigned
     */
    public LocationAssignedEvent(LocationId locationId, TenantId tenantId, String stockItemId, BigDecimal quantity) {
        super(locationId);
        this.locationId = locationId;
        this.tenantId = tenantId;
        this.stockItemId = stockItemId;
        this.quantity = quantity;
    }

    /**
     * Constructor for LocationAssignedEvent with metadata.
     *
     * @param locationId  Location identifier
     * @param tenantId    Tenant identifier
     * @param stockItemId Stock item identifier (as String, cross-service reference)
     * @param quantity    Quantity assigned
     * @param metadata    Event metadata (correlation ID, user ID, etc.)
     */
    public LocationAssignedEvent(LocationId locationId, TenantId tenantId, String stockItemId, BigDecimal quantity, EventMetadata metadata) {
        super(locationId, metadata);
        this.locationId = locationId;
        this.tenantId = tenantId;
        this.stockItemId = stockItemId;
        this.quantity = quantity;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getStockItemId() {
        return stockItemId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}

