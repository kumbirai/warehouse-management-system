package com.ccbsa.wms.picking.domain.core.event;

import java.util.List;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.entity.Load;

/**
 * Domain Event: LoadPlannedEvent
 * <p>
 * Published when picking locations have been planned for a load.
 * <p>
 * This event indicates that:
 * - Picking locations have been optimized based on FEFO principles
 * - Picking tasks have been created
 * - Load status has been updated to PLANNED
 */
public class LoadPlannedEvent extends PickingEvent<Load> {
    private static final String AGGREGATE_TYPE = "Load";

    private final TenantId tenantId;
    private final String pickingListId;
    private final List<String> pickingTaskIds;

    /**
     * Constructor for LoadPlannedEvent.
     *
     * @param aggregateId    Load ID (as String)
     * @param tenantId       Tenant identifier
     * @param pickingListId  Picking list ID (as String)
     * @param pickingTaskIds List of picking task IDs (as String)
     */
    public LoadPlannedEvent(String aggregateId, TenantId tenantId, String pickingListId, List<String> pickingTaskIds) {
        super(aggregateId, AGGREGATE_TYPE);
        this.tenantId = tenantId;
        this.pickingListId = pickingListId;
        this.pickingTaskIds = pickingTaskIds != null ? List.copyOf(pickingTaskIds) : List.of();
    }

    /**
     * Constructor for LoadPlannedEvent with metadata.
     *
     * @param aggregateId    Load ID (as String)
     * @param tenantId       Tenant identifier
     * @param pickingListId  Picking list ID (as String)
     * @param pickingTaskIds List of picking task IDs (as String)
     * @param metadata       Event metadata for traceability
     */
    public LoadPlannedEvent(String aggregateId, TenantId tenantId, String pickingListId, List<String> pickingTaskIds, EventMetadata metadata) {
        super(aggregateId, AGGREGATE_TYPE, metadata);
        this.tenantId = tenantId;
        this.pickingListId = pickingListId;
        this.pickingTaskIds = pickingTaskIds != null ? List.copyOf(pickingTaskIds) : List.of();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getPickingListId() {
        return pickingListId;
    }

    public List<String> getPickingTaskIds() {
        return pickingTaskIds;
    }
}
