package com.ccbsa.wms.stock.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command DTO: AssignLocationToStockCommandDTO
 * <p>
 * API request DTO for assigning a location to a stock item.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignLocationToStockCommandDTO {
    @NotBlank(message = "Location ID is required")
    private String locationId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
}

