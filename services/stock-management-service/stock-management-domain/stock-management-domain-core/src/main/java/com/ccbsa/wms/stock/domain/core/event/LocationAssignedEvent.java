package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.Quantity;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

/**
 * Domain Event: LocationAssignedEvent
 * <p>
 * Published when a location is assigned to a stock item.
 * <p>
 * This event indicates that:
 * - Stock item has been assigned to a location
 * - Location Management Service should update location status and capacity
 * - Used for event-driven choreography between services
 */
public class LocationAssignedEvent extends StockManagementEvent<StockItem> {
    private static final String AGGREGATE_TYPE = "StockItem";

    private final StockItemId stockItemId;
    private final ProductId productId;
    private final LocationId locationId;
    private final Quantity quantity;
    private final ExpirationDate expirationDate;
    private final StockClassification classification;

    /**
     * Constructor for LocationAssignedEvent.
     *
     * @param stockItemId    Stock item identifier
     * @param productId      Product identifier
     * @param locationId     Location identifier
     * @param quantity       Quantity assigned
     * @param expirationDate Expiration date (may be null for non-perishable)
     * @param classification Stock classification
     */
    public LocationAssignedEvent(StockItemId stockItemId, ProductId productId, LocationId locationId, Quantity quantity, ExpirationDate expirationDate,
                                 StockClassification classification) {
        super(stockItemId.getValueAsString(), AGGREGATE_TYPE);
        this.stockItemId = stockItemId;
        this.productId = productId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
        this.classification = classification;
    }

    public StockItemId getStockItemId() {
        return stockItemId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public ExpirationDate getExpirationDate() {
        return expirationDate;
    }

    public StockClassification getClassification() {
        return classification;
    }
}

