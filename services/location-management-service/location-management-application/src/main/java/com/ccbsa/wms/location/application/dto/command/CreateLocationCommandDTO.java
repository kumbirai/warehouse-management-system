package com.ccbsa.wms.location.application.dto.command;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command DTO: CreateLocationCommandDTO
 * <p>
 * Request DTO for creating a new warehouse location.
 * Supports both hierarchical model (code, name, type, parentLocationId) and coordinate-based model (zone, aisle, rack, level).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores DTO directly. DTOs are immutable when returned from API.")
public final class CreateLocationCommandDTO {
    // Hierarchical model fields
    @Size(max = 100, message = "Code must not exceed 100 characters")
    private String code;

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 50, message = "Type must not exceed 50 characters")
    private String type;

    @Size(max = 255, message = "Parent location ID must not exceed 255 characters")
    private String parentLocationId;

    private Integer capacity;

    private LocationDimensionsDTO dimensions;

    // Coordinate-based model fields (optional - used for direct coordinate specification)
    @Size(max = 10, message = "Zone must not exceed 10 characters")
    private String zone;

    @Size(max = 10, message = "Aisle must not exceed 10 characters")
    private String aisle;

    @Size(max = 10, message = "Rack must not exceed 10 characters")
    private String rack;

    @Size(max = 10, message = "Level must not exceed 10 characters")
    private String level;

    @Size(max = 20, message = "Barcode must not exceed 20 characters")
    private String barcode; // Optional - will be auto-generated if not provided

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Checks if this DTO uses the hierarchical model (has code/type).
     *
     * @return true if hierarchical model is used
     */
    public boolean isHierarchicalModel() {
        return code != null || type != null;
    }

    /**
     * Checks if this DTO uses the coordinate-based model (has zone/aisle/rack/level).
     *
     * @return true if coordinate-based model is used
     */
    public boolean isCoordinateBasedModel() {
        return zone != null || aisle != null || rack != null || level != null;
    }
}

