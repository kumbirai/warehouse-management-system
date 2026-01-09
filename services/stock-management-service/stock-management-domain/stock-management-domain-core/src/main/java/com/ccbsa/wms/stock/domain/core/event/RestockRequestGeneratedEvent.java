package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.common.domain.valueobject.BigDecimalQuantity;
import com.ccbsa.common.domain.valueobject.MaximumQuantity;
import com.ccbsa.common.domain.valueobject.MinimumQuantity;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.RestockPriority;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.entity.RestockRequest;

/**
 * Domain Event: RestockRequestGeneratedEvent
 * <p>
 * Published when a restock request is generated.
 * <p>
 * This event indicates that:
 * - Stock level has fallen below minimum threshold
 * - A restock request has been created
 * - May trigger D365 integration if enabled
 */
public class RestockRequestGeneratedEvent extends StockManagementEvent<RestockRequest> {
    private static final String AGGREGATE_TYPE = "RestockRequest";

    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final BigDecimalQuantity currentQuantity;
    private final MinimumQuantity minimumQuantity;
    private final MaximumQuantity maximumQuantity;
    private final BigDecimalQuantity requestedQuantity;
    private final RestockPriority priority;

    /**
     * Constructor for RestockRequestGeneratedEvent.
     *
     * @param restockRequestId  Restock request ID (as String)
     * @param tenantId          Tenant identifier
     * @param productId         Product identifier
     * @param locationId        Location identifier (optional)
     * @param currentQuantity   Current stock quantity
     * @param minimumQuantity   Minimum threshold quantity
     * @param maximumQuantity   Maximum threshold quantity (optional)
     * @param requestedQuantity Requested restock quantity
     * @param priority          Restock priority
     */
    public RestockRequestGeneratedEvent(String restockRequestId, TenantId tenantId, ProductId productId, LocationId locationId, BigDecimalQuantity currentQuantity,
                                        MinimumQuantity minimumQuantity, MaximumQuantity maximumQuantity, BigDecimalQuantity requestedQuantity, RestockPriority priority) {
        super(restockRequestId, AGGREGATE_TYPE);
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.currentQuantity = currentQuantity;
        this.minimumQuantity = minimumQuantity;
        this.maximumQuantity = maximumQuantity;
        this.requestedQuantity = requestedQuantity;
        this.priority = priority;
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

    public BigDecimalQuantity getCurrentQuantity() {
        return currentQuantity;
    }

    public MinimumQuantity getMinimumQuantity() {
        return minimumQuantity;
    }

    public MaximumQuantity getMaximumQuantity() {
        return maximumQuantity;
    }

    public BigDecimalQuantity getRequestedQuantity() {
        return requestedQuantity;
    }

    public RestockPriority getPriority() {
        return priority;
    }
}
