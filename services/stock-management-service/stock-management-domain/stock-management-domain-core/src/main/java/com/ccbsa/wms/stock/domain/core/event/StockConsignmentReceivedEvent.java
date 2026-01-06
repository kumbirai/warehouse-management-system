package com.ccbsa.wms.stock.domain.core.event;

import java.util.List;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.WarehouseId;
import com.ccbsa.wms.stock.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentLineItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;

/**
 * Domain Event: StockConsignmentReceivedEvent
 * <p>
 * Published when a stock consignment is received at the warehouse.
 * <p>
 * This event indicates that: - A new consignment has been created - Consignment is in RECEIVED status - Line items have been recorded
 */
public class StockConsignmentReceivedEvent extends StockManagementEvent<StockConsignment> {
    private static final String AGGREGATE_TYPE = "StockConsignment";

    private final ConsignmentReference consignmentReference;
    private final TenantId tenantId;
    private final WarehouseId warehouseId;
    private final List<ConsignmentLineItem> lineItems;

    /**
     * Constructor for StockConsignmentReceivedEvent.
     *
     * @param aggregateId          Consignment ID (as String)
     * @param consignmentReference Consignment reference
     * @param tenantId             Tenant identifier
     * @param warehouseId          Warehouse identifier
     * @param lineItems            List of consignment line items
     */
    public StockConsignmentReceivedEvent(String aggregateId, ConsignmentReference consignmentReference, TenantId tenantId, WarehouseId warehouseId,
                                         List<ConsignmentLineItem> lineItems) {
        super(aggregateId, AGGREGATE_TYPE);
        this.consignmentReference = consignmentReference;
        this.tenantId = tenantId;
        this.warehouseId = warehouseId;
        this.lineItems = lineItems != null ? List.copyOf(lineItems) : List.of();
    }

    /**
     * Constructor for StockConsignmentReceivedEvent with metadata.
     *
     * @param aggregateId          Consignment ID (as String)
     * @param consignmentReference Consignment reference
     * @param tenantId             Tenant identifier
     * @param warehouseId          Warehouse identifier
     * @param lineItems            List of consignment line items
     * @param metadata             Event metadata for traceability
     */
    public StockConsignmentReceivedEvent(String aggregateId, ConsignmentReference consignmentReference, TenantId tenantId, WarehouseId warehouseId,
                                         List<ConsignmentLineItem> lineItems, EventMetadata metadata) {
        super(aggregateId, AGGREGATE_TYPE, metadata);
        this.consignmentReference = consignmentReference;
        this.tenantId = tenantId;
        this.warehouseId = warehouseId;
        this.lineItems = lineItems != null ? List.copyOf(lineItems) : List.of();
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

    public List<ConsignmentLineItem> getLineItems() {
        return lineItems;
    }
}

