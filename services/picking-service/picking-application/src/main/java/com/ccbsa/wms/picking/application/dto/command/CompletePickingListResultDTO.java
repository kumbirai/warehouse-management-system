package com.ccbsa.wms.picking.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command Result DTO: CompletePickingListResultDTO
 * <p>
 * API response DTO for picking list completion result.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletePickingListResultDTO {
    private String pickingListId;
    private String status;
}
