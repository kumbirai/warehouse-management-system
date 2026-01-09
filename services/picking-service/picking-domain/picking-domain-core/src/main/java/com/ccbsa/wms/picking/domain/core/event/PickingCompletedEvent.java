package com.ccbsa.wms.picking.domain.core.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;

/**
 * Domain Event: PickingCompletedEvent
 * <p>
 * Published when a picking list is completed.
 * <p>
 * This event indicates that:
 * - All picking tasks have been completed or partially completed
 * - Picking list is ready for shipping
 * - Stock levels have been updated
 * - Stock movements have been recorded
 */
public class PickingCompletedEvent extends PickingEvent<PickingList> {
    private static final String AGGREGATE_TYPE = "PickingList";

    private final TenantId tenantId;
    private final List<String> loadIds;
    private final String completedByUserId;

    /**
     * Constructor for PickingCompletedEvent.
     *
     * @param pickingListId     Picking list ID (as String)
     * @param tenantId          Tenant identifier
     * @param loadIds           List of load IDs in the picking list
     * @param completedByUserId User ID who completed the picking list
     */
    public PickingCompletedEvent(String pickingListId, TenantId tenantId, List<String> loadIds, String completedByUserId) {
        super(pickingListId, AGGREGATE_TYPE);
        this.tenantId = tenantId;
        // Create defensive copy of loadIds list
        this.loadIds = loadIds != null ? new ArrayList<>(loadIds) : new ArrayList<>();
        this.completedByUserId = completedByUserId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    /**
     * Returns a defensive copy of the load IDs list to prevent external modification.
     *
     * @return unmodifiable copy of the load IDs list
     */
    public List<String> getLoadIds() {
        return Collections.unmodifiableList(new ArrayList<>(loadIds));
    }

    public String getCompletedByUserId() {
        return completedByUserId;
    }
}
