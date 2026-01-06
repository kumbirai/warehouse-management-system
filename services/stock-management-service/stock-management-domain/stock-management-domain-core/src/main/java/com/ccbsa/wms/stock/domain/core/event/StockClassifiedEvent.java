package com.ccbsa.wms.stock.domain.core.event;

import java.time.LocalDate;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;

/**
 * Domain Event: StockClassifiedEvent
 * <p>
 * Published when stock item classification changes.
 * <p>
 * This event indicates that:
 * - Stock item has been classified or reclassified
 * - Classification changed from old to new value
 * - Triggers FEFO location assignment in Location Management Service
 */
public class StockClassifiedEvent extends StockManagementEvent<StockItem> {
    private static final String AGGREGATE_TYPE = "StockItem";

    private final ProductId productId;
    private final TenantId tenantId;
    private final StockClassification oldClassification;
    private final StockClassification newClassification;
    private final LocalDate expirationDate;
    private final Quantity quantity;

    /**
     * Constructor for StockClassifiedEvent.
     *
     * @param aggregateId       Stock item ID (as String)
     * @param productId         Product identifier
     * @param tenantId          Tenant identifier
     * @param oldClassification Previous classification (may be null for initial classification)
     * @param newClassification New classification
     * @param expirationDate    Expiration date (may be null for non-perishable)
     * @param quantity          Stock item quantity (required for FEFO location assignment)
     */
    public StockClassifiedEvent(String aggregateId, ProductId productId, TenantId tenantId, StockClassification oldClassification, StockClassification newClassification,
                                LocalDate expirationDate, Quantity quantity) {
        super(aggregateId, AGGREGATE_TYPE);
        this.productId = productId;
        this.tenantId = tenantId;
        this.oldClassification = oldClassification;
        this.newClassification = newClassification;
        this.expirationDate = expirationDate;
        this.quantity = quantity;
    }

    /**
     * Constructor for StockClassifiedEvent with metadata.
     *
     * @param aggregateId       Stock item ID (as String)
     * @param productId         Product identifier
     * @param tenantId          Tenant identifier
     * @param oldClassification Previous classification (may be null for initial classification)
     * @param newClassification New classification
     * @param expirationDate    Expiration date (may be null for non-perishable)
     * @param quantity          Stock item quantity (required for FEFO location assignment)
     * @param metadata          Event metadata for traceability
     */
    public StockClassifiedEvent(String aggregateId, ProductId productId, TenantId tenantId, StockClassification oldClassification, StockClassification newClassification,
                                LocalDate expirationDate, Quantity quantity, EventMetadata metadata) {
        super(aggregateId, AGGREGATE_TYPE, metadata);
        this.productId = productId;
        this.tenantId = tenantId;
        this.oldClassification = oldClassification;
        this.newClassification = newClassification;
        this.expirationDate = expirationDate;
        this.quantity = quantity;
    }

    public ProductId getProductId() {
        return productId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public StockClassification getOldClassification() {
        return oldClassification;
    }

    public StockClassification getNewClassification() {
        return newClassification;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public Quantity getQuantity() {
        return quantity;
    }
}

