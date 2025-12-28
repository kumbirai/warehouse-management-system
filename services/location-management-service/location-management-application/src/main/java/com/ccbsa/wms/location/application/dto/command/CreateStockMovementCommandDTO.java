package com.ccbsa.wms.location.application.dto.command;

import java.util.UUID;

import com.ccbsa.common.domain.valueobject.MovementReason;
import com.ccbsa.wms.location.domain.core.valueobject.MovementType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command DTO: CreateStockMovementCommandDTO
 * <p>
 * Request DTO for creating a stock movement.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockMovementCommandDTO {
    // StockItemId is optional - if not provided, will be found by productId and sourceLocationId
    private String stockItemId;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Source location ID is required")
    private UUID sourceLocationId;

    @NotNull(message = "Destination location ID is required")
    private UUID destinationLocationId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @NotNull(message = "Movement type is required")
    private MovementType movementType;

    @NotNull(message = "Reason is required")
    private MovementReason reason;
}

