package com.ccbsa.wms.location.application.dto.command;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command DTO: UpdateLocationCommandDTO
 * <p>
 * Request DTO for updating an existing warehouse location.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class UpdateLocationCommandDTO {
    @Size(max = 10, message = "Zone must not exceed 10 characters")
    private String zone;

    @Size(max = 10, message = "Aisle must not exceed 10 characters")
    private String aisle;

    @Size(max = 10, message = "Rack must not exceed 10 characters")
    private String rack;

    @Size(max = 10, message = "Level must not exceed 10 characters")
    private String level;

    @Size(max = 20, message = "Barcode must not exceed 20 characters")
    private String barcode; // Optional - can be null if not updating

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description; // Optional - can be null if not updating
}

