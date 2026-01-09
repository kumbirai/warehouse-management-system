package com.ccbsa.wms.picking.application.service.command.dto;

import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command Result DTO: CompletePickingListResult
 * <p>
 * Result object returned after completing a picking list.
 */
@Getter
@Builder
public final class CompletePickingListResult {
    private final PickingListId pickingListId;
    private final String status;
}
