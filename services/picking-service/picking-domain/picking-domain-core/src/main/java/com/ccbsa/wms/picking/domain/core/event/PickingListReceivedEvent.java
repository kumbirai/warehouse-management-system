package com.ccbsa.wms.picking.domain.core.event;

import java.util.List;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;

/**
 * Domain Event: PickingListReceivedEvent
 * <p>
 * Published when a picking list is received (created from CSV upload or manual entry).
 * <p>
 * This event indicates that:
 * - A new picking list has been created
 * - Picking list is in RECEIVED status
 * - Loads and orders have been recorded
 */
public class PickingListReceivedEvent extends PickingEvent<PickingList> {
    private static final String AGGREGATE_TYPE = "PickingList";

    private final TenantId tenantId;
    private final List<String> loadIds;

    /**
     * Constructor for PickingListReceivedEvent.
     *
     * @param aggregateId Picking list ID (as String)
     * @param tenantId    Tenant identifier
     * @param loadIds     List of load IDs (as String)
     */
    public PickingListReceivedEvent(String aggregateId, TenantId tenantId, List<String> loadIds) {
        super(aggregateId, AGGREGATE_TYPE);
        this.tenantId = tenantId;
        this.loadIds = loadIds != null ? List.copyOf(loadIds) : List.of();
    }

    /**
     * Constructor for PickingListReceivedEvent with metadata.
     *
     * @param aggregateId Picking list ID (as String)
     * @param tenantId    Tenant identifier
     * @param loadIds     List of load IDs (as String)
     * @param metadata    Event metadata for traceability
     */
    public PickingListReceivedEvent(String aggregateId, TenantId tenantId, List<String> loadIds, EventMetadata metadata) {
        super(aggregateId, AGGREGATE_TYPE, metadata);
        this.tenantId = tenantId;
        this.loadIds = loadIds != null ? List.copyOf(loadIds) : List.of();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public List<String> getLoadIds() {
        return loadIds;
    }
}
