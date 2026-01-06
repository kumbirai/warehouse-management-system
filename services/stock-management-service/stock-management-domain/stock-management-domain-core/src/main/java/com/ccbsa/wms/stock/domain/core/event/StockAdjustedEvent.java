package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.AdjustmentReason;
import com.ccbsa.common.domain.valueobject.AdjustmentType;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.entity.StockAdjustment;
import com.ccbsa.wms.stock.domain.core.valueobject.Notes;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAdjustmentId;

/**
 * Domain Event: StockAdjustedEvent
 * <p>
 * Published when stock is successfully adjusted.
 * <p>
 * This event indicates that:
 * - Stock level has been adjusted (increased or decreased)
 * - Used for updating stock item quantities
 * - May trigger location capacity updates
 */
public class StockAdjustedEvent extends StockManagementEvent<StockAdjustment> {

    private final StockAdjustmentId adjustmentId;
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final StockItemId stockItemId;
    private final AdjustmentType adjustmentType;
    private final Quantity quantity;
    private final Quantity quantityBefore;
    private final Quantity quantityAfter;
    private final AdjustmentReason reason;
    private final Notes notes;

    /**
     * Constructor for StockAdjustedEvent.
     *
     * @param adjustmentId   Adjustment identifier
     * @param tenantId       Tenant identifier
     * @param productId      Product identifier
     * @param locationId     Location identifier (optional)
     * @param stockItemId    Stock item identifier (optional)
     * @param adjustmentType Adjustment type (INCREASE or DECREASE)
     * @param quantity       Adjustment quantity
     * @param quantityBefore Quantity before adjustment
     * @param quantityAfter  Quantity after adjustment
     * @param reason         Adjustment reason
     * @param notes          Adjustment notes
     */
    public StockAdjustedEvent(StockAdjustmentId adjustmentId, TenantId tenantId, ProductId productId, LocationId locationId, StockItemId stockItemId, AdjustmentType adjustmentType,
                              Quantity quantity, Quantity quantityBefore, Quantity quantityAfter, AdjustmentReason reason, Notes notes) {
        super(adjustmentId.getValueAsString(), "StockAdjustment");
        this.adjustmentId = adjustmentId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.stockItemId = stockItemId;
        this.adjustmentType = adjustmentType;
        this.quantity = quantity;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.reason = reason;
        this.notes = notes;
    }

    /**
     * Constructor for StockAdjustedEvent with metadata.
     *
     * @param adjustmentId   Adjustment identifier
     * @param tenantId       Tenant identifier
     * @param productId      Product identifier
     * @param locationId     Location identifier (optional)
     * @param stockItemId    Stock item identifier (optional)
     * @param adjustmentType Adjustment type (INCREASE or DECREASE)
     * @param quantity       Adjustment quantity
     * @param quantityBefore Quantity before adjustment
     * @param quantityAfter  Quantity after adjustment
     * @param reason         Adjustment reason
     * @param notes          Adjustment notes
     * @param metadata       Event metadata (correlation ID, user ID, etc.)
     */
    public StockAdjustedEvent(StockAdjustmentId adjustmentId, TenantId tenantId, ProductId productId, LocationId locationId, StockItemId stockItemId, AdjustmentType adjustmentType,
                              Quantity quantity, Quantity quantityBefore, Quantity quantityAfter, AdjustmentReason reason, Notes notes, EventMetadata metadata) {
        super(adjustmentId.getValueAsString(), "StockAdjustment", metadata);
        this.adjustmentId = adjustmentId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.stockItemId = stockItemId;
        this.adjustmentType = adjustmentType;
        this.quantity = quantity;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.reason = reason;
        this.notes = notes;
    }

    public StockAdjustmentId getAdjustmentId() {
        return adjustmentId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public StockItemId getStockItemId() {
        return stockItemId;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Quantity getQuantityBefore() {
        return quantityBefore;
    }

    public Quantity getQuantityAfter() {
        return quantityAfter;
    }

    public AdjustmentReason getReason() {
        return reason;
    }

    public Notes getNotes() {
        return notes;
    }
}

