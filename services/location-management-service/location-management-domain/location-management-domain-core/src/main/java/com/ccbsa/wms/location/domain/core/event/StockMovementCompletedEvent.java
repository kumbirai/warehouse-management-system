package com.ccbsa.wms.location.domain.core.event;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.MovementReason;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.MovementType;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

/**
 * Domain Event: StockMovementCompletedEvent
 * <p>
 * Published when a stock movement is completed.
 * <p>
 * This event indicates that:
 * - A stock movement has been successfully completed
 * - Stock has been moved from source to destination location
 * - Used for updating stock item locations and location capacities
 * - May trigger downstream processes (e.g., picking task creation)
 */
public class StockMovementCompletedEvent extends LocationManagementEvent {

    private final StockMovementId movementId;
    private final TenantId tenantId;
    private final String stockItemId; // Cross-service reference (StockItemId as String)
    private final ProductId productId;
    private final LocationId sourceLocationId;
    private final LocationId destinationLocationId;
    private final Quantity quantity;
    private final MovementType movementType;
    private final MovementReason reason;
    private final UserId initiatedBy;
    private final LocalDateTime initiatedAt;
    private final UserId completedBy;
    private final LocalDateTime completedAt;

    /**
     * Constructor for StockMovementCompletedEvent.
     *
     * @param movementId            Movement identifier
     * @param tenantId              Tenant identifier
     * @param stockItemId           Stock item identifier
     * @param productId             Product identifier
     * @param sourceLocationId      Source location identifier
     * @param destinationLocationId Destination location identifier
     * @param quantity              Movement quantity
     * @param movementType          Movement type
     * @param reason                Movement reason
     * @param initiatedBy           User who initiated the movement
     * @param initiatedAt           Initiation timestamp
     * @param completedBy           User who completed the movement
     * @param completedAt           Completion timestamp
     */
    public StockMovementCompletedEvent(StockMovementId movementId, TenantId tenantId, String stockItemId, ProductId productId, LocationId sourceLocationId,
                                       LocationId destinationLocationId, Quantity quantity, MovementType movementType, MovementReason reason, UserId initiatedBy,
                                       LocalDateTime initiatedAt, UserId completedBy, LocalDateTime completedAt) {
        super(destinationLocationId); // Use destination location as aggregate ID for event routing
        this.movementId = movementId;
        this.tenantId = tenantId;
        this.stockItemId = stockItemId;
        this.productId = productId;
        this.sourceLocationId = sourceLocationId;
        this.destinationLocationId = destinationLocationId;
        this.quantity = quantity;
        this.movementType = movementType;
        this.reason = reason;
        this.initiatedBy = initiatedBy;
        this.initiatedAt = initiatedAt;
        this.completedBy = completedBy;
        this.completedAt = completedAt;
    }

    /**
     * Constructor for StockMovementCompletedEvent with metadata.
     *
     * @param movementId            Movement identifier
     * @param tenantId              Tenant identifier
     * @param stockItemId           Stock item identifier
     * @param productId             Product identifier
     * @param sourceLocationId      Source location identifier
     * @param destinationLocationId Destination location identifier
     * @param quantity              Movement quantity
     * @param movementType          Movement type
     * @param reason                Movement reason
     * @param initiatedBy           User who initiated the movement
     * @param initiatedAt           Initiation timestamp
     * @param completedBy           User who completed the movement
     * @param completedAt           Completion timestamp
     * @param metadata              Event metadata (correlation ID, user ID, etc.)
     */
    public StockMovementCompletedEvent(StockMovementId movementId, TenantId tenantId, String stockItemId, ProductId productId, LocationId sourceLocationId,
                                       LocationId destinationLocationId, Quantity quantity, MovementType movementType, MovementReason reason, UserId initiatedBy,
                                       LocalDateTime initiatedAt, UserId completedBy, LocalDateTime completedAt, EventMetadata metadata) {
        super(destinationLocationId, metadata);
        this.movementId = movementId;
        this.tenantId = tenantId;
        this.stockItemId = stockItemId;
        this.productId = productId;
        this.sourceLocationId = sourceLocationId;
        this.destinationLocationId = destinationLocationId;
        this.quantity = quantity;
        this.movementType = movementType;
        this.reason = reason;
        this.initiatedBy = initiatedBy;
        this.initiatedAt = initiatedAt;
        this.completedBy = completedBy;
        this.completedAt = completedAt;
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

    public ProductId getProductId() {
        return productId;
    }

    public LocationId getSourceLocationId() {
        return sourceLocationId;
    }

    public LocationId getDestinationLocationId() {
        return destinationLocationId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public MovementReason getReason() {
        return reason;
    }

    public UserId getInitiatedBy() {
        return initiatedBy;
    }

    public LocalDateTime getInitiatedAt() {
        return initiatedAt;
    }

    public UserId getCompletedBy() {
        return completedBy;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}

