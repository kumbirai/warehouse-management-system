package com.ccbsa.wms.location.application.dto.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ccbsa.wms.location.application.dto.common.LocationCoordinatesDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Result DTO: LocationQueryResultDTO
 * <p>
 * Response DTO for location query operations.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores DTO directly. DTOs are immutable when returned from API.")
public final class LocationQueryResultDTO {
    private String locationId;
    private String code;
    private String name;
    private String type;
    private String path;
    private String barcode;
    private LocationCoordinatesDTO coordinates;
    private String status;
    @JsonIgnore // Hide from JSON serialization - use getCapacity()/setCapacity() for Integer instead
    private LocationCapacityDTO capacityDTO;
    private String description;
    private String parentLocationId;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    @JsonIgnore // Internal use only - not serialized to JSON
    public LocationCapacityDTO getCapacityDTO() {
        return capacityDTO;
    }

    @JsonIgnore // Internal use only - not deserialized from JSON
    public void setCapacityDTO(LocationCapacityDTO capacity) {
        this.capacityDTO = capacity;
    }

    // JSON getter for capacity as Integer (maps to "capacity" field)
    @JsonProperty("capacity")
    public Integer getCapacity() {
        if (capacityDTO != null && capacityDTO.getMaximumQuantity() != null) {
            return capacityDTO.getMaximumQuantity().intValue();
        }
        return null;
    }

    // JSON setter for capacity as Integer (maps from "capacity" field)
    @JsonProperty("capacity")
    public void setCapacity(Integer capacity) {
        // When deserializing, create a LocationCapacityDTO with the Integer as maximumQuantity
        if (capacity != null) {
            this.capacityDTO = new LocationCapacityDTO(BigDecimal.ZERO, // currentQuantity defaults to 0
                    BigDecimal.valueOf(capacity) // maximumQuantity from Integer
            );
        }
    }
}

