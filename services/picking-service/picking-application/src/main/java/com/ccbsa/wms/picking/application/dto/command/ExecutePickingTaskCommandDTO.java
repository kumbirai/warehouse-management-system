package com.ccbsa.wms.picking.application.dto.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command DTO: ExecutePickingTaskCommandDTO
 * <p>
 * API request DTO for executing a picking task.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePickingTaskCommandDTO {
    @NotNull(message = "Picked quantity is required")
    @Min(value = 1, message = "Picked quantity must be at least 1")
    private Integer pickedQuantity;

    private Boolean isPartialPicking;

    private String partialReason;
}
