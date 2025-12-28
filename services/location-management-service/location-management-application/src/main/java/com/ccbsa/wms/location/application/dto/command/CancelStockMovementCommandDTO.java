package com.ccbsa.wms.location.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command DTO: CancelStockMovementCommandDTO
 * <p>
 * Request DTO for canceling a stock movement.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelStockMovementCommandDTO {
    @NotBlank(message = "Cancellation reason is required")
    private String cancellationReason;
}

