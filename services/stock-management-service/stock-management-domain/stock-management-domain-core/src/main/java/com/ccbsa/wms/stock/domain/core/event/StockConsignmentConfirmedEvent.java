package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.WarehouseId;
import com.ccbsa.wms.stock.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;

/**
 * Domain Event: StockConsignmentConfirmedEvent
 * <p>
 * Published when a stock consignment is confirmed.
 * <p>
 * This event indicates that: - Consignment has been confirmed and is ready for processing - Consignment status changed from RECEIVED to CONFIRMED
 */
public class StockConsignmentConfirmedEvent extends StockManagementEvent<StockConsignment> {
    private static final String AGGREGATE_TYPE = "StockConsignment";

    private final ConsignmentReference consignmentReference;
    private final TenantId tenantId;
    private final WarehouseId warehouseId;

    /**
     * Constructor for StockConsignmentConfirmedEvent.
     *
     * @param aggregateId          Consignment ID (as String)
     * @param consignmentReference Consignment reference
     * @param tenantId             Tenant identifier
     * @param warehouseId          Warehouse identifier
     */
    public StockConsignmentConfirmedEvent(String aggregateId, ConsignmentReference consignmentReference, TenantId tenantId, WarehouseId warehouseId) {
        super(aggregateId, AGGREGATE_TYPE);
        this.consignmentReference = consignmentReference;
        this.tenantId = tenantId;
        this.warehouseId = warehouseId;
    }

    /**
     * Constructor for StockConsignmentConfirmedEvent with metadata.
     *
     * @param aggregateId          Consignment ID (as String)
     * @param consignmentReference Consignment reference
     * @param tenantId             Tenant identifier
     * @param warehouseId          Warehouse identifier
     * @param metadata             Event metadata for traceability
     */
    public StockConsignmentConfirmedEvent(String aggregateId, ConsignmentReference consignmentReference, TenantId tenantId, WarehouseId warehouseId, EventMetadata metadata) {
        super(aggregateId, AGGREGATE_TYPE, metadata);
        this.consignmentReference = consignmentReference;
        this.tenantId = tenantId;
        this.warehouseId = warehouseId;
    }

    public ConsignmentReference getConsignmentReference() {
        return consignmentReference;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public WarehouseId getWarehouseId() {
        return warehouseId;
    }
}

