package com.ccbsa.wms.picking.application.service.command.dto;

import java.time.ZonedDateTime;

import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: CreatePickingListResult
 * <p>
 * Result object returned after creating a picking list.
 */
@Getter
@Builder
public final class CreatePickingListResult {
    private final PickingListId pickingListId;
    private final PickingListStatus status;
    private final ZonedDateTime receivedAt;
    private final int loadCount;

    public CreatePickingListResult(PickingListId pickingListId, PickingListStatus status, ZonedDateTime receivedAt, int loadCount) {
        if (pickingListId == null) {
            throw new IllegalArgumentException("PickingListId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        this.pickingListId = pickingListId;
        this.status = status;
        this.receivedAt = receivedAt;
        this.loadCount = loadCount;
    }
}
