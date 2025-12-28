package com.ccbsa.wms.stock.application.dto.command;

import java.util.UUID;

import com.ccbsa.common.domain.valueobject.AdjustmentReason;
import com.ccbsa.common.domain.valueobject.AdjustmentType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command DTO: AdjustStockCommandDTO
 * <p>
 * Request DTO for adjusting stock levels.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustStockCommandDTO {
    @NotNull(message = "Product ID is required")
    private UUID productId;

    private UUID locationId; // Optional - null for product-wide adjustment

    private UUID stockItemId; // Optional - null for product/location adjustment

    @NotNull(message = "Adjustment type is required")
    private AdjustmentType adjustmentType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @NotNull(message = "Reason is required")
    private AdjustmentReason reason;

    private String notes;

    private String authorizationCode; // For large adjustments
}

