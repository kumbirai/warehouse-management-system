package com.ccbsa.wms.location.domain.core.event;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

/**
 * Domain Event: StockMovementCancelledEvent
 * <p>
 * Published when a stock movement is cancelled.
 * <p>
 * This event indicates that:
 * - A stock movement has been cancelled before completion
 * - Used for tracking cancelled movements and audit purposes
 * - May trigger cleanup processes
 */
public class StockMovementCancelledEvent extends LocationManagementEvent {

    private final StockMovementId movementId;
    private final TenantId tenantId;
    private final String stockItemId; // Cross-service reference (StockItemId as String)
    private final LocationId sourceLocationId;
    private final LocationId destinationLocationId;
    private final UserId cancelledBy;
    private final LocalDateTime cancelledAt;
    private final String cancellationReason;

    /**
     * Constructor for StockMovementCancelledEvent.
     *
     * @param movementId            Movement identifier
     * @param tenantId              Tenant identifier
     * @param stockItemId           Stock item identifier
     * @param sourceLocationId      Source location identifier
     * @param destinationLocationId Destination location identifier
     * @param cancelledBy           User who cancelled the movement
     * @param cancelledAt           Cancellation timestamp
     * @param cancellationReason    Reason for cancellation
     */
    public StockMovementCancelledEvent(StockMovementId movementId, TenantId tenantId, String stockItemId, LocationId sourceLocationId, LocationId destinationLocationId,
                                       UserId cancelledBy, LocalDateTime cancelledAt, String cancellationReason) {
        super(sourceLocationId); // Use source location as aggregate ID for event routing
        this.movementId = movementId;
        this.tenantId = tenantId;
        this.stockItemId = stockItemId;
        this.sourceLocationId = sourceLocationId;
        this.destinationLocationId = destinationLocationId;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
        this.cancellationReason = cancellationReason;
    }

    /**
     * Constructor for StockMovementCancelledEvent with metadata.
     *
     * @param movementId            Movement identifier
     * @param tenantId              Tenant identifier
     * @param stockItemId           Stock item identifier
     * @param sourceLocationId      Source location identifier
     * @param destinationLocationId Destination location identifier
     * @param cancelledBy           User who cancelled the movement
     * @param cancelledAt           Cancellation timestamp
     * @param cancellationReason    Reason for cancellation
     * @param metadata              Event metadata (correlation ID, user ID, etc.)
     */
    public StockMovementCancelledEvent(StockMovementId movementId, TenantId tenantId, String stockItemId, LocationId sourceLocationId, LocationId destinationLocationId,
                                       UserId cancelledBy, LocalDateTime cancelledAt, String cancellationReason, EventMetadata metadata) {
        super(sourceLocationId, metadata);
        this.movementId = movementId;
        this.tenantId = tenantId;
        this.stockItemId = stockItemId;
        this.sourceLocationId = sourceLocationId;
        this.destinationLocationId = destinationLocationId;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
        this.cancellationReason = cancellationReason;
    }

    public StockMovementId getMovementId() {
        return movementId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getStockItemId() {
        return stockItemId;
    }

    public LocationId getSourceLocationId() {
        return sourceLocationId;
    }

    public LocationId getDestinationLocationId() {
        return destinationLocationId;
    }

    public UserId getCancelledBy() {
        return cancelledBy;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }
}

