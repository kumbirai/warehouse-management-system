package com.ccbsa.wms.stock.application.dto.command;

import java.util.UUID;

import com.ccbsa.common.domain.valueobject.AllocationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command DTO: AllocateStockCommandDTO
 * <p>
 * Request DTO for allocating stock for picking orders or reservations.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocateStockCommandDTO {
    @NotNull(message = "Product ID is required")
    private UUID productId;

    private UUID locationId; // Optional - null for FEFO allocation

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @NotNull(message = "Allocation type is required")
    private AllocationType allocationType;

    @NotBlank(message = "Reference ID is required for PICKING_ORDER allocation")
    private String referenceId; // Order ID, picking list ID, etc.

    private String notes;
}

