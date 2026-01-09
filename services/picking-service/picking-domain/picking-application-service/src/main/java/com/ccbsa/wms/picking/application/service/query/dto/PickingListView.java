package com.ccbsa.wms.picking.application.service.query.dto;

import java.time.ZonedDateTime;

import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListReference;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * View DTO: PickingListView
 * <p>
 * Read model view for PickingList queries.
 */
@Getter
@Builder
public final class PickingListView {
    private final PickingListId id;
    private final PickingListReference pickingListReference;
    private final PickingListStatus status;
    private final ZonedDateTime receivedAt;
    private final ZonedDateTime processedAt;
    private final int loadCount;
    private final int totalOrderCount;
    private final String notes;
}
