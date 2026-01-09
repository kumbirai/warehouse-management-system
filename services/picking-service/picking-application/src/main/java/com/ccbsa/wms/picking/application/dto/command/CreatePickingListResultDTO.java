package com.ccbsa.wms.picking.application.dto.command;

import java.time.ZonedDateTime;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: CreatePickingListResultDTO
 * <p>
 * DTO for picking list creation result.
 */
@Getter
@Builder
public class CreatePickingListResultDTO {
    private final String pickingListId;
    private final String status;
    private final ZonedDateTime receivedAt;
    private final int loadCount;
}
